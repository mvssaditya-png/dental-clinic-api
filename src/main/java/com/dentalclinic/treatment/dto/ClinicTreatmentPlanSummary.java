package com.dentalclinic.treatment.dto;
import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
public record ClinicTreatmentPlanSummary(UUID id, UUID clinicId, UUID patientId,
 String patientName, String patientNumber, String treatmentPlanNumber, String title,
 TreatmentPlanStatus status, UUID caseSheetId, UUID consultationId,
 Integer estimatedTotalVisits, BigDecimal finalAmount, LocalDateTime createdAt, LocalDateTime updatedAt) {}
