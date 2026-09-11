package com.dentalclinic.treatment.entity;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.consultation.entity.Consultation;
import com.dentalclinic.doctor.entity.DoctorProfile;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "treatment_plan",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_treatment_plan_number",
                        columnNames = {"clinic_id", "treatment_plan_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentPlan {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_sheet_id")
    private CaseSheet caseSheet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @Column(
            name = "treatment_plan_number",
            nullable = false,
            length = 50
    )
    private String treatmentPlanNumber;

    @Column(length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TreatmentPlanStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

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

    @Column(name = "estimated_total_visits")
    private Integer estimatedTotalVisits;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "patient_notes", columnDefinition = "text")
    private String patientNotes;

    @Column(name = "presented_at")
    private LocalDateTime presentedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "approval_notes", columnDefinition = "text")
    private String approvalNotes;

    @Column(name = "decline_reason", columnDefinition = "text")
    private String declineReason;

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

        if (status == null) {
            status = TreatmentPlanStatus.DRAFT;
        }

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
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