package com.dentalclinic.treatment.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.dto.*;
import com.dentalclinic.treatment.entity.*;
import com.dentalclinic.treatment.repository.*;

/*
 * Use your existing CaseSheet import.
 */
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.casesheet.entity.CaseSheetStatus;
import com.dentalclinic.casesheet.repository.CaseSheetRepository;

import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TreatmentPlanService {

    private final TreatmentPlanRepository treatmentPlanRepository;

    private final TreatmentPlanStatusHistoryRepository
            treatmentPlanStatusHistoryRepository;

    private final CaseSheetRepository caseSheetRepository;

    private final ClinicRepository clinicRepository;
    private final TreatmentPlanItemRepository treatmentPlanItemRepository;

    private final ProcedureMasterRepository procedureMasterRepository;

    private final ProcedurePriceRepository procedurePriceRepository;
    @Transactional
    public TreatmentPlanResponse createTreatmentPlan(
            CreateTreatmentPlanRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getCaseSheetId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found"
                                )
                        );

        if (caseSheet.getStatus()
                != CaseSheetStatus.DRAFT) {

            throw new IllegalArgumentException(
                    "Treatment plan can only be created while case sheet is DRAFT"
            );
        }

        String treatmentPlanNumber =
                generateTreatmentPlanNumber(
                        clinic.getId()
                );

        TreatmentPlan treatmentPlan =
                TreatmentPlan.builder()

                        .clinic(clinic)

                        .patient(
                                caseSheet.getPatient()
                        )

                        .appointment(
                                caseSheet.getAppointment()
                        )

                        .consultation(
                                caseSheet.getConsultation()
                        )

                        .caseSheet(caseSheet)

                        .doctor(
                                caseSheet.getDoctor()
                        )

                        .treatmentPlanNumber(
                                treatmentPlanNumber
                        )

                        .title(
                                normalize(
                                        request.getTitle()
                                )
                        )

                        .status(
                                TreatmentPlanStatus.DRAFT
                        )

                        .subtotal(
                                BigDecimal.ZERO
                        )

                        .discountType(null)

                        .discountValue(
                                BigDecimal.ZERO
                        )

                        .discountAmount(
                                BigDecimal.ZERO
                        )

                        .finalAmount(
                                BigDecimal.ZERO
                        )

                        .estimatedTotalVisits(
                                request.getEstimatedTotalVisits()
                        )

                        .notes(
                                normalize(
                                        request.getNotes()
                                )
                        )

                        .patientNotes(
                                normalize(
                                        request.getPatientNotes()
                                )
                        )

                        .createdBy(currentUser)

                        .updatedBy(currentUser)

                        .build();

        treatmentPlan =
                treatmentPlanRepository.save(
                        treatmentPlan
                );

        TreatmentPlanStatusHistory history =
                TreatmentPlanStatusHistory.builder()

                        .clinic(clinic)

                        .treatmentPlan(
                                treatmentPlan
                        )

                        .fromStatus(null)

                        .toStatus(
                                TreatmentPlanStatus.DRAFT
                        )

                        .reason(
                                "Treatment plan created"
                        )

                        .changedBy(
                                currentUser
                        )

                        .build();

        treatmentPlanStatusHistoryRepository
                .save(history);

        return mapResponse(
                treatmentPlan
        );
    }

    @Transactional(readOnly = true)
    public TreatmentPlanResponse getTreatmentPlan(
            UUID treatmentPlanId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        TreatmentPlan treatmentPlan =
                treatmentPlanRepository
                        .findByIdAndClinicIdWithDetails(
                                treatmentPlanId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Treatment plan not found"
                                )
                        );

        return mapResponse(
                treatmentPlan
        );
    }

    private String generateTreatmentPlanNumber(
            UUID clinicId
    ) {

        long nextNumber =
                treatmentPlanRepository
                        .countByClinicId(
                                clinicId
                        ) + 1;

        String treatmentPlanNumber;

        do {

            treatmentPlanNumber =
                    String.format(
                            "TP%06d",
                            nextNumber
                    );

            nextNumber++;

        } while (
                treatmentPlanRepository
                        .existsByClinicIdAndTreatmentPlanNumber(
                                clinicId,
                                treatmentPlanNumber
                        )
        );

        return treatmentPlanNumber;
    }

    private TreatmentPlanResponse mapResponse(
            TreatmentPlan treatmentPlan
    ) {

        String patientName =
                buildName(
                        treatmentPlan
                                .getPatient()
                                .getFirstName(),

                        treatmentPlan
                                .getPatient()
                                .getLastName()
                );

        String doctorName =
                buildName(
                        treatmentPlan
                                .getDoctor()
                                .getUser()
                                .getFirstName(),

                        treatmentPlan
                                .getDoctor()
                                .getUser()
                                .getLastName()
                );

        return TreatmentPlanResponse.builder()

                .id(
                        treatmentPlan.getId()
                )

                .clinicId(
                        treatmentPlan
                                .getClinic()
                                .getId()
                )

                .treatmentPlanNumber(
                        treatmentPlan
                                .getTreatmentPlanNumber()
                )

                .patientId(
                        treatmentPlan
                                .getPatient()
                                .getId()
                )

                .patientNumber(
                        treatmentPlan
                                .getPatient()
                                .getPatientNumber()
                )

                .patientName(
                        patientName
                )

                .doctorId(
                        treatmentPlan
                                .getDoctor()
                                .getId()
                )

                .doctorName(
                        doctorName
                )

                .appointmentId(
                        treatmentPlan.getAppointment() != null
                                ? treatmentPlan
                                  .getAppointment()
                                  .getId()
                                : null
                )

                .appointmentNumber(
                        treatmentPlan.getAppointment() != null
                                ? treatmentPlan
                                  .getAppointment()
                                  .getAppointmentNumber()
                                : null
                )

                .consultationId(
                        treatmentPlan.getConsultation() != null
                                ? treatmentPlan
                                  .getConsultation()
                                  .getId()
                                : null
                )

                .caseSheetId(
                        treatmentPlan.getCaseSheet() != null
                                ? treatmentPlan
                                  .getCaseSheet()
                                  .getId()
                                : null
                )

                .caseSheetNumber(
                        treatmentPlan.getCaseSheet() != null
                                ? treatmentPlan
                                  .getCaseSheet()
                                  .getCaseSheetNumber()
                                : null
                )

                .title(
                        treatmentPlan.getTitle()
                )

                .status(
                        treatmentPlan.getStatus()
                )

                .subtotal(
                        treatmentPlan.getSubtotal()
                )

                .discountType(
                        treatmentPlan.getDiscountType()
                )

                .discountValue(
                        treatmentPlan.getDiscountValue()
                )

                .discountAmount(
                        treatmentPlan.getDiscountAmount()
                )

                .finalAmount(
                        treatmentPlan.getFinalAmount()
                )

                .estimatedTotalVisits(
                        treatmentPlan
                                .getEstimatedTotalVisits()
                )

                .notes(
                        treatmentPlan.getNotes()
                )

                .patientNotes(
                        treatmentPlan.getPatientNotes()
                )

                .presentedAt(
                        treatmentPlan.getPresentedAt()
                )

                .approvedAt(
                        treatmentPlan.getApprovedAt()
                )

                .declinedAt(
                        treatmentPlan.getDeclinedAt()
                )

                .completedAt(
                        treatmentPlan.getCompletedAt()
                )

                .cancelledAt(
                        treatmentPlan.getCancelledAt()
                )

                .approvalNotes(
                        treatmentPlan.getApprovalNotes()
                )

                .declineReason(
                        treatmentPlan.getDeclineReason()
                )

                .cancellationReason(
                        treatmentPlan
                                .getCancellationReason()
                )

                .createdAt(
                        treatmentPlan.getCreatedAt()
                )

                .updatedAt(
                        treatmentPlan.getUpdatedAt()
                )

                .build();
    }

    private String buildName(
            String firstName,
            String lastName
    ) {

        StringBuilder name =
                new StringBuilder();

        if (firstName != null &&
                !firstName.isBlank()) {

            name.append(
                    firstName.trim()
            );
        }

        if (lastName != null &&
                !lastName.isBlank()) {

            if (!name.isEmpty()) {
                name.append(" ");
            }

            name.append(
                    lastName.trim()
            );
        }

        return name.toString();
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
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
                .findById(
                        requestedClinicId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Clinic not found"
                        )
                );
    }

    @Transactional
    public TreatmentPlanItemResponse addTreatmentPlanItem(
            UUID treatmentPlanId,
            CreateTreatmentPlanItemRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        TreatmentPlan treatmentPlan =
                treatmentPlanRepository
                        .findByIdAndClinicIdWithDetails(
                                treatmentPlanId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Treatment plan not found"
                                )
                        );

        if (treatmentPlan.getStatus()
                != TreatmentPlanStatus.DRAFT) {

            throw new IllegalArgumentException(
                    "Items can only be added while treatment plan is DRAFT"
            );
        }

        ProcedureMaster procedure =
                procedureMasterRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getProcedureId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Procedure not found"
                                )
                        );

        if (!Boolean.TRUE.equals(
                procedure.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Procedure is inactive"
            );
        }

        ProcedurePrice currentPrice =
                procedurePriceRepository
                        .findCurrentPrice(
                                clinic.getId(),
                                procedure.getId(),
                                LocalDate.now()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No current price configured for this procedure"
                                )
                        );

        int quantity =
                request.getQuantity() != null
                        ? request.getQuantity()
                        : 1;

        BigDecimal unitPrice =
                currentPrice.getPrice()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal grossAmount =
                unitPrice
                        .multiply(
                                BigDecimal.valueOf(
                                        quantity
                                )
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        int sequenceNumber =
                request.getSequenceNumber() != null
                        ? request.getSequenceNumber()
                        : (int) treatmentPlanItemRepository
                                .countByTreatmentPlanId(
                                        treatmentPlan.getId()
                                ) + 1;

        TreatmentPlanItem item =
                TreatmentPlanItem.builder()

                        .clinic(clinic)

                        .treatmentPlan(
                                treatmentPlan
                        )

                        .procedure(
                                procedure
                        )

                        /*
                         * We'll connect these optional V7 references
                         * in the next supporting pass.
                         */
                        .odontogramTooth(null)
                        .condition(null)

                        .toothNumber(
                                normalize(
                                        request.getToothNumber()
                                )
                        )

                        /*
                         * Snapshot values.
                         */
                        .procedureCodeSnapshot(
                                procedure.getProcedureCode()
                        )

                        .procedureNameSnapshot(
                                procedure.getProcedureName()
                        )

                        .description(
                                request.getDescription() != null
                                        ? normalize(
                                        request.getDescription()
                                )
                                        : normalize(
                                        procedure.getDescription()
                                )
                        )

                        .quantity(
                                quantity
                        )

                        .unitPrice(
                                unitPrice
                        )

                        .grossAmount(
                                grossAmount
                        )

                        /*
                         * No item-level discount yet.
                         */
                        .discountType(null)

                        .discountValue(
                                BigDecimal.ZERO
                        )

                        .discountAmount(
                                BigDecimal.ZERO
                        )

                        .finalAmount(
                                grossAmount
                        )

                        .estimatedVisits(
                                request.getEstimatedVisits() != null
                                        ? request.getEstimatedVisits()
                                        : procedure
                                          .getDefaultEstimatedVisits()
                        )

                        .status(
                                TreatmentPlanItemStatus.PLANNED
                        )

                        .sequenceNumber(
                                sequenceNumber
                        )

                        .clinicalNotes(
                                request.getClinicalNotes() != null
                                        ? normalize(
                                        request.getClinicalNotes()
                                )
                                        : normalize(
                                        procedure.getDefaultNotes()
                                )
                        )

                        .patientExplanation(
                                request.getPatientExplanation() != null
                                        ? normalize(
                                        request.getPatientExplanation()
                                )
                                        : normalize(
                                        procedure.getPatientExplanation()
                                )
                        )

                        .followUpInstructions(
                                request.getFollowUpInstructions() != null
                                        ? normalize(
                                        request.getFollowUpInstructions()
                                )
                                        : normalize(
                                        procedure.getFollowUpInstructions()
                                )
                        )

                        .createdBy(
                                currentUser
                        )

                        .updatedBy(
                                currentUser
                        )

                        .build();

        item =
                treatmentPlanItemRepository.save(
                        item
                );

        recalculateTreatmentPlan(
                treatmentPlan,
                currentUser
        );

        return mapItemResponse(
                item
        );
    }

    @Transactional(readOnly = true)
    public List<TreatmentPlanItemResponse> getTreatmentPlanItems(
            UUID treatmentPlanId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        treatmentPlanRepository
                .findByIdAndClinicIdWithDetails(
                        treatmentPlanId,
                        clinic.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Treatment plan not found"
                        )
                );

        return treatmentPlanItemRepository
                .findAllByTreatmentPlanIdWithDetails(
                        treatmentPlanId
                )
                .stream()
                .map(this::mapItemResponse)
                .toList();
    }

    private void recalculateTreatmentPlan(
            TreatmentPlan treatmentPlan,
            AppUser currentUser
    ) {

        List<TreatmentPlanItem> items =
                treatmentPlanItemRepository
                        .findAllByTreatmentPlanIdWithDetails(
                                treatmentPlan.getId()
                        );

        BigDecimal subtotal =
                items.stream()
                        .filter(item ->
                                item.getStatus()
                                        != TreatmentPlanItemStatus.CANCELLED
                        )
                        .map(TreatmentPlanItem::getFinalAmount)
                        .filter(amount ->
                                amount != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal discountAmount =
                BigDecimal.ZERO;

        if (treatmentPlan.getDiscountType() != null &&
                treatmentPlan.getDiscountValue() != null) {

            if (treatmentPlan.getDiscountType()
                    == DiscountType.PERCENTAGE) {

                discountAmount =
                        subtotal
                                .multiply(
                                        treatmentPlan
                                                .getDiscountValue()
                                )
                                .divide(
                                        BigDecimal.valueOf(100),
                                        2,
                                        RoundingMode.HALF_UP
                                );

            } else if (
                    treatmentPlan.getDiscountType()
                            == DiscountType.FIXED
            ) {

                discountAmount =
                        treatmentPlan
                                .getDiscountValue()
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                );
            }
        }

        if (discountAmount.compareTo(
                subtotal
        ) > 0) {

            discountAmount =
                    subtotal;
        }

        BigDecimal finalAmount =
                subtotal
                        .subtract(
                                discountAmount
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        treatmentPlan.setSubtotal(
                subtotal
        );

        treatmentPlan.setDiscountAmount(
                discountAmount
        );

        treatmentPlan.setFinalAmount(
                finalAmount
        );

        treatmentPlan.setUpdatedBy(
                currentUser
        );

        treatmentPlanRepository.save(
                treatmentPlan
        );
    }

    private TreatmentPlanItemResponse mapItemResponse(
            TreatmentPlanItem item
    ) {

        return TreatmentPlanItemResponse.builder()

                .id(
                        item.getId()
                )

                .treatmentPlanId(
                        item.getTreatmentPlan()
                                .getId()
                )

                .procedureId(
                        item.getProcedure()
                                .getId()
                )

                .procedureCode(
                        item.getProcedureCodeSnapshot()
                )

                .procedureName(
                        item.getProcedureNameSnapshot()
                )

                .toothNumber(
                        item.getToothNumber()
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

                .discountType(
                        item.getDiscountType()
                )

                .discountValue(
                        item.getDiscountValue()
                )

                .discountAmount(
                        item.getDiscountAmount()
                )

                .finalAmount(
                        item.getFinalAmount()
                )

                .estimatedVisits(
                        item.getEstimatedVisits()
                )

                .status(
                        item.getStatus()
                )

                .sequenceNumber(
                        item.getSequenceNumber()
                )

                .clinicalNotes(
                        item.getClinicalNotes()
                )

                .patientExplanation(
                        item.getPatientExplanation()
                )

                .followUpInstructions(
                        item.getFollowUpInstructions()
                )

                .startedAt(
                        item.getStartedAt()
                )

                .completedAt(
                        item.getCompletedAt()
                )

                .cancelledAt(
                        item.getCancelledAt()
                )

                .cancellationReason(
                        item.getCancellationReason()
                )

                .createdAt(
                        item.getCreatedAt()
                )

                .updatedAt(
                        item.getUpdatedAt()
                )

                .build();
    }

    @Transactional
    public TreatmentPlanResponse updateTreatmentPlan(
            UUID treatmentPlanId,
            UpdateTreatmentPlanRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        TreatmentPlan treatmentPlan =
                treatmentPlanRepository
                        .findByIdAndClinicIdWithDetails(
                                treatmentPlanId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Treatment plan not found"
                                )
                        );

        if (treatmentPlan.getStatus()
                != TreatmentPlanStatus.DRAFT) {

            throw new IllegalArgumentException(
                    "Only DRAFT treatment plans can be updated"
            );
        }

        treatmentPlan.setTitle(
                normalize(
                        request.getTitle()
                )
        );

        treatmentPlan.setEstimatedTotalVisits(
                request.getEstimatedTotalVisits()
        );

        treatmentPlan.setNotes(
                normalize(
                        request.getNotes()
                )
        );

        treatmentPlan.setPatientNotes(
                normalize(
                        request.getPatientNotes()
                )
        );

        /*
         * Discount handling.
         */
        if (request.getDiscountType() == null) {

            treatmentPlan.setDiscountType(null);
            treatmentPlan.setDiscountValue(
                    BigDecimal.ZERO
            );

        } else {

            if (request.getDiscountValue() == null) {
                throw new IllegalArgumentException(
                        "discountValue is required when discountType is provided"
                );
            }

            if (request.getDiscountType()
                    == DiscountType.PERCENTAGE
                    && request.getDiscountValue()
                    .compareTo(
                            BigDecimal.valueOf(100)
                    ) > 0) {

                throw new IllegalArgumentException(
                        "Percentage discount cannot exceed 100"
                );
            }

            treatmentPlan.setDiscountType(
                    request.getDiscountType()
            );

            treatmentPlan.setDiscountValue(
                    request.getDiscountValue()
            );
        }

        treatmentPlan.setUpdatedBy(
                currentUser
        );

        /*
         * Recalculates:
         * subtotal
         * discountAmount
         * finalAmount
         */
        recalculateTreatmentPlan(
                treatmentPlan,
                currentUser
        );

        /*
         * Reload using our targeted fetch query so
         * response mapping has all required relations.
         */
        TreatmentPlan updated =
                treatmentPlanRepository
                        .findByIdAndClinicIdWithDetails(
                                treatmentPlanId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Treatment plan could not be reloaded"
                                )
                        );

        return mapResponse(
                updated
        );
    }

    @Transactional
    public TreatmentPlanResponse updateTreatmentPlanStatus(
            UUID treatmentPlanId,
            UpdateTreatmentPlanStatusRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        TreatmentPlan treatmentPlan =
                treatmentPlanRepository
                        .findByIdAndClinicIdWithDetails(
                                treatmentPlanId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Treatment plan not found"
                                )
                        );

        TreatmentPlanStatus currentStatus =
                treatmentPlan.getStatus();

        TreatmentPlanStatus newStatus =
                request.getStatus();

        if (currentStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Treatment plan is already in status "
                            + newStatus
            );
        }

        validateStatusTransition(
                currentStatus,
                newStatus
        );

        if (newStatus == TreatmentPlanStatus.COMPLETED) {

            long incompleteItemCount =
                    treatmentPlanItemRepository
                            .countByTreatmentPlanIdAndStatusNotIn(
                                    treatmentPlan.getId(),
                                    java.util.List.of(
                                            TreatmentPlanItemStatus.COMPLETED,
                                            TreatmentPlanItemStatus.CANCELLED
                                    )
                            );

            if (incompleteItemCount > 0) {

                throw new IllegalArgumentException(
                        "Treatment plan cannot be completed until all active treatment items are completed"
                );
            }
        }

        if (newStatus == TreatmentPlanStatus.COMPLETED) {

            List<TreatmentPlanItem> items =
                    treatmentPlanItemRepository
                            .findAllByTreatmentPlanIdWithDetails(
                                    treatmentPlan.getId()
                            );

            boolean hasCompletedItem =
                    items.stream()
                            .anyMatch(item ->
                                    item.getStatus()
                                            == TreatmentPlanItemStatus.COMPLETED
                            );

            if (!hasCompletedItem) {

                throw new IllegalArgumentException(
                        "Treatment plan must have at least one completed treatment item"
                );
            }
        }

        /*
         * Before presenting a treatment plan,
         * it should actually contain treatment.
         */
        if (newStatus == TreatmentPlanStatus.PRESENTED) {

            long itemCount =
                    treatmentPlanItemRepository
                            .countByTreatmentPlanId(
                                    treatmentPlan.getId()
                            );

            if (itemCount == 0) {
                throw new IllegalArgumentException(
                        "Treatment plan must contain at least one item before it can be presented"
                );
            }
        }

        if (newStatus == TreatmentPlanStatus.DECLINED
                && normalize(
                request.getDeclineReason()
        ) == null) {

            throw new IllegalArgumentException(
                    "declineReason is required when treatment plan is declined"
            );
        }

        if (newStatus == TreatmentPlanStatus.CANCELLED
                && normalize(
                request.getCancellationReason()
        ) == null) {

            throw new IllegalArgumentException(
                    "cancellationReason is required when treatment plan is cancelled"
            );
        }

        switch (newStatus) {

            case PRESENTED -> {
                treatmentPlan.setPresentedAt(
                        java.time.LocalDateTime.now()
                );
            }

            case APPROVED -> {
                treatmentPlan.setApprovedAt(
                        java.time.LocalDateTime.now()
                );

                treatmentPlan.setApprovalNotes(
                        normalize(
                                request.getApprovalNotes()
                        )
                );
            }

            case DECLINED -> {
                treatmentPlan.setDeclinedAt(
                        java.time.LocalDateTime.now()
                );

                treatmentPlan.setDeclineReason(
                        normalize(
                                request.getDeclineReason()
                        )
                );
            }

            case COMPLETED -> {
                treatmentPlan.setCompletedAt(
                        java.time.LocalDateTime.now()
                );
            }

            case CANCELLED -> {
                treatmentPlan.setCancelledAt(
                        java.time.LocalDateTime.now()
                );

                treatmentPlan.setCancellationReason(
                        normalize(
                                request.getCancellationReason()
                        )
                );
            }

            case IN_PROGRESS -> {
                /*
                 * V7 treatment_plan does not have
                 * an in_progress_at column.
                 */
            }

            default -> {
            }
        }

        treatmentPlan.setStatus(
                newStatus
        );

        treatmentPlan.setUpdatedBy(
                currentUser
        );

        treatmentPlanRepository.save(
                treatmentPlan
        );

        TreatmentPlanStatusHistory history =
                TreatmentPlanStatusHistory.builder()

                        .clinic(clinic)

                        .treatmentPlan(
                                treatmentPlan
                        )

                        .fromStatus(
                                currentStatus
                        )

                        .toStatus(
                                newStatus
                        )

                        .reason(
                                determineStatusReason(
                                        newStatus,
                                        request
                                )
                        )

                        .changedBy(
                                currentUser
                        )

                        .build();

        treatmentPlanStatusHistoryRepository
                .save(history);

        return mapResponse(
                treatmentPlan
        );
    }

    private void validateStatusTransition(
            TreatmentPlanStatus currentStatus,
            TreatmentPlanStatus newStatus
    ) {

        boolean allowed =
                switch (currentStatus) {

                    case DRAFT ->
                            newStatus
                                    == TreatmentPlanStatus.PRESENTED
                                    || newStatus
                                    == TreatmentPlanStatus.CANCELLED;

                    case PRESENTED ->
                            newStatus
                                    == TreatmentPlanStatus.APPROVED
                                    || newStatus
                                    == TreatmentPlanStatus.DECLINED
                                    || newStatus
                                    == TreatmentPlanStatus.CANCELLED;

                    case APPROVED ->
                            newStatus
                                    == TreatmentPlanStatus.IN_PROGRESS
                                    || newStatus
                                    == TreatmentPlanStatus.CANCELLED;

                    case IN_PROGRESS ->
                            newStatus
                                    == TreatmentPlanStatus.COMPLETED
                                    || newStatus
                                    == TreatmentPlanStatus.CANCELLED;

                    case COMPLETED,
                         DECLINED,
                         CANCELLED ->
                            false;
                };

        if (!allowed) {

            throw new IllegalArgumentException(
                    "Invalid treatment plan status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }

    private String determineStatusReason(
            TreatmentPlanStatus newStatus,
            UpdateTreatmentPlanStatusRequest request
    ) {

        String explicitReason =
                normalize(
                        request.getReason()
                );

        if (explicitReason != null) {
            return explicitReason;
        }

        return switch (newStatus) {

            case PRESENTED ->
                    "Treatment plan presented to patient";

            case APPROVED ->
                    "Treatment plan approved";

            case IN_PROGRESS ->
                    "Treatment started";

            case COMPLETED ->
                    "Treatment plan completed";

            case DECLINED ->
                    normalize(
                            request.getDeclineReason()
                    );

            case CANCELLED ->
                    normalize(
                            request.getCancellationReason()
                    );

            default ->
                    "Treatment plan status updated";
        };
    }

    @Transactional
    public TreatmentPlanItemResponse updateTreatmentPlanItemStatus(
            UUID treatmentPlanId,
            UUID itemId,
            UpdateTreatmentPlanItemStatusRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        TreatmentPlan treatmentPlan =
                treatmentPlanRepository
                        .findByIdAndClinicIdWithDetails(
                                treatmentPlanId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Treatment plan not found"
                                )
                        );

        if (treatmentPlan.getStatus()
                != TreatmentPlanStatus.APPROVED
                &&
                treatmentPlan.getStatus()
                        != TreatmentPlanStatus.IN_PROGRESS) {

            throw new IllegalArgumentException(
                    "Treatment items can only be progressed when treatment plan is APPROVED or IN_PROGRESS"
            );
        }

        TreatmentPlanItem item =
                treatmentPlanItemRepository
                        .findByIdAndClinicIdWithDetails(
                                itemId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Treatment plan item not found"
                                )
                        );

        /*
         * Important:
         * itemId must actually belong to treatmentPlanId
         * supplied in the URL.
         */
        if (!item.getTreatmentPlan()
                .getId()
                .equals(
                        treatmentPlan.getId()
                )) {

            throw new IllegalArgumentException(
                    "Treatment plan item does not belong to this treatment plan"
            );
        }

        TreatmentPlanItemStatus currentStatus =
                item.getStatus();

        TreatmentPlanItemStatus newStatus =
                request.getStatus();

        if (currentStatus == newStatus) {

            throw new IllegalArgumentException(
                    "Treatment plan item is already in status "
                            + newStatus
            );
        }

        validateItemStatusTransition(
                currentStatus,
                newStatus
        );

        if (newStatus
                == TreatmentPlanItemStatus.CANCELLED
                &&
                normalize(
                        request.getCancellationReason()
                ) == null) {

            throw new IllegalArgumentException(
                    "cancellationReason is required when treatment plan item is cancelled"
            );
        }

        java.time.LocalDateTime now =
                java.time.LocalDateTime.now();

        switch (newStatus) {

            case IN_PROGRESS -> {

                if (item.getStartedAt() == null) {
                    item.setStartedAt(
                            now
                    );
                }
            }

            case COMPLETED -> {

                if (item.getStartedAt() == null) {
                    item.setStartedAt(
                            now
                    );
                }

                item.setCompletedAt(
                        now
                );
            }

            case CANCELLED -> {

                item.setCancelledAt(
                        now
                );

                item.setCancellationReason(
                        normalize(
                                request.getCancellationReason()
                        )
                );
            }

            default -> {
            }
        }

        item.setStatus(
                newStatus
        );

        item.setUpdatedBy(
                currentUser
        );

        item =
                treatmentPlanItemRepository.save(
                        item
                );

        /*
         * If treatment actually begins,
         * automatically move the plan from
         * APPROVED → IN_PROGRESS.
         *
         * This avoids:
         *
         * Plan = APPROVED
         * Item = IN_PROGRESS
         */
        if (newStatus
                == TreatmentPlanItemStatus.IN_PROGRESS
                &&
                treatmentPlan.getStatus()
                        == TreatmentPlanStatus.APPROVED) {

            TreatmentPlanStatus oldPlanStatus =
                    treatmentPlan.getStatus();

            treatmentPlan.setStatus(
                    TreatmentPlanStatus.IN_PROGRESS
            );

            treatmentPlan.setUpdatedBy(
                    currentUser
            );

            treatmentPlanRepository.save(
                    treatmentPlan
            );

            TreatmentPlanStatusHistory history =
                    TreatmentPlanStatusHistory.builder()
                            .clinic(clinic)
                            .treatmentPlan(
                                    treatmentPlan
                            )
                            .fromStatus(
                                    oldPlanStatus
                            )
                            .toStatus(
                                    TreatmentPlanStatus.IN_PROGRESS
                            )
                            .reason(
                                    "Treatment started"
                            )
                            .changedBy(
                                    currentUser
                            )
                            .build();

            treatmentPlanStatusHistoryRepository
                    .save(history);
        }

        /*
         * CANCELLED items no longer contribute
         * to the treatment plan total.
         */
        if (newStatus
                == TreatmentPlanItemStatus.CANCELLED) {

            recalculateTreatmentPlan(
                    treatmentPlan,
                    currentUser
            );
        }

        return mapItemResponse(
                item
        );
    }

    private void validateItemStatusTransition(
            TreatmentPlanItemStatus currentStatus,
            TreatmentPlanItemStatus newStatus
    ) {

        boolean allowed =
                switch (currentStatus) {

                    case PLANNED ->
                            newStatus
                                    == TreatmentPlanItemStatus.SCHEDULED
                                    ||
                                    newStatus
                                            == TreatmentPlanItemStatus.IN_PROGRESS
                                    ||
                                    newStatus
                                            == TreatmentPlanItemStatus.CANCELLED;

                    case SCHEDULED ->
                            newStatus
                                    == TreatmentPlanItemStatus.IN_PROGRESS
                                    ||
                                    newStatus
                                            == TreatmentPlanItemStatus.CANCELLED;

                    case IN_PROGRESS ->
                            newStatus
                                    == TreatmentPlanItemStatus.COMPLETED
                                    ||
                                    newStatus
                                            == TreatmentPlanItemStatus.CANCELLED;

                    case COMPLETED,
                         CANCELLED ->
                            false;
                };

        if (!allowed) {

            throw new IllegalArgumentException(
                    "Invalid treatment plan item status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }
}