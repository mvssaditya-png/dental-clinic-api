package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.ProcedureMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcedureMasterRepository
        extends JpaRepository<ProcedureMaster, UUID> {

    boolean existsByClinicIdAndProcedureCode(
            UUID clinicId,
            String procedureCode
    );

    Optional<ProcedureMaster> findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );

    @Query("""
        SELECT pm
        FROM ProcedureMaster pm
        LEFT JOIN FETCH pm.department d
        WHERE pm.clinic.id = :clinicId
          AND pm.active = true
        ORDER BY pm.procedureName ASC
    """)
    List<ProcedureMaster> findActiveByClinicId(
            @Param("clinicId") UUID clinicId
    );

    @Query("""
        SELECT pm
        FROM ProcedureMaster pm
        LEFT JOIN FETCH pm.department d
        JOIN FETCH pm.clinic c
        WHERE pm.id = :procedureId
          AND c.id = :clinicId
    """)
    Optional<ProcedureMaster> findByIdAndClinicIdWithDetails(
            @Param("procedureId") UUID procedureId,
            @Param("clinicId") UUID clinicId
    );
}