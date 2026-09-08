package com.dentalclinic.casesheet.entity;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.consultation.entity.Consultation;
import com.dentalclinic.doctor.entity.DoctorProfile;
import com.dentalclinic.department.entity.Department;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.user.entity.AppUser;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "case_sheet",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_case_sheet_consultation",
                        columnNames = "consultation_id"
                ),
                @UniqueConstraint(
                        name = "uq_case_sheet_number",
                        columnNames = {"clinic_id", "case_sheet_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseSheet {

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

    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "consultation_id",
            nullable = false,
            unique = true
    )
    private Consultation consultation;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "appointment_id",
            nullable = false
    )
    private Appointment appointment;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_id",
            nullable = false
    )
    private Patient patient;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "doctor_id",
            nullable = false
    )
    private DoctorProfile doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(
            name = "case_sheet_number",
            nullable = false,
            length = 50
    )
    private String caseSheetNumber;

    @Column(
            name = "visit_number",
            nullable = false
    )
    private Integer visitNumber;

    @Column(name = "chief_complaint")
    private String chiefComplaint;

    @Column(name = "history_of_present_illness")
    private String historyOfPresentIllness;

    @Column(name = "clinical_examination")
    private String clinicalExamination;

    @Column(name = "diagnosis_summary")
    private String diagnosisSummary;

    @Column(name = "treatment_notes")
    private String treatmentNotes;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private CaseSheetStatus status;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_by")
    private AppUser finalizedBy;

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

        if (visitNumber == null) {
            visitNumber = 1;
        }

        if (status == null) {
            status = CaseSheetStatus.DRAFT;
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