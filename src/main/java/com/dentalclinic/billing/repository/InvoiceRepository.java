package com.dentalclinic.billing.repository;

import com.dentalclinic.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByClinicIdAndInvoiceNumber(
            UUID clinicId,
            String invoiceNumber
    );

    long countByClinicId(UUID clinicId);

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.clinic
            JOIN FETCH i.patient
            LEFT JOIN FETCH i.treatmentPlan
            LEFT JOIN FETCH i.appointment
            WHERE i.id = :invoiceId
              AND i.clinic.id = :clinicId
            """)
    Optional<Invoice> findByIdAndClinicIdWithDetails(
            @Param("invoiceId") UUID invoiceId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.patient
            LEFT JOIN FETCH i.treatmentPlan
            LEFT JOIN FETCH i.appointment
            WHERE i.patient.id = :patientId
              AND i.clinic.id = :clinicId
            ORDER BY i.invoiceDate DESC, i.createdAt DESC
            """)
    List<Invoice> findAllByPatientAndClinicWithDetails(
            @Param("patientId") UUID patientId,
            @Param("clinicId") UUID clinicId
    );

    @Query("""
            SELECT i
            FROM Invoice i
            JOIN FETCH i.patient
            LEFT JOIN FETCH i.treatmentPlan
            LEFT JOIN FETCH i.appointment
            WHERE i.treatmentPlan.id = :treatmentPlanId
              AND i.clinic.id = :clinicId
            ORDER BY i.createdAt DESC
            """)
    List<Invoice> findAllByTreatmentPlanAndClinicWithDetails(
            @Param("treatmentPlanId") UUID treatmentPlanId,
            @Param("clinicId") UUID clinicId
    );

    boolean existsByClinicIdAndTreatmentPlanId(
            UUID clinicId,
            UUID treatmentPlanId
    );
}