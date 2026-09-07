package com.dentalclinic.appointment.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dental_chair",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_dental_chair_code",
                        columnNames = {
                                "clinic_id",
                                "chair_code"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_dental_chair_name",
                        columnNames = {
                                "clinic_id",
                                "chair_name"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DentalChair {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(
            name = "chair_code",
            nullable = false,
            length = 50
    )
    private String chairCode;

    @Column(
            name = "chair_name",
            nullable = false,
            length = 100
    )
    private String chairName;

    @Column(length = 255)
    private String location;

    @Column(length = 255)
    private String description;

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

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}