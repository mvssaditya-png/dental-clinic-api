package com.dentalclinic.odontogram.repository;

import com.dentalclinic.odontogram.entity.Odontogram;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OdontogramRepository
        extends JpaRepository<Odontogram, UUID> {

    Optional<Odontogram>
    findByPatientIdAndClinicId(
            UUID patientId,
            UUID clinicId
    );

    boolean existsByPatientId(UUID patientId);

    Optional<Odontogram> findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );
}