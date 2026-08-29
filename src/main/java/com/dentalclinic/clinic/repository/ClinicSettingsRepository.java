package com.dentalclinic.clinic.repository;

import com.dentalclinic.clinic.entity.ClinicSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicSettingsRepository
        extends JpaRepository<ClinicSettings, UUID> {

    Optional<ClinicSettings> findByClinicId(UUID clinicId);

    boolean existsByClinicId(UUID clinicId);
}