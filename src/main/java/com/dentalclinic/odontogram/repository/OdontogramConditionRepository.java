package com.dentalclinic.odontogram.repository;

import com.dentalclinic.odontogram.entity.OdontogramCondition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OdontogramConditionRepository
        extends JpaRepository<OdontogramCondition, UUID> {

    List<OdontogramCondition>
    findAllByClinicIdAndActiveTrueOrderByConditionNameAsc(
            UUID clinicId
    );

    Optional<OdontogramCondition>
    findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );

    boolean existsByClinicIdAndConditionCode(
            UUID clinicId,
            String conditionCode
    );
}