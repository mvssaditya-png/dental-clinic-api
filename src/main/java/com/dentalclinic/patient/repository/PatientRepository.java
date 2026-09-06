package com.dentalclinic.patient.repository;

import com.dentalclinic.patient.entity.Patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT p
        FROM Patient p
        WHERE p.clinic.id = :clinicId
          AND p.active = true
          AND (
                :search IS NULL
                OR :search = ''
                OR LOWER(p.patientNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(p.lastName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                OR p.phone LIKE CONCAT('%', :search, '%')
                OR LOWER(
                    CONCAT(
                        p.firstName,
                        ' ',
                        COALESCE(p.lastName, '')
                    )
                ) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY p.createdAt DESC
        """)
    Page<Patient> searchPatients(
            @Param("clinicId") UUID clinicId,
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByClinicIdAndAadhaarNumberAndIdNot(
            UUID clinicId,
            String aadhaarNumber,
            UUID id
    );
}