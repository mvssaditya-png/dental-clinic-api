package com.dentalclinic.treatment.repository;
import com.dentalclinic.treatment.entity.TreatmentPlanItemAudit; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.domain.*;
public interface TreatmentPlanItemAuditRepository extends JpaRepository<TreatmentPlanItemAudit,UUID> {
 Page<TreatmentPlanItemAudit> findByClinicIdAndTreatmentPlanId(UUID clinicId,UUID treatmentPlanId,Pageable page);
}
