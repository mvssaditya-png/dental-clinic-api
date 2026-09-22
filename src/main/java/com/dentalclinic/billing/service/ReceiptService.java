package com.dentalclinic.billing.service;

import com.dentalclinic.billing.dto.CreateReceiptRequest;
import com.dentalclinic.billing.dto.ReceiptResponse;
import com.dentalclinic.billing.entity.Payment;
import com.dentalclinic.billing.entity.PaymentStatus;
import com.dentalclinic.billing.entity.Receipt;
import com.dentalclinic.billing.repository.PaymentRepository;
import com.dentalclinic.billing.repository.ReceiptRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    public ReceiptResponse createReceipt(
            CreateReceiptRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                request.getClinicId()
        );

        Payment payment =
                paymentRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getPaymentId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Payment not found"
                                )
                        );

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Receipt can only be created for a completed payment"
            );
        }

        if (receiptRepository.existsByPaymentId(payment.getId())) {
            throw new IllegalArgumentException(
                    "A receipt already exists for this payment"
            );
        }

        Receipt receipt = new Receipt();

        receipt.setClinic(clinic);
        receipt.setPayment(payment);
        receipt.setPatient(payment.getPatient());

        receipt.setReceiptNumber(
                generateReceiptNumber(clinic.getId())
        );

        receipt.setAmount(payment.getAmount());

        receipt.setIssuedBy(currentUser);

        receipt.setNotes(
                normalize(request.getNotes())
        );

        /*
         * PDF generation/S3 upload is intentionally
         * postponed for the MVP backend stage.
         */
        receipt.setPdfUrl(null);

        receipt = receiptRepository.save(receipt);

        return mapToResponse(receipt);
    }

    private String generateReceiptNumber(UUID clinicId) {

        long next =
                receiptRepository.countByClinicId(clinicId) + 1;

        String receiptNumber;

        do {

            receiptNumber =
                    String.format(
                            "REC%06d",
                            next
                    );

            next++;

        } while (
                receiptRepository
                        .existsByClinicIdAndReceiptNumber(
                                clinicId,
                                receiptNumber
                        )
        );

        return receiptNumber;
    }

    private ReceiptResponse mapToResponse(
            Receipt receipt
    ) {

        Payment payment = receipt.getPayment();
        Patient patient = receipt.getPatient();

        return ReceiptResponse.builder()

                .id(receipt.getId())

                .clinicId(
                        receipt.getClinic().getId()
                )

                .paymentId(
                        payment.getId()
                )

                .paymentNumber(
                        payment.getPaymentNumber()
                )

                .patientId(
                        patient.getId()
                )

                .patientNumber(
                        patient.getPatientNumber()
                )

                .patientName(
                        buildPatientName(patient)
                )

                .receiptNumber(
                        receipt.getReceiptNumber()
                )

                .amount(
                        receipt.getAmount()
                )

                .issuedAt(
                        receipt.getIssuedAt()
                )

                .issuedById(
                        receipt.getIssuedBy().getId()
                )

                .pdfUrl(
                        receipt.getPdfUrl()
                )

                .notes(
                        receipt.getNotes()
                )

                .createdAt(
                        receipt.getCreatedAt()
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
    public ReceiptResponse getReceipt(
            UUID receiptId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Receipt receipt =
                receiptRepository
                        .findByIdAndClinicIdWithDetails(
                                receiptId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Receipt not found"
                                )
                        );

        return mapToResponse(receipt);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByPayment(
            UUID paymentId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Receipt receipt =
                receiptRepository
                        .findByPaymentIdAndClinicIdWithDetails(
                                paymentId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Receipt not found"
                                )
                        );

        return mapToResponse(receipt);
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> getPatientReceipts(
            UUID patientId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        return receiptRepository
                .findAllByPatientAndClinicWithDetails(
                        patientId,
                        clinic.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}