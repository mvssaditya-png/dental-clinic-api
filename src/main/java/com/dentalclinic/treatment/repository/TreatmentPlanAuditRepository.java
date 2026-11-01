package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.TreatmentPlanAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import java.util.UUID;

/** No update/delete operations; database triggers independently enforce append-only storage. */
public interface TreatmentPlanAuditRepository extends Repository<TreatmentPlanAudit, UUID> {
    TreatmentPlanAudit save(TreatmentPlanAudit event);
    Page<TreatmentPlanAudit> findByClinicIdAndTreatmentPlanId(UUID clinicId, UUID treatmentPlanId, Pageable page);
}
