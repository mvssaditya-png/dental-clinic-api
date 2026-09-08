package com.dentalclinic.casesheet.repository;

import com.dentalclinic.casesheet.entity.CaseSheet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CaseSheetRepository
        extends JpaRepository<CaseSheet, UUID> {

    boolean existsByConsultationId(UUID consultationId);

    boolean existsByClinicIdAndCaseSheetNumber(
            UUID clinicId,
            String caseSheetNumber
    );

    long countByClinicId(UUID clinicId);

    long countByPatientId(UUID patientId);

    @Query("""
        SELECT cs
        FROM CaseSheet cs
        JOIN FETCH cs.clinic cl
        JOIN FETCH cs.consultation c
        JOIN FETCH cs.appointment a
        JOIN FETCH cs.patient p
        JOIN FETCH cs.doctor d
        JOIN FETCH d.user du
        LEFT JOIN FETCH cs.department dep
        WHERE cs.id = :caseSheetId
          AND cl.id = :clinicId
    """)
    Optional<CaseSheet> findByIdAndClinicIdWithDetails(
            @Param("caseSheetId") UUID caseSheetId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
        SELECT cs
        FROM CaseSheet cs
        JOIN FETCH cs.clinic cl
        JOIN FETCH cs.consultation c
        JOIN FETCH cs.appointment a
        JOIN FETCH cs.patient p
        JOIN FETCH cs.doctor d
        JOIN FETCH d.user du
        LEFT JOIN FETCH cs.department dep
        WHERE c.id = :consultationId
          AND cl.id = :clinicId
    """)
    Optional<CaseSheet> findByConsultationIdAndClinicIdWithDetails(
            @Param("consultationId") UUID consultationId,
            @Param("clinicId") UUID clinicId
    );
}