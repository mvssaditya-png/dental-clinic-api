package com.dentalclinic.consultation.repository;

import com.dentalclinic.consultation.entity.Consultation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository
        extends JpaRepository<Consultation, UUID> {

    boolean existsByAppointmentId(UUID appointmentId);

    @Query("""
        SELECT c
        FROM Consultation c
        JOIN FETCH c.clinic cl
        JOIN FETCH c.appointment a
        JOIN FETCH c.patient p
        JOIN FETCH c.doctor d
        JOIN FETCH d.user du
        WHERE c.id = :consultationId
          AND cl.id = :clinicId
    """)
    Optional<Consultation> findByIdAndClinicIdWithDetails(
            @Param("consultationId") UUID consultationId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
        SELECT c
        FROM Consultation c
        JOIN FETCH c.clinic cl
        JOIN FETCH c.appointment a
        JOIN FETCH c.patient p
        JOIN FETCH c.doctor d
        JOIN FETCH d.user du
        WHERE a.id = :appointmentId
          AND cl.id = :clinicId
    """)
    Optional<Consultation> findByAppointmentIdAndClinicIdWithDetails(
            @Param("appointmentId") UUID appointmentId,
            @Param("clinicId") UUID clinicId
    );
}