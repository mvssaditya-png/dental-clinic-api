package com.dentalclinic.billing.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_allocation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payment_invoice_allocation",
                        columnNames = {
                                "payment_id",
                                "invoice_id"
                        }
                )
        }
)
@Getter
@Setter
public class PaymentAllocation {

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
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(
            name = "allocated_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal allocatedAmount;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}