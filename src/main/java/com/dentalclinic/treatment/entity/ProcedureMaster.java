package com.dentalclinic.treatment.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.department.entity.Department;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "procedure_master",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_procedure_master_code",
                        columnNames = {"clinic_id", "procedure_code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcedureMaster {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "procedure_code", nullable = false, length = 50)
    private String procedureCode;

    @Column(name = "procedure_name", nullable = false, length = 200)
    private String procedureName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "patient_explanation", columnDefinition = "text")
    private String patientExplanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "default_duration_minutes")
    private Integer defaultDurationMinutes;

    @Column(name = "default_estimated_visits")
    private Integer defaultEstimatedVisits;

    @Column(name = "default_notes", columnDefinition = "text")
    private String defaultNotes;

    @Column(name = "follow_up_instructions", columnDefinition = "text")
    private String followUpInstructions;

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