package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.ConditionProcedureMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConditionProcedureMappingRepository
        extends JpaRepository<ConditionProcedureMapping, UUID> {

    boolean existsByConditionIdAndProcedureId(
            UUID conditionId,
            UUID procedureId
    );

    @Query("""
    SELECT m
    FROM ConditionProcedureMapping m
    JOIN FETCH m.condition c
    JOIN FETCH m.procedure p
    LEFT JOIN FETCH p.department d
    WHERE m.clinic.id = :clinicId
      AND c.id = :conditionId
      AND m.active = true
      AND p.active = true
    ORDER BY m.primary DESC,
             m.displayOrder ASC,
             p.procedureName ASC
""")
    List<ConditionProcedureMapping> findActiveSuggestions(
            @Param("clinicId") UUID clinicId,
            @Param("conditionId") UUID conditionId
    );
}