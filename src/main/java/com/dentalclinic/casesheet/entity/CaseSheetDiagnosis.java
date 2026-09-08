package com.dentalclinic.casesheet.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "case_sheet_diagnosis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseSheetDiagnosis {

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
            name = "case_sheet_id",
            nullable = false
    )
    private CaseSheet caseSheet;

    @Column(
            name = "tooth_number",
            length = 10
    )
    private String toothNumber;

    @Column(
            name = "diagnosis_code",
            length = 50
    )
    private String diagnosisCode;

    @Column(
            name = "diagnosis_name",
            nullable = false,
            length = 255
    )
    private String diagnosisName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "diagnosis_type",
            nullable = false,
            length = 30
    )
    private DiagnosisType diagnosisType;

    @Column(name = "notes")
    private String notes;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @PrePersist
    protected void onCreate() {

        if (diagnosisType == null) {
            diagnosisType = DiagnosisType.PRIMARY;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}