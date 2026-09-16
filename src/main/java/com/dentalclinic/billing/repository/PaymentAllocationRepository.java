package com.dentalclinic.billing.repository;

import com.dentalclinic.billing.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentAllocationRepository
        extends JpaRepository<PaymentAllocation, UUID> {

    boolean existsByPaymentIdAndInvoiceId(
            UUID paymentId,
            UUID invoiceId
    );

    @Query("""
            SELECT pa
            FROM PaymentAllocation pa
            JOIN FETCH pa.payment
            JOIN FETCH pa.invoice
            WHERE pa.payment.id = :paymentId
              AND pa.clinic.id = :clinicId
            ORDER BY pa.createdAt ASC
            """)
    List<PaymentAllocation> findAllByPaymentIdWithDetails(
            @Param("paymentId") UUID paymentId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT pa
            FROM PaymentAllocation pa
            JOIN FETCH pa.payment
            JOIN FETCH pa.invoice
            WHERE pa.invoice.id = :invoiceId
              AND pa.clinic.id = :clinicId
            ORDER BY pa.createdAt ASC
            """)
    List<PaymentAllocation> findAllByInvoiceIdWithDetails(
            @Param("invoiceId") UUID invoiceId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT COALESCE(SUM(pa.allocatedAmount), 0)
            FROM PaymentAllocation pa
            WHERE pa.invoice.id = :invoiceId
              AND pa.clinic.id = :clinicId
              AND pa.payment.status = com.dentalclinic.billing.entity.PaymentStatus.COMPLETED
            """)
    BigDecimal sumCompletedAllocationsForInvoice(
            @Param("invoiceId") UUID invoiceId,
            @Param("clinicId") UUID clinicId
    );
}