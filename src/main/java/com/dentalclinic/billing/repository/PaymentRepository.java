package com.dentalclinic.billing.repository;

import com.dentalclinic.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    boolean existsByClinicIdAndPaymentNumber(
            UUID clinicId,
            String paymentNumber
    );

    long countByClinicId(UUID clinicId);

    @Query("""
            SELECT p
            FROM Payment p
            JOIN FETCH p.patient
            JOIN FETCH p.receivedBy
            WHERE p.id = :paymentId
              AND p.clinic.id = :clinicId
            """)
    Optional<Payment> findByIdAndClinicIdWithDetails(
            @Param("paymentId") UUID paymentId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT p
            FROM Payment p
            JOIN FETCH p.patient
            JOIN FETCH p.receivedBy
            WHERE p.patient.id = :patientId
              AND p.clinic.id = :clinicId
            ORDER BY p.paymentDate DESC
            """)
    List<Payment> findAllByPatientAndClinicWithDetails(
            @Param("patientId") UUID patientId,
            @Param("clinicId") UUID clinicId
    );
}