package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.TreatmentPlanStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TreatmentPlanStatusHistoryRepository
        extends JpaRepository<TreatmentPlanStatusHistory, UUID> {

    @org.springframework.data.jpa.repository.Query(value = """
        select h from TreatmentPlanStatusHistory h left join fetch h.changedBy
        where h.clinic.id = :clinicId and h.treatmentPlan.id = :planId
          and h.treatmentPlan.clinic.id = :clinicId
        order by h.changedAt desc, h.id desc
        """, countQuery = """
        select count(h) from TreatmentPlanStatusHistory h
        where h.clinic.id = :clinicId and h.treatmentPlan.id = :planId
          and h.treatmentPlan.clinic.id = :clinicId
        """)
    org.springframework.data.domain.Page<TreatmentPlanStatusHistory> readLifecycle(
        @org.springframework.data.repository.query.Param("clinicId") UUID clinicId,
        @org.springframework.data.repository.query.Param("planId") UUID planId,
        org.springframework.data.domain.Pageable pageable);

    List<TreatmentPlanStatusHistory>
    findAllByTreatmentPlanIdOrderByChangedAtAsc(
            UUID treatmentPlanId
    );
}