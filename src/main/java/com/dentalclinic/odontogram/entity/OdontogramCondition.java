package com.dentalclinic.odontogram.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "odontogram_condition",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_odontogram_condition_code",
                        columnNames = {
                                "clinic_id",
                                "condition_code"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdontogramCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "clinic_id",
            nullable = false
    )
    private Clinic clinic;

    @Column(
            name = "condition_code",
            nullable = false,
            length = 100
    )
    private String conditionCode;

    @Column(
            name = "condition_name",
            nullable = false,
            length = 150
    )
    private String conditionName;

    @Column(name = "description")
    private String description;

    @Column(name = "patient_explanation")
    private String patientExplanation;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean active;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (active == null) {
            active = true;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}