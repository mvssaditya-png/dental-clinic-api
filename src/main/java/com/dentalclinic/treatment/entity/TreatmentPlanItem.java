package com.dentalclinic.treatment.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.odontogram.entity.OdontogramCondition;
import com.dentalclinic.odontogram.entity.OdontogramTooth;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "treatment_plan_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentPlanItem {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "treatment_plan_id", nullable = false)
    private TreatmentPlan treatmentPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedure_id", nullable = false)
    private ProcedureMaster procedure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontogram_tooth_id")
    private OdontogramTooth odontogramTooth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    private OdontogramCondition condition;

    @Column(name = "tooth_number", length = 10)
    private String toothNumber;

    @Column(
            name = "procedure_code_snapshot",
            nullable = false,
            length = 50
    )
    private String procedureCodeSnapshot;

    @Column(
            name = "procedure_name_snapshot",
            nullable = false,
            length = 200
    )
    private String procedureNameSnapshot;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
            name = "gross_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal grossAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(
            name = "discount_value",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountValue;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;

    @Column(
            name = "final_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal finalAmount;

    @Column(name = "estimated_visits")
    private Integer estimatedVisits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TreatmentPlanItemStatus status;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "clinical_notes", columnDefinition = "text")
    private String clinicalNotes;

    @Column(name = "patient_explanation", columnDefinition = "text")
    private String patientExplanation;

    @Column(name = "follow_up_instructions", columnDefinition = "text")
    private String followUpInstructions;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (quantity == null) {
            quantity = 1;
        }

        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }

        if (grossAmount == null) {
            grossAmount = BigDecimal.ZERO;
        }

        if (discountValue == null) {
            discountValue = BigDecimal.ZERO;
        }

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        if (finalAmount == null) {
            finalAmount = BigDecimal.ZERO;
        }

        if (status == null) {
            status = TreatmentPlanItemStatus.PLANNED;
        }

        if (sequenceNumber == null) {
            sequenceNumber = 0;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}