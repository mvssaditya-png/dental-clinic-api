package com.dentalclinic.billing.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "receipt",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_receipt_number",
                        columnNames = {
                                "clinic_id",
                                "receipt_number"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_receipt_payment",
                        columnNames = "payment_id"
                )
        }
)
@Getter
@Setter
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(
            name = "receipt_number",
            nullable = false,
            length = 50
    )
    private String receiptNumber;

    @Column(
            name = "amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private AppUser issuedBy;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (issuedAt == null) {
            issuedAt = now;
        }

        createdAt = now;
    }
}