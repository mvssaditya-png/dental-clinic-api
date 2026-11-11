package com.dentalclinic.odontogram.repository;

import com.dentalclinic.odontogram.entity.Odontogram;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OdontogramRepository
        extends JpaRepository<Odontogram, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Odontogram e where e.id=:id and e.clinic.id=:clinicId")
    Optional<Odontogram> lockByIdAndClinicId(@org.springframework.data.repository.query.Param("id") UUID id, @org.springframework.data.repository.query.Param("clinicId") UUID clinicId);


    Optional<Odontogram>
    findByPatientIdAndClinicId(
            UUID patientId,
            UUID clinicId
    );

    boolean existsByPatientId(UUID patientId);
    boolean existsByPatientIdAndStatus(UUID patientId, com.dentalclinic.odontogram.entity.OdontogramStatus status);
    Optional<Odontogram> findByPatientIdAndClinicIdAndStatus(UUID patientId,UUID clinicId,com.dentalclinic.odontogram.entity.OdontogramStatus status);
    org.springframework.data.domain.Page<Odontogram> findByPatientIdAndClinicIdAndStatus(UUID patientId,UUID clinicId,com.dentalclinic.odontogram.entity.OdontogramStatus status,org.springframework.data.domain.Pageable page);

    Optional<Odontogram> findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );
}