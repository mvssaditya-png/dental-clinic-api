package com.dentalclinic.billing.entity;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.treatment.entity.TreatmentPlan;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "invoice",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_invoice_number",
                        columnNames = {
                                "clinic_id",
                                "invoice_number"
                        }
                )
        }
)
@Getter
@Setter
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_plan_id")
    private TreatmentPlan treatmentPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status;

    @Column(
            name = "subtotal",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal subtotal;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;

    @Column(
            name = "tax_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal taxAmount;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "paid_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal paidAmount;

    @Column(
            name = "balance_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal balanceAmount;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(
            name = "cancellation_reason",
            columnDefinition = "TEXT"
    )
    private String cancellationReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (invoiceDate == null) {
            invoiceDate = LocalDate.now();
        }

        if (status == null) {
            status = InvoiceStatus.DRAFT;
        }

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        if (taxAmount == null) {
            taxAmount = BigDecimal.ZERO;
        }

        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }

        if (balanceAmount == null) {
            balanceAmount = BigDecimal.ZERO;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}