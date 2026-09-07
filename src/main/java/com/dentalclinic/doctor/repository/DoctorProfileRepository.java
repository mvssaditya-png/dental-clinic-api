package com.dentalclinic.doctor.repository;

import com.dentalclinic.doctor.entity.DoctorProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository
        extends JpaRepository<DoctorProfile, UUID> {

    Optional<DoctorProfile> findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );

    Optional<DoctorProfile> findByUserId(
            UUID userId
    );

    List<DoctorProfile> findAllByClinicIdAndActiveTrue(
            UUID clinicId
    );

    boolean existsByClinicIdAndRegistrationNumber(
            UUID clinicId,
            String registrationNumber
    );

    @Query("""
    SELECT dp
    FROM DoctorProfile dp
    JOIN FETCH dp.user u
    JOIN FETCH dp.clinic c
    WHERE dp.clinic.id = :clinicId
      AND dp.active = true
    ORDER BY u.firstName ASC, u.lastName ASC
""")
    List<DoctorProfile> findActiveDoctorsByClinicId(
            @Param("clinicId") UUID clinicId
    );

    @Query("""
    SELECT dp
    FROM DoctorProfile dp
    JOIN FETCH dp.user u
    JOIN FETCH dp.clinic c
    WHERE dp.id = :doctorId
      AND dp.clinic.id = :clinicId
""")
    Optional<DoctorProfile> findByIdAndClinicIdWithDetails(
            @Param("doctorId") UUID doctorId,
            @Param("clinicId") UUID clinicId
    );
}