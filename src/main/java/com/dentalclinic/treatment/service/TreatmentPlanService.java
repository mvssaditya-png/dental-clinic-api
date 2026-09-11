package com.dentalclinic.treatment.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.dto.CreateTreatmentPlanItemRequest;
import com.dentalclinic.treatment.dto.CreateTreatmentPlanRequest;
import com.dentalclinic.treatment.dto.TreatmentPlanItemResponse;
import com.dentalclinic.treatment.dto.TreatmentPlanResponse;
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
}