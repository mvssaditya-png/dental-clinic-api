package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.TreatmentPlanStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TreatmentPlanStatusHistoryRepository
        extends JpaRepository<TreatmentPlanStatusHistory, UUID> {

    List<TreatmentPlanStatusHistory>
    findAllByTreatmentPlanIdOrderByChangedAtAsc(
            UUID treatmentPlanId
    );
}