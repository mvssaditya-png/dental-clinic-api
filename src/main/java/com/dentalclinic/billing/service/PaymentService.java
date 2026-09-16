package com.dentalclinic.billing.service;

import com.dentalclinic.billing.dto.CreatePaymentRequest;
import com.dentalclinic.billing.dto.PaymentAllocationResponse;
import com.dentalclinic.billing.dto.PaymentResponse;
import com.dentalclinic.billing.entity.*;
import com.dentalclinic.billing.repository.InvoiceRepository;
import com.dentalclinic.billing.repository.PaymentAllocationRepository;
import com.dentalclinic.billing.repository.PaymentRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final PaymentAllocationRepository
            paymentAllocationRepository;

    private final InvoiceRepository invoiceRepository;

    private final ClinicRepository clinicRepository;

    @Transactional
    public PaymentResponse createPayment(
            CreatePaymentRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                request.getClinicId()
        );

        Invoice invoice =
                invoiceRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getInvoiceId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invoice not found"
                                )
                        );

        /*
         * Payment is allowed only against an issued invoice.
         *
         * PARTIALLY_PAID is also valid because subsequent
         * payments can settle the remaining balance.
         */
        if (invoice.getStatus() != InvoiceStatus.ISSUED
                && invoice.getStatus()
                != InvoiceStatus.PARTIALLY_PAID) {

            throw new IllegalArgumentException(
                    "Payment cannot be recorded for invoice in status "
                            + invoice.getStatus()
            );
        }

        BigDecimal amount = money(request.getAmount());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        BigDecimal currentBalance =
                money(invoice.getBalanceAmount());

        if (amount.compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount cannot exceed invoice balance"
            );
        }

        Payment payment = new Payment();

        payment.setClinic(clinic);
        payment.setPatient(invoice.getPatient());

        payment.setPaymentNumber(
                generatePaymentNumber(clinic.getId())
        );

        payment.setAmount(amount);
        payment.setPaymentMode(
                request.getPaymentMode()
        );

        payment.setStatus(
                PaymentStatus.COMPLETED
        );

        payment.setTransactionReference(
                normalize(request.getTransactionReference())
        );

        payment.setNotes(
                normalize(request.getNotes())
        );

        payment.setReceivedBy(currentUser);
        payment.setCreatedBy(currentUser);
        payment.setUpdatedBy(currentUser);

        payment = paymentRepository.save(payment);

        /*
         * Connect the actual payment to the invoice.
         */
        PaymentAllocation allocation =
                new PaymentAllocation();

        allocation.setClinic(clinic);
        allocation.setPayment(payment);
        allocation.setInvoice(invoice);
        allocation.setAllocatedAmount(amount);
        allocation.setCreatedBy(currentUser);

        paymentAllocationRepository.save(allocation);

        /*
         * Recalculate from allocation records instead of simply:
         *
         * oldPaid + amount
         *
         * This keeps Invoice as a financial summary of the
         * underlying completed allocations.
         */
        BigDecimal paidAmount =
                money(
                        paymentAllocationRepository
                                .sumCompletedAllocationsForInvoice(
                                        invoice.getId(),
                                        clinic.getId()
                                )
                );

        BigDecimal totalAmount =
                money(invoice.getTotalAmount());

        BigDecimal balanceAmount =
                money(
                        totalAmount.subtract(paidAmount)
                );

        if (balanceAmount.signum() < 0) {
            throw new IllegalStateException(
                    "Invoice paid amount exceeds total amount"
            );
        }

        invoice.setPaidAmount(paidAmount);
        invoice.setBalanceAmount(balanceAmount);

        if (balanceAmount.compareTo(BigDecimal.ZERO) == 0) {

            invoice.setStatus(
                    InvoiceStatus.PAID
            );

        } else {

            invoice.setStatus(
                    InvoiceStatus.PARTIALLY_PAID
            );
        }

        invoice.setUpdatedBy(currentUser);

        invoiceRepository.save(invoice);

        List<PaymentAllocation> allocations =
                paymentAllocationRepository
                        .findAllByPaymentIdWithDetails(
                                payment.getId(),
                                clinic.getId()
                        );

        return mapToResponse(
                payment,
                allocations
        );
    }

    private String generatePaymentNumber(
            UUID clinicId
    ) {

        long next =
                paymentRepository.countByClinicId(
                        clinicId
                ) + 1;

        String paymentNumber;

        do {

            paymentNumber =
                    String.format(
                            "PAY%06d",
                            next
                    );

            next++;

        } while (
                paymentRepository
                        .existsByClinicIdAndPaymentNumber(
                                clinicId,
                                paymentNumber
                        )
        );

        return paymentNumber;
    }

    private PaymentResponse mapToResponse(
            Payment payment,
            List<PaymentAllocation> allocations
    ) {

        return PaymentResponse.builder()

                .id(payment.getId())

                .clinicId(
                        payment.getClinic().getId()
                )

                .patientId(
                        payment.getPatient().getId()
                )

                .patientNumber(
                        payment.getPatient()
                                .getPatientNumber()
                )

                .patientName(
                        buildPatientName(
                                payment.getPatient()
                        )
                )

                .paymentNumber(
                        payment.getPaymentNumber()
                )

                .paymentDate(
                        payment.getPaymentDate()
                )

                .amount(
                        payment.getAmount()
                )

                .paymentMode(
                        payment.getPaymentMode()
                )

                .status(
                        payment.getStatus()
                )

                .transactionReference(
                        payment.getTransactionReference()
                )

                .notes(
                        payment.getNotes()
                )

                .receivedById(
                        payment.getReceivedBy().getId()
                )

                .allocations(
                        allocations.stream()
                                .map(this::mapAllocationToResponse)
                                .toList()
                )

                .createdAt(
                        payment.getCreatedAt()
                )

                .updatedAt(
                        payment.getUpdatedAt()
                )

                .build();
    }

    private PaymentAllocationResponse
    mapAllocationToResponse(
            PaymentAllocation allocation
    ) {

        return PaymentAllocationResponse.builder()

                .id(allocation.getId())

                .invoiceId(
                        allocation.getInvoice().getId()
                )

                .invoiceNumber(
                        allocation.getInvoice()
                                .getInvoiceNumber()
                )

                .allocatedAmount(
                        allocation.getAllocatedAmount()
                )

                .createdAt(
                        allocation.getCreatedAt()
                )

                .build();
    }

    private String buildPatientName(
            Patient patient
    ) {

        String firstName =
                patient.getFirstName() != null
                        ? patient.getFirstName().trim()
                        : "";

        String lastName =
                patient.getLastName() != null
                        ? patient.getLastName().trim()
                        : "";

        return String.join(
                " ",
                java.util.stream.Stream
                        .of(firstName, lastName)
                        .filter(name -> !name.isBlank())
                        .toList()
        );
    }

    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private BigDecimal money(BigDecimal value) {

        if (value == null) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private Clinic resolveClinic(
            AppUser currentUser,
            UUID requestedClinicId
    ) {

        if (currentUser.getClinic() != null) {
            return currentUser.getClinic();
        }

        if (requestedClinicId == null) {
            throw new IllegalArgumentException(
                    "clinicId is required"
            );
        }

        return clinicRepository
                .findById(requestedClinicId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Clinic not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(
            UUID paymentId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Payment payment =
                paymentRepository
                        .findByIdAndClinicIdWithDetails(
                                paymentId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Payment not found"
                                )
                        );

        List<PaymentAllocation> allocations =
                paymentAllocationRepository
                        .findAllByPaymentIdWithDetails(
                                payment.getId(),
                                clinic.getId()
                        );

        return mapToResponse(
                payment,
                allocations
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPatientPayments(
            UUID patientId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        List<Payment> payments =
                paymentRepository
                        .findAllByPatientAndClinicWithDetails(
                                patientId,
                                clinic.getId()
                        );

        return payments.stream()
                .map(payment -> {

                    List<PaymentAllocation> allocations =
                            paymentAllocationRepository
                                    .findAllByPaymentIdWithDetails(
                                            payment.getId(),
                                            clinic.getId()
                                    );

                    return mapToResponse(
                            payment,
                            allocations
                    );
                })
                .toList();
    }
}