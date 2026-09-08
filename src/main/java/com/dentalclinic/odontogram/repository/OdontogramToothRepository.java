package com.dentalclinic.odontogram.repository;

import com.dentalclinic.odontogram.entity.OdontogramTooth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OdontogramToothRepository
        extends JpaRepository<OdontogramTooth, UUID> {

    List<OdontogramTooth>
    findAllByOdontogramIdOrderByToothNumberAsc(
            UUID odontogramId
    );

    Optional<OdontogramTooth>
    findByOdontogramIdAndToothNumber(
            UUID odontogramId,
            String toothNumber
    );
}