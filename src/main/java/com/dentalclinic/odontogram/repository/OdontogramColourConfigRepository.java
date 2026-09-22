package com.dentalclinic.odontogram.repository;

import com.dentalclinic.odontogram.entity.OdontogramColourConfig;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OdontogramColourConfigRepository
        extends JpaRepository<OdontogramColourConfig, UUID> {

    boolean existsByConditionId(
            UUID conditionId
    );

    Optional<OdontogramColourConfig>
    findByConditionIdAndClinicId(
            UUID conditionId,
            UUID clinicId
    );

    Optional<OdontogramColourConfig>
    findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );

    @Query("""
            SELECT c
            FROM OdontogramColourConfig c
            JOIN FETCH c.condition condition
            WHERE c.clinic.id = :clinicId
              AND c.active = true
              AND condition.active = true
            ORDER BY c.displayOrder ASC,
                     condition.conditionName ASC
            """)
    List<OdontogramColourConfig>
    findAllActiveWithCondition(
            @Param("clinicId") UUID clinicId
    );
}