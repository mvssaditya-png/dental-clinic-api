package com.dentalclinic.clinic.repository;

import com.dentalclinic.clinic.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {

    Optional<Clinic> findByClinicCode(String clinicCode);

    Optional<Clinic> findByEmailIgnoreCase(String email);

    boolean existsByClinicCode(String clinicCode);
    @Query("""
        SELECT c FROM Clinic c
        WHERE (:active IS NULL OR c.active = :active)
          AND (:search = '' OR LOWER(c.clinicCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.clinicName) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY c.clinicName ASC, c.id ASC
        """)
    Page<Clinic> searchClinics(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
}
