package com.dentalclinic.treatment.entity;
import jakarta.persistence.*; import lombok.*; import java.util.*; import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.Immutable
@Entity @Table(name="treatment_plan_item_audit") @Getter @Setter @NoArgsConstructor
public class TreatmentPlanItemAudit {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 private UUID clinicId; private UUID treatmentPlanId; private UUID itemId; private String operation; private UUID actorId;
 private LocalDateTime createdAt;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> beforeSnapshot;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> afterSnapshot;
 @Column(columnDefinition="text") private String removalReason;
 @PrePersist void timestamp(){createdAt=LocalDateTime.now();}
}
