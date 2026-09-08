package com.dentalclinic.odontogram.entity;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.consultation.entity.Consultation;
import com.dentalclinic.user.entity.AppUser;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "odontogram_tooth_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdontogramToothHistory {

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "odontogram_tooth_id",
            nullable = false
    )
    private OdontogramTooth odontogramTooth;

    @Column(
            name = "tooth_number",
            nullable = false,
            length = 10
    )
    private String toothNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_condition_id")
    private OdontogramCondition previousCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_condition_id")
    private OdontogramCondition newCondition;

    @Column(name = "previous_notes")
    private String previousNotes;

    @Column(name = "new_notes")
    private String newNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_sheet_id")
    private CaseSheet caseSheet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultation_id")
    private Consultation consultation;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "change_type",
            nullable = false,
            length = 30
    )
    private OdontogramChangeType changeType;

    @Column(name = "change_reason")
    private String changeReason;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "changed_by",
            nullable = false
    )
    private AppUser changedBy;

    @Column(
            name = "changed_at",
            nullable = false
    )
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {

        if (changeType == null) {
            changeType =
                    OdontogramChangeType.UPDATED;
        }

        if (changedAt == null) {
            changedAt =
                    LocalDateTime.now();
        }
    }
}