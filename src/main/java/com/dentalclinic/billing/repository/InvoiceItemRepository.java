package com.dentalclinic.billing.repository;

import com.dentalclinic.billing.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceItemRepository
        extends JpaRepository<InvoiceItem, UUID> {

    @Query("""
            SELECT ii
            FROM InvoiceItem ii
            LEFT JOIN FETCH ii.treatmentPlanItem
            LEFT JOIN FETCH ii.procedure
            WHERE ii.invoice.id = :invoiceId
              AND ii.clinic.id = :clinicId
            ORDER BY ii.displayOrder ASC, ii.createdAt ASC
            """)
    List<InvoiceItem> findAllByInvoiceIdWithDetails(
            @Param("invoiceId") UUID invoiceId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT ii
            FROM InvoiceItem ii
            JOIN FETCH ii.invoice
            LEFT JOIN FETCH ii.treatmentPlanItem
            LEFT JOIN FETCH ii.procedure
            WHERE ii.id = :itemId
              AND ii.clinic.id = :clinicId
            """)
    Optional<InvoiceItem> findByIdAndClinicIdWithDetails(
            @Param("itemId") UUID itemId,
            @Param("clinicId") UUID clinicId
    );

    long countByInvoiceId(UUID invoiceId);
}