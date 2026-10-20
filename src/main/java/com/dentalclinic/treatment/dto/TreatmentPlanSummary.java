package com.dentalclinic.treatment.dto;

import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Discovery projection: no clinical notes or item collections. */
public record TreatmentPlanSummary(UUID id, UUID clinicId, String treatmentPlanNumber,
        String title, UUID patientId, UUID caseSheetId, UUID consultationId,
        TreatmentPlanStatus status, Integer estimatedTotalVisits, BigDecimal finalAmount,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
