package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.TreatmentPlan;
import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentPlanRepository
        extends JpaRepository<TreatmentPlan, UUID> {

    boolean existsByClinicIdAndTreatmentPlanNumber(
            UUID clinicId,
            String treatmentPlanNumber
    );

    long countByClinicId(UUID clinicId);

    @Query("""
        SELECT tp
        FROM TreatmentPlan tp
        JOIN FETCH tp.clinic c
        JOIN FETCH tp.patient p
        JOIN FETCH tp.doctor d
        JOIN FETCH d.user du
        LEFT JOIN FETCH tp.appointment a
        LEFT JOIN FETCH tp.consultation con
        LEFT JOIN FETCH tp.caseSheet cs
        WHERE tp.id = :treatmentPlanId
          AND c.id = :clinicId
    """)
    Optional<TreatmentPlan> findByIdAndClinicIdWithDetails(
            @Param("treatmentPlanId")
            UUID treatmentPlanId,
            @Param("clinicId")
            UUID clinicId
    );

    @Query("""
        SELECT tp
        FROM TreatmentPlan tp
        JOIN FETCH tp.clinic c
        JOIN FETCH tp.patient p
        JOIN FETCH tp.doctor d
        JOIN FETCH d.user du
        LEFT JOIN FETCH tp.appointment a
        LEFT JOIN FETCH tp.consultation con
        LEFT JOIN FETCH tp.caseSheet cs
        WHERE p.id = :patientId
          AND c.id = :clinicId
        ORDER BY tp.createdAt DESC
    """)
    List<TreatmentPlan>
    findAllByPatientAndClinicWithDetails(
            @Param("patientId")
            UUID patientId,
            @Param("clinicId")
            UUID clinicId
    );

    @Query("""
        SELECT tp
        FROM TreatmentPlan tp
        JOIN FETCH tp.clinic c
        JOIN FETCH tp.patient p
        JOIN FETCH tp.doctor d
        JOIN FETCH d.user du
        LEFT JOIN FETCH tp.appointment a
        LEFT JOIN FETCH tp.consultation con
        LEFT JOIN FETCH tp.caseSheet cs
        WHERE cs.id = :caseSheetId
          AND c.id = :clinicId
        ORDER BY tp.createdAt DESC
    """)
    List<TreatmentPlan>
    findAllByCaseSheetAndClinicWithDetails(
            @Param("caseSheetId")
            UUID caseSheetId,
            @Param("clinicId")
            UUID clinicId
    );

    boolean existsByCaseSheetIdAndStatusNot(
            UUID caseSheetId,
            TreatmentPlanStatus status
    );
}