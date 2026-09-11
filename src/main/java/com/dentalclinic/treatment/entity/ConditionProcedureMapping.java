package com.dentalclinic.treatment.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.odontogram.entity.OdontogramCondition;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "condition_procedure_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_condition_procedure",
                        columnNames = {"condition_id", "procedure_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionProcedureMapping {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_id", nullable = false)
    private OdontogramCondition condition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedure_id", nullable = false)
    private ProcedureMaster procedure;

    @Column(name = "is_primary", nullable = false)
    private Boolean primary;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "default_notes", columnDefinition = "text")
    private String defaultNotes;

    @Column(name = "patient_explanation", columnDefinition = "text")
    private String patientExplanation;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

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

        if (primary == null) {
            primary = false;
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        if (active == null) {
            active = true;
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