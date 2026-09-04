package com.dentalclinic.patient.repository;

import com.dentalclinic.patient.entity.PatientActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatientActivityRepository
        extends JpaRepository<PatientActivity, UUID> {

    List<PatientActivity>
    findAllByPatientIdOrderByCreatedAtDesc(
            UUID patientId
    );
}