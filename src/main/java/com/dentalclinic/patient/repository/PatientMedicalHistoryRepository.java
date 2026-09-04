package com.dentalclinic.patient.repository;

import com.dentalclinic.patient.entity.PatientMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientMedicalHistoryRepository
        extends JpaRepository<PatientMedicalHistory, UUID> {

    Optional<PatientMedicalHistory> findByPatientId(
            UUID patientId
    );
}