package com.dentalclinic.consultation.repository;

import com.dentalclinic.consultation.entity.Consultation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository
        extends JpaRepository<Consultation, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consultation c where c.id=:id and c.clinic.id=:clinicId")
    Optional<Consultation> lockByIdAndClinicId(@Param("id") UUID id,@Param("clinicId") UUID clinicId);

    // Null legacy starts sort after dated visits, then by UUID; no synthetic timestamp is returned.
    @Query(value = """
        SELECT new com.dentalclinic.consultation.dto.ClinicalVisitSummary(
            c.id, c.status, c.startedAt, c.endedAt, a.id, a.appointmentNumber,
            a.appointmentDate, a.startTime, d.id,
            trim(concat(coalesce(u.firstName, ''), ' ', coalesce(u.lastName, ''))),
            cs.id, cs.caseSheetNumber, cs.status, cs.visitNumber, cs.chiefComplaint)
        FROM Consultation c
        JOIN c.appointment a
        JOIN c.doctor d
        JOIN d.user u
        LEFT JOIN CaseSheet cs ON cs.consultation.id = c.id
            AND cs.clinic.id = :clinicId AND cs.patient.id = :patientId
        WHERE c.clinic.id = :clinicId AND c.patient.id = :patientId
            AND a.clinic.id = :clinicId AND a.patient.id = :patientId
            AND d.clinic.id = :clinicId
            AND (:excluded IS NULL OR c.id <> :excluded)
        ORDER BY c.startedAt DESC NULLS LAST, c.id DESC
        """, countQuery = """
        SELECT count(c) FROM Consultation c
        JOIN c.appointment a JOIN c.doctor d JOIN d.user u
        WHERE c.clinic.id = :clinicId AND c.patient.id = :patientId
            AND a.clinic.id = :clinicId AND a.patient.id = :patientId
            AND d.clinic.id = :clinicId
            AND (:excluded IS NULL OR c.id <> :excluded)
        """)
    org.springframework.data.domain.Page<com.dentalclinic.consultation.dto.ClinicalVisitSummary> readClinicalVisits(
            @Param("clinicId") UUID clinicId, @Param("patientId") UUID patientId,
            @Param("excluded") UUID excluded, org.springframework.data.domain.Pageable pageable);
    boolean existsByAppointmentId(UUID appointmentId);

    @Query("""
        SELECT c
        FROM Consultation c
        JOIN FETCH c.clinic cl
        JOIN FETCH c.appointment a
        JOIN FETCH c.patient p
        JOIN FETCH c.doctor d
        JOIN FETCH d.user du
        WHERE c.id = :consultationId
          AND cl.id = :clinicId
    """)
    Optional<Consultation> findByIdAndClinicIdWithDetails(
            @Param("consultationId") UUID consultationId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
        SELECT c
        FROM Consultation c
        JOIN FETCH c.clinic cl
        JOIN FETCH c.appointment a
        JOIN FETCH c.patient p
        JOIN FETCH c.doctor d
        JOIN FETCH d.user du
        WHERE a.id = :appointmentId
          AND cl.id = :clinicId
    """)
    Optional<Consultation> findByAppointmentIdAndClinicIdWithDetails(
            @Param("appointmentId") UUID appointmentId,
            @Param("clinicId") UUID clinicId
    );

    Optional<Consultation> findByAppointmentIdAndClinicId(
            UUID appointmentId,
            UUID clinicId
    );

 @Query(value="""
 SELECT new com.dentalclinic.consultation.dto.ConsultationSummary(c.id,c.clinic.id,a.id,a.appointmentNumber,
 p.id,trim(concat(concat(coalesce(p.firstName,''),' '),coalesce(p.lastName,''))),p.patientNumber,
 trim(concat(concat(coalesce(u.firstName,''),' '),coalesce(u.lastName,''))),c.status,a.status,c.startedAt,c.endedAt,a.appointmentDate)

 FROM Consultation c JOIN c.appointment a JOIN c.patient p JOIN c.doctor d JOIN d.user u
 WHERE c.clinic.id=:clinicId AND a.clinic.id=:clinicId AND p.clinic.id=:clinicId AND d.clinic.id=:clinicId
 AND a.patient.id=p.id
 AND (:status IS NULL OR c.status=:status)
 AND (cast(:fromDate as date) IS NULL OR a.appointmentDate>=:fromDate) AND (cast(:toDate as date) IS NULL OR a.appointmentDate<=:toDate)
 AND (:pattern IS NULL OR lower(concat(concat(coalesce(p.firstName,''),' '),coalesce(p.lastName,''))) LIKE :pattern ESCAPE '!'
 OR lower(p.patientNumber) LIKE :pattern ESCAPE '!' OR lower(a.appointmentNumber) LIKE :pattern ESCAPE '!'
 OR lower(cast(c.id as String)) LIKE :pattern ESCAPE '!')

 ORDER BY c.startedAt DESC NULLS LAST,c.id DESC
 """,countQuery="""
 SELECT count(c)
 FROM Consultation c JOIN c.appointment a JOIN c.patient p JOIN c.doctor d JOIN d.user u
 WHERE c.clinic.id=:clinicId AND a.clinic.id=:clinicId AND p.clinic.id=:clinicId AND d.clinic.id=:clinicId
 AND a.patient.id=p.id
 AND (:status IS NULL OR c.status=:status)
 AND (cast(:fromDate as date) IS NULL OR a.appointmentDate>=:fromDate) AND (cast(:toDate as date) IS NULL OR a.appointmentDate<=:toDate)
 AND (:pattern IS NULL OR lower(concat(concat(coalesce(p.firstName,''),' '),coalesce(p.lastName,''))) LIKE :pattern ESCAPE '!'
 OR lower(p.patientNumber) LIKE :pattern ESCAPE '!' OR lower(a.appointmentNumber) LIKE :pattern ESCAPE '!'
 OR lower(cast(c.id as String)) LIKE :pattern ESCAPE '!')

 """)
 org.springframework.data.domain.Page<com.dentalclinic.consultation.dto.ConsultationSummary> discover(
 @Param("clinicId") UUID clinicId,@Param("pattern") String pattern,
 @Param("status") com.dentalclinic.consultation.entity.ConsultationStatus status,
 @Param("fromDate") java.time.LocalDate from,@Param("toDate") java.time.LocalDate to,
 org.springframework.data.domain.Pageable pageable);
}
