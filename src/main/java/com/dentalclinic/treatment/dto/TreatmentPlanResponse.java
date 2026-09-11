package com.dentalclinic.treatment.dto;

import com.dentalclinic.treatment.entity.DiscountType;
import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TreatmentPlanResponse {

    private UUID id;
    private UUID clinicId;

    private String treatmentPlanNumber;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private UUID doctorId;
    private String doctorName;

    private UUID appointmentId;
    private String appointmentNumber;

    private UUID consultationId;

    private UUID caseSheetId;
    private String caseSheetNumber;

    private String title;

    private TreatmentPlanStatus status;

    private BigDecimal subtotal;

    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private Integer estimatedTotalVisits;

    private String notes;
    private String patientNotes;

    private LocalDateTime presentedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime declinedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    private String approvalNotes;
    private String declineReason;
    private String cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}