package com.dentalclinic.treatment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Append-only persistence foundation. No current mutation writes plan audit events. */
@Immutable
@Entity
@Table(name = "treatment_plan_audit")
@Getter
@Setter
@NoArgsConstructor
public class TreatmentPlanAudit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID clinicId;
    private UUID treatmentPlanId;
    private String operation;
    private UUID actorId;
    private LocalDateTime createdAt;
    @Column(columnDefinition = "text")
    private String removalReason;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private Map<String, Object> planSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb")
    private List<RetainedItem> retainedItems;

    public record RetainedItem(UUID itemId, long version) {}

    @PrePersist
    void timestamp() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
