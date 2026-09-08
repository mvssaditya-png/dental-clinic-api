package com.dentalclinic.casesheet.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "case_sheet_clinical_finding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseSheetClinicalFinding {

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
            name = "finding_type",
            nullable = false,
            length = 100
    )
    private String findingType;

    @Column(
            name = "finding_value",
            length = 255
    )
    private String findingValue;

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

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}