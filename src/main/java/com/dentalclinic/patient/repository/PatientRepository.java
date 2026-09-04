package com.dentalclinic.patient.repository;

import com.dentalclinic.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository
        extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );

    List<Patient> findAllByClinicIdAndActiveTrue(
            UUID clinicId
    );

    List<Patient> findAllByClinicIdAndPhone(
            UUID clinicId,
            String phone
    );

    boolean existsByClinicIdAndPatientNumber(
            UUID clinicId,
            String patientNumber
    );

    boolean existsByClinicIdAndAadhaarNumber(
            UUID clinicId,
            String aadhaarNumber
    );
}