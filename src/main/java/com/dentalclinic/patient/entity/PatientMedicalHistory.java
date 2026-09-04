package com.dentalclinic.patient.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "patient_medical_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_patient_medical_history",
                        columnNames = "patient_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private Boolean diabetes;

    @Column(nullable = false)
    private Boolean hypertension;

    @Column(nullable = false)
    private Boolean thyroid;

    @Column(name = "heart_disease", nullable = false)
    private Boolean heartDisease;

    @Column(name = "kidney_disease", nullable = false)
    private Boolean kidneyDisease;

    @Column(nullable = false)
    private Boolean pregnancy;

    @Column(nullable = false)
    private Boolean asthma;

    @Column(nullable = false)
    private Boolean allergies;

    @Column(nullable = false)
    private Boolean tobacco;

    @Column(nullable = false)
    private Boolean alcohol;

    @Column(nullable = false)
    private Boolean smoking;

    @Column(name = "allergy_notes", columnDefinition = "TEXT")
    private String allergyNotes;

    @Column(name = "other_conditions", columnDefinition = "TEXT")
    private String otherConditions;

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

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (diabetes == null) diabetes = false;
        if (hypertension == null) hypertension = false;
        if (thyroid == null) thyroid = false;
        if (heartDisease == null) heartDisease = false;
        if (kidneyDisease == null) kidneyDisease = false;
        if (pregnancy == null) pregnancy = false;
        if (asthma == null) asthma = false;
        if (allergies == null) allergies = false;
        if (tobacco == null) tobacco = false;
        if (alcohol == null) alcohol = false;
        if (smoking == null) smoking = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}