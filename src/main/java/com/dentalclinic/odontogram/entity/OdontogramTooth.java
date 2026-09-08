package com.dentalclinic.odontogram.entity;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "odontogram_tooth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_odontogram_tooth",
                        columnNames = {
                                "odontogram_id",
                                "tooth_number"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdontogramTooth {

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "odontogram_id",
            nullable = false
    )
    private Odontogram odontogram;

    @Column(
            name = "tooth_number",
            nullable = false,
            length = 10
    )
    private String toothNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    private OdontogramCondition condition;

    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_case_sheet_id")
    private CaseSheet lastCaseSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_appointment_id")
    private Appointment lastAppointment;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_changed_by")
    private AppUser lastChangedBy;

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

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

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