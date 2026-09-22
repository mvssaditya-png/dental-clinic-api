package com.dentalclinic.appointment.repository;

import com.dentalclinic.appointment.entity.DentalChair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DentalChairRepository
        extends JpaRepository<DentalChair, UUID> {

    List<DentalChair>
    findAllByClinicIdAndActiveTrueOrderByChairNameAsc(
            UUID clinicId
    );

    Optional<DentalChair> findByIdAndClinicId(
            UUID chairId,
            UUID clinicId
    );

    Optional<DentalChair> findByClinicIdAndChairCode(
            UUID clinicId,
            String chairCode
    );

    boolean existsByClinicIdAndChairCode(
            UUID clinicId,
            String chairCode
    );

    boolean existsByClinicIdAndChairNameIgnoreCase(
            UUID clinicId,
            String chairName
    );
}