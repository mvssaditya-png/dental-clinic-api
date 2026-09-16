package com.dentalclinic.billing.service;

import com.dentalclinic.billing.dto.CreateInvoiceRequest;
import com.dentalclinic.billing.dto.InvoiceItemResponse;
import com.dentalclinic.billing.dto.InvoiceResponse;
import com.dentalclinic.billing.entity.Invoice;
import com.dentalclinic.billing.entity.InvoiceItem;
import com.dentalclinic.billing.entity.InvoiceStatus;
import com.dentalclinic.billing.repository.InvoiceItemRepository;
import com.dentalclinic.billing.repository.InvoiceRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.entity.TreatmentPlan;
import com.dentalclinic.treatment.entity.TreatmentPlanItem;
import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import com.dentalclinic.treatment.repository.TreatmentPlanItemRepository;
import com.dentalclinic.treatment.repository.TreatmentPlanRepository;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final Set<TreatmentPlanStatus> BILLABLE_PLAN_STATUSES =
            EnumSet.of(
                    TreatmentPlanStatus.APPROVED,
                    TreatmentPlanStatus.IN_PROGRESS,
                    TreatmentPlanStatus.COMPLETED
            );

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    private final TreatmentPlanRepository treatmentPlanRepository;
    private final TreatmentPlanItemRepository treatmentPlanItemRepository;

    private final ClinicRepository clinicRepository;

    @Transactional
    public InvoiceResponse createInvoice(
            CreateInvoiceRequest request
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                request.getClinicId()
        );

        TreatmentPlan treatmentPlan =
                treatmentPlanRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getTreatmentPlanId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Treatment plan not found"
                                )
                        );

        /*
         * Only patient-approved / active / completed
         * treatment plans can be billed.
         */
        if (!BILLABLE_PLAN_STATUSES.contains(
                treatmentPlan.getStatus()
        )) {

            throw new IllegalArgumentException(
                    "Treatment plan cannot be invoiced in status "
                            + treatmentPlan.getStatus()
            );
        }

        /*
         * MVP rule:
         * One invoice per treatment plan.
         *
         * V8 itself allows multiple invoices.
         * We can relax this later for partial/visit-wise billing.
         */
        if (invoiceRepository.existsByClinicIdAndTreatmentPlanId(
                clinic.getId(),
                treatmentPlan.getId()
        )) {

            throw new IllegalArgumentException(
                    "An invoice already exists for this treatment plan"
            );
        }

        List<TreatmentPlanItem> treatmentItems =
                treatmentPlanItemRepository
                        .findAllByTreatmentPlanIdWithDetails(
                                treatmentPlan.getId()
                        );

        if (treatmentItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Treatment plan has no items to invoice"
            );
        }

        /*
         * Cancelled treatment items must never be billed.
         */
        List<TreatmentPlanItem> billableItems =
                treatmentItems.stream()
                        .filter(item ->
                                item.getStatus()
                                        != com.dentalclinic.treatment.entity
                                        .TreatmentPlanItemStatus.CANCELLED
                        )
                        .toList();

        if (billableItems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Treatment plan has no billable items"
            );
        }

        LocalDate invoiceDate = LocalDate.now();

        if (request.getDueDate() != null
                && request.getDueDate().isBefore(invoiceDate)) {

            throw new IllegalArgumentException(
                    "Due date cannot be before invoice date"
            );
        }

        Invoice invoice = new Invoice();

        invoice.setClinic(clinic);
        invoice.setPatient(treatmentPlan.getPatient());
        invoice.setTreatmentPlan(treatmentPlan);
        invoice.setAppointment(treatmentPlan.getAppointment());

        invoice.setInvoiceNumber(
                generateInvoiceNumber(clinic.getId())
        );

        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(request.getDueDate());

        invoice.setStatus(InvoiceStatus.DRAFT);

        invoice.setSubtotal(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setBalanceAmount(BigDecimal.ZERO);

        invoice.setNotes(request.getNotes());

        invoice.setCreatedBy(currentUser);
        invoice.setUpdatedBy(currentUser);

        invoice = invoiceRepository.save(invoice);

        int displayOrder = 1;

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (TreatmentPlanItem treatmentItem : billableItems) {

            InvoiceItem invoiceItem =
                    createInvoiceItem(
                            clinic,
                            invoice,
                            treatmentItem,
                            displayOrder++
                    );

            invoiceItem =
                    invoiceItemRepository.save(invoiceItem);

            subtotal = subtotal.add(
                    invoiceItem.getGrossAmount()
            );

            totalDiscount = totalDiscount.add(
                    invoiceItem.getDiscountAmount()
            );

            totalTax = totalTax.add(
                    invoiceItem.getTaxAmount()
            );

            totalAmount = totalAmount.add(
                    invoiceItem.getFinalAmount()
            );
        }

        subtotal = money(subtotal);
        totalDiscount = money(totalDiscount);
        totalTax = money(totalTax);
        totalAmount = money(totalAmount);

        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(totalDiscount);
        invoice.setTaxAmount(totalTax);
        invoice.setTotalAmount(totalAmount);

        invoice.setPaidAmount(
                money(BigDecimal.ZERO)
        );

        invoice.setBalanceAmount(totalAmount);

        invoice.setUpdatedBy(currentUser);

        invoice = invoiceRepository.save(invoice);

        List<InvoiceItem> savedItems =
                invoiceItemRepository
                        .findAllByInvoiceIdWithDetails(
                                invoice.getId(),
                                clinic.getId()
                        );

        return mapToResponse(
                invoice,
                savedItems
        );
    }

    private InvoiceItem createInvoiceItem(
            Clinic clinic,
            Invoice invoice,
            TreatmentPlanItem treatmentItem,
            int displayOrder
    ) {

        InvoiceItem item = new InvoiceItem();

        item.setClinic(clinic);
        item.setInvoice(invoice);

        item.setTreatmentPlanItem(treatmentItem);
        item.setProcedure(treatmentItem.getProcedure());

        item.setToothNumber(
                treatmentItem.getToothNumber()
        );

        /*
         * Snapshot fields.
         * We use the Treatment Plan Item snapshot,
         * not the current Procedure Master name/code.
         */
        item.setItemCodeSnapshot(
                treatmentItem.getProcedureCodeSnapshot()
        );

        item.setItemNameSnapshot(
                treatmentItem.getProcedureNameSnapshot()
        );

        item.setDescription(
                treatmentItem.getDescription()
        );

        Integer quantity =
                treatmentItem.getQuantity() == null
                        ? 1
                        : treatmentItem.getQuantity();

        item.setQuantity(quantity);

        BigDecimal unitPrice =
                money(treatmentItem.getUnitPrice());

        BigDecimal grossAmount =
                money(treatmentItem.getGrossAmount());

        BigDecimal discountAmount =
                money(treatmentItem.getDiscountAmount());

        /*
         * Tax is currently zero because V7 TreatmentPlanItem
         * does not carry tax information.
         *
         * V8 supports tax, so we retain the fields correctly
         * without inventing a tax configuration.
         */
        BigDecimal taxableAmount =
                grossAmount.subtract(discountAmount);

        if (taxableAmount.signum() < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        taxableAmount = money(taxableAmount);

        BigDecimal taxRate =
                BigDecimal.ZERO.setScale(
                        4,
                        RoundingMode.HALF_UP
                );

        BigDecimal taxAmount =
                money(BigDecimal.ZERO);

        /*
         * Treatment Plan Item finalAmount already represents
         * the snapshotted item amount after its item discount.
         */
        BigDecimal finalAmount =
                money(treatmentItem.getFinalAmount());

        item.setUnitPrice(unitPrice);
        item.setGrossAmount(grossAmount);
        item.setDiscountAmount(discountAmount);

        item.setTaxableAmount(taxableAmount);
        item.setTaxRate(taxRate);
        item.setTaxAmount(taxAmount);

        item.setFinalAmount(finalAmount);

        item.setDisplayOrder(displayOrder);

        return item;
    }

    private String generateInvoiceNumber(UUID clinicId) {

        long next =
                invoiceRepository.countByClinicId(clinicId) + 1;

        String invoiceNumber;

        do {

            invoiceNumber =
                    String.format(
                            "INV%06d",
                            next
                    );

            next++;

        } while (
                invoiceRepository
                        .existsByClinicIdAndInvoiceNumber(
                                clinicId,
                                invoiceNumber
                        )
        );

        return invoiceNumber;
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

    private InvoiceResponse mapToResponse(
            Invoice invoice,
            List<InvoiceItem> items
    ) {

        return InvoiceResponse.builder()

                .id(invoice.getId())

                .clinicId(
                        invoice.getClinic().getId()
                )

                .patientId(
                        invoice.getPatient().getId()
                )

                .patientNumber(
                        invoice.getPatient().getPatientNumber()
                )

                /*
                 * Use your existing Patient name mapper here
                 * if Patient does not expose getFullName().
                 */
                .patientName(
                        buildPatientName(invoice.getPatient())
                )

                .treatmentPlanId(
                        invoice.getTreatmentPlan() != null
                                ? invoice.getTreatmentPlan().getId()
                                : null
                )

                .treatmentPlanNumber(
                        invoice.getTreatmentPlan() != null
                                ? invoice.getTreatmentPlan()
                                  .getTreatmentPlanNumber()
                                : null
                )

                .appointmentId(
                        invoice.getAppointment() != null
                                ? invoice.getAppointment().getId()
                                : null
                )

                .appointmentNumber(
                        invoice.getAppointment() != null
                                ? invoice.getAppointment()
                                  .getAppointmentNumber()
                                : null
                )

                .invoiceNumber(
                        invoice.getInvoiceNumber()
                )

                .invoiceDate(
                        invoice.getInvoiceDate()
                )

                .dueDate(
                        invoice.getDueDate()
                )

                .status(
                        invoice.getStatus()
                )

                .subtotal(
                        invoice.getSubtotal()
                )

                .discountAmount(
                        invoice.getDiscountAmount()
                )

                .taxAmount(
                        invoice.getTaxAmount()
                )

                .totalAmount(
                        invoice.getTotalAmount()
                )

                .paidAmount(
                        invoice.getPaidAmount()
                )

                .balanceAmount(
                        invoice.getBalanceAmount()
                )

                .notes(
                        invoice.getNotes()
                )

                .issuedAt(
                        invoice.getIssuedAt()
                )

                .cancelledAt(
                        invoice.getCancelledAt()
                )

                .cancellationReason(
                        invoice.getCancellationReason()
                )

                .items(
                        items.stream()
                                .map(this::mapItemToResponse)
                                .toList()
                )

                .createdAt(
                        invoice.getCreatedAt()
                )

                .updatedAt(
                        invoice.getUpdatedAt()
                )

                .build();
    }

    private InvoiceItemResponse mapItemToResponse(
            InvoiceItem item
    ) {

        return InvoiceItemResponse.builder()

                .id(item.getId())

                .invoiceId(
                        item.getInvoice().getId()
                )

                .treatmentPlanItemId(
                        item.getTreatmentPlanItem() != null
                                ? item.getTreatmentPlanItem().getId()
                                : null
                )

                .procedureId(
                        item.getProcedure() != null
                                ? item.getProcedure().getId()
                                : null
                )

                .toothNumber(
                        item.getToothNumber()
                )

                .itemCode(
                        item.getItemCodeSnapshot()
                )

                .itemName(
                        item.getItemNameSnapshot()
                )

                .description(
                        item.getDescription()
                )

                .quantity(
                        item.getQuantity()
                )

                .unitPrice(
                        item.getUnitPrice()
                )

                .grossAmount(
                        item.getGrossAmount()
                )

                .discountAmount(
                        item.getDiscountAmount()
                )

                .taxableAmount(
                        item.getTaxableAmount()
                )

                .taxRate(
                        item.getTaxRate()
                )

                .taxAmount(
                        item.getTaxAmount()
                )

                .finalAmount(
                        item.getFinalAmount()
                )

                .displayOrder(
                        item.getDisplayOrder()
                )

                .createdAt(
                        item.getCreatedAt()
                )

                .build();
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

    private String buildPatientName(Patient patient) {

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

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(
            UUID invoiceId,
            UUID requestedClinicId
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Invoice invoice =
                invoiceRepository
                        .findByIdAndClinicIdWithDetails(
                                invoiceId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invoice not found"
                                )
                        );

        List<InvoiceItem> items =
                invoiceItemRepository
                        .findAllByInvoiceIdWithDetails(
                                invoice.getId(),
                                clinic.getId()
                        );

        return mapToResponse(invoice, items);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getPatientInvoices(
            UUID patientId,
            UUID requestedClinicId
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        return invoiceRepository
                .findAllByPatientAndClinicWithDetails(
                        patientId,
                        clinic.getId()
                )
                .stream()
                .map(invoice -> {

                    List<InvoiceItem> items =
                            invoiceItemRepository
                                    .findAllByInvoiceIdWithDetails(
                                            invoice.getId(),
                                            clinic.getId()
                                    );

                    return mapToResponse(
                            invoice,
                            items
                    );
                })
                .toList();
    }

    @Transactional
    public InvoiceResponse issueInvoice(
            UUID invoiceId,
            UUID requestedClinicId
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Invoice invoice =
                invoiceRepository
                        .findByIdAndClinicIdWithDetails(
                                invoiceId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invoice not found"
                                )
                        );

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Only a DRAFT invoice can be issued"
            );
        }

        List<InvoiceItem> items =
                invoiceItemRepository
                        .findAllByInvoiceIdWithDetails(
                                invoice.getId(),
                                clinic.getId()
                        );

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invoice cannot be issued without items"
            );
        }

        if (invoice.getTotalAmount() == null
                || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Invoice total amount must be greater than zero"
            );
        }

        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setUpdatedBy(currentUser);

        invoice = invoiceRepository.save(invoice);

        return mapToResponse(
                invoice,
                items
        );
    }

    @Transactional
    public InvoiceResponse cancelInvoice(
            UUID invoiceId,
            UUID requestedClinicId,
            String reason
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Invoice invoice =
                invoiceRepository
                        .findByIdAndClinicIdWithDetails(
                                invoiceId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invoice not found"
                                )
                        );

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Invoice is already cancelled"
            );
        }

        /*
         * Once money has been allocated to the invoice,
         * cancellation should not be used to reverse it.
         * Refund/reversal belongs to the payment workflow.
         */
        if (invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID
                || invoice.getStatus() == InvoiceStatus.PAID) {

            throw new IllegalArgumentException(
                    "A paid or partially paid invoice cannot be cancelled"
            );
        }

        if (invoice.getStatus() != InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceStatus.ISSUED) {

            throw new IllegalArgumentException(
                    "Invoice cannot be cancelled in status "
                            + invoice.getStatus()
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required"
            );
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);

        invoice.setCancelledAt(
                LocalDateTime.now()
        );

        invoice.setCancellationReason(
                reason.trim()
        );

        invoice.setUpdatedBy(currentUser);

        invoice = invoiceRepository.save(invoice);

        List<InvoiceItem> items =
                invoiceItemRepository
                        .findAllByInvoiceIdWithDetails(
                                invoice.getId(),
                                clinic.getId()
                        );

        return mapToResponse(
                invoice,
                items
        );
    }
}