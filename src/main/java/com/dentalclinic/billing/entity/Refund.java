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
        name = "refund",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_refund_number",
                        columnNames = {
                                "clinic_id",
                                "refund_number"
                        }
                )
        }
)
@Getter
@Setter
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(
            name = "refund_number",
            nullable = false,
            length = 50
    )
    private String refundNumber;

    @Column(
            name = "amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_mode", length = 30)
    private PaymentMode refundMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status;

    @Column(
            name = "reason",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String reason;

    @Column(
            name = "transaction_reference",
            length = 255
    )
    private String transactionReference;

    @Column(name = "refunded_at", nullable = false)
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refunded_by", nullable = false)
    private AppUser refundedBy;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = RefundStatus.COMPLETED;
        }

        if (refundedAt == null) {
            refundedAt = now;
        }

        createdAt = now;
    }
}