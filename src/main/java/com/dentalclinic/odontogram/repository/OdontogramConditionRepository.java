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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
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