package com.dentalclinic.clinic.repository;

import com.dentalclinic.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findByClinicCode(String clinicCode);

    Optional<Clinic> findByEmailIgnoreCase(String email);

    boolean existsByClinicCode(String clinicCode);
}