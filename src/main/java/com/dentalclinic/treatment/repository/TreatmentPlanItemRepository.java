package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.TreatmentPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentPlanItemRepository
        extends JpaRepository<TreatmentPlanItem, UUID> {

    @Query("""
        SELECT i
        FROM TreatmentPlanItem i
        JOIN FETCH i.procedure p
        LEFT JOIN FETCH i.condition c
        LEFT JOIN FETCH i.odontogramTooth ot
        WHERE i.treatmentPlan.id = :treatmentPlanId
        ORDER BY i.sequenceNumber ASC,
                 i.createdAt ASC
    """)
    List<TreatmentPlanItem>
    findAllByTreatmentPlanIdWithDetails(
            @Param("treatmentPlanId")
            UUID treatmentPlanId
    );

    @Query("""
        SELECT i
        FROM TreatmentPlanItem i
        JOIN FETCH i.treatmentPlan tp
        JOIN FETCH i.procedure p
        LEFT JOIN FETCH i.condition c
        LEFT JOIN FETCH i.odontogramTooth ot
        WHERE i.id = :itemId
          AND tp.clinic.id = :clinicId
    """)
    Optional<TreatmentPlanItem>
    findByIdAndClinicIdWithDetails(
            @Param("itemId")
            UUID itemId,
            @Param("clinicId")
            UUID clinicId
    );

    long countByTreatmentPlanId(
            UUID treatmentPlanId
    );
}