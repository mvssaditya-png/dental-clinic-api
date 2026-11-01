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

    // Explicit active/removed reads; unfiltered identity lookup is reserved for recovery/history.
    Optional<TreatmentPlan> findByIdAndClinicIdAndRemovedAtIsNull(UUID id, UUID clinicId);
    Optional<TreatmentPlan> findByIdAndClinicIdAndRemovedAtIsNotNull(UUID id, UUID clinicId);
    Optional<TreatmentPlan> findByIdAndClinicId(UUID id, UUID clinicId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TreatmentPlan p where p.id=:id and p.clinic.id=:clinicId")
    Optional<TreatmentPlan> lockByIdAndClinicId(@Param("id") UUID id,@Param("clinicId") UUID clinicId);

    @Query(value = """
 SELECT new com.dentalclinic.treatment.dto.ClinicTreatmentPlanSummary(
 tp.id, tp.clinic.id, p.id, trim(concat(concat(p.firstName, ' '), coalesce(p.lastName, ''))),
 p.patientNumber, tp.treatmentPlanNumber, tp.title, tp.status, cs.id, con.id,
 tp.estimatedTotalVisits, tp.finalAmount, tp.createdAt, tp.updatedAt)
 FROM TreatmentPlan tp JOIN tp.patient p
 LEFT JOIN tp.caseSheet cs LEFT JOIN tp.consultation con
 WHERE tp.removedAt IS NULL AND tp.clinic.id = :clinicId AND p.clinic.id = :clinicId
 AND (:status IS NULL OR tp.status = :status)
 AND (:pattern IS NULL OR lower(concat(concat(p.firstName, ' '), coalesce(p.lastName, ''))) LIKE :pattern ESCAPE '!'
 OR lower(p.patientNumber) LIKE :pattern ESCAPE '!'
 OR lower(tp.treatmentPlanNumber) LIKE :pattern ESCAPE '!')
 ORDER BY tp.createdAt DESC, tp.id DESC
 """, countQuery = """
 SELECT count(tp) FROM TreatmentPlan tp JOIN tp.patient p
 WHERE tp.removedAt IS NULL AND tp.clinic.id = :clinicId AND p.clinic.id = :clinicId
 AND (:status IS NULL OR tp.status = :status)
 AND (:pattern IS NULL OR lower(concat(concat(p.firstName, ' '), coalesce(p.lastName, ''))) LIKE :pattern ESCAPE '!'
 OR lower(p.patientNumber) LIKE :pattern ESCAPE '!'
 OR lower(tp.treatmentPlanNumber) LIKE :pattern ESCAPE '!')
 """)
 org.springframework.data.domain.Page<com.dentalclinic.treatment.dto.ClinicTreatmentPlanSummary> discoverByClinic(
 @Param("clinicId") UUID clinicId, @Param("pattern") String pattern,
 @Param("status") TreatmentPlanStatus status, org.springframework.data.domain.Pageable pageable);

    @Query(value = """
        SELECT new com.dentalclinic.treatment.dto.TreatmentPlanSummary(
            tp.id, tp.clinic.id, tp.treatmentPlanNumber, tp.title, p.id, cs.id, con.id,
            tp.status, tp.estimatedTotalVisits, tp.finalAmount, tp.createdAt, tp.updatedAt)
        FROM TreatmentPlan tp
        JOIN tp.patient p
        LEFT JOIN tp.caseSheet cs
        LEFT JOIN tp.consultation con
        WHERE tp.removedAt IS NULL AND tp.clinic.id = :clinicId AND p.id = :parentId
        ORDER BY tp.createdAt DESC, tp.id DESC
        """, countQuery = """
        SELECT count(tp) FROM TreatmentPlan tp
        WHERE tp.removedAt IS NULL AND tp.clinic.id = :clinicId AND tp.patient.id = :parentId
        """)
    org.springframework.data.domain.Page<com.dentalclinic.treatment.dto.TreatmentPlanSummary>
    discoverByPatient(@Param("parentId") UUID parentId, @Param("clinicId") UUID clinicId,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = """
        SELECT new com.dentalclinic.treatment.dto.TreatmentPlanSummary(
            tp.id, tp.clinic.id, tp.treatmentPlanNumber, tp.title, p.id, cs.id, con.id,
            tp.status, tp.estimatedTotalVisits, tp.finalAmount, tp.createdAt, tp.updatedAt)
        FROM TreatmentPlan tp
        JOIN tp.patient p
        LEFT JOIN tp.caseSheet cs
        LEFT JOIN tp.consultation con
        WHERE tp.removedAt IS NULL AND tp.clinic.id = :clinicId AND cs.id = :parentId
        ORDER BY tp.createdAt DESC, tp.id DESC
        """, countQuery = """
        SELECT count(tp) FROM TreatmentPlan tp
        WHERE tp.removedAt IS NULL AND tp.clinic.id = :clinicId AND tp.caseSheet.id = :parentId
        """)
    org.springframework.data.domain.Page<com.dentalclinic.treatment.dto.TreatmentPlanSummary>
    discoverByCaseSheet(@Param("parentId") UUID parentId, @Param("clinicId") UUID clinicId,
            org.springframework.data.domain.Pageable pageable);

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
          AND c.id = :clinicId AND tp.removedAt IS NULL
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
          AND c.id = :clinicId AND tp.removedAt IS NULL
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
          AND c.id = :clinicId AND tp.removedAt IS NULL
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
