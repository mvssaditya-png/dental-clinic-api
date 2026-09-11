package com.dentalclinic.treatment.dto;

import com.dentalclinic.treatment.entity.DiscountType;
import com.dentalclinic.treatment.entity.TreatmentPlanItemStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TreatmentPlanItemResponse {

    private UUID id;

    private UUID treatmentPlanId;

    private UUID procedureId;

    private String procedureCode;
    private String procedureName;

    private String toothNumber;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal grossAmount;

    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private Integer estimatedVisits;

    private TreatmentPlanItemStatus status;

    private Integer sequenceNumber;

    private String clinicalNotes;

    private String patientExplanation;

    private String followUpInstructions;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    private String cancellationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}