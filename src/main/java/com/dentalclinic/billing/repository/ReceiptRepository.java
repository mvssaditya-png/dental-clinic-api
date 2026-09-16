package com.dentalclinic.billing.repository;

import com.dentalclinic.billing.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository
        extends JpaRepository<Receipt, UUID> {

    boolean existsByClinicIdAndReceiptNumber(
            UUID clinicId,
            String receiptNumber
    );

    boolean existsByPaymentId(UUID paymentId);

    long countByClinicId(UUID clinicId);

    @Query("""
            SELECT r
            FROM Receipt r
            JOIN FETCH r.payment
            JOIN FETCH r.patient
            JOIN FETCH r.issuedBy
            WHERE r.payment.id = :paymentId
              AND r.clinic.id = :clinicId
            """)
    Optional<Receipt> findByPaymentIdAndClinicIdWithDetails(
            @Param("paymentId") UUID paymentId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT r
            FROM Receipt r
            JOIN FETCH r.payment
            JOIN FETCH r.patient
            JOIN FETCH r.issuedBy
            WHERE r.patient.id = :patientId
              AND r.clinic.id = :clinicId
            ORDER BY r.issuedAt DESC
            """)
    List<Receipt> findAllByPatientAndClinicWithDetails(
            @Param("patientId") UUID patientId,
            @Param("clinicId") UUID clinicId
    );
}