package com.dentalclinic.billing.repository;

import com.dentalclinic.billing.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RefundRepository
        extends JpaRepository<Refund, UUID> {

    boolean existsByClinicIdAndRefundNumber(
            UUID clinicId,
            String refundNumber
    );

    long countByClinicId(UUID clinicId);

    @Query("""
            SELECT r
            FROM Refund r
            JOIN FETCH r.payment
            JOIN FETCH r.patient
            JOIN FETCH r.refundedBy
            WHERE r.payment.id = :paymentId
              AND r.clinic.id = :clinicId
            ORDER BY r.refundedAt DESC
            """)
    List<Refund> findAllByPaymentIdWithDetails(
            @Param("paymentId") UUID paymentId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM Refund r
            WHERE r.payment.id = :paymentId
              AND r.clinic.id = :clinicId
              AND r.status = com.dentalclinic.billing.entity.RefundStatus.COMPLETED
            """)
    BigDecimal sumCompletedRefundsForPayment(
            @Param("paymentId") UUID paymentId,
            @Param("clinicId") UUID clinicId
    );
}