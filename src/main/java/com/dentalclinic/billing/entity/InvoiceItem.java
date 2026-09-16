package com.dentalclinic.billing.entity;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.treatment.entity.ProcedureMaster;
import com.dentalclinic.treatment.entity.TreatmentPlanItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_item")
@Getter
@Setter
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_plan_item_id")
    private TreatmentPlanItem treatmentPlanItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_id")
    private ProcedureMaster procedure;

    @Column(name = "tooth_number", length = 10)
    private String toothNumber;

    @Column(name = "item_code_snapshot", length = 50)
    private String itemCodeSnapshot;

    @Column(
            name = "item_name_snapshot",
            nullable = false,
            length = 255
    )
    private String itemNameSnapshot;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(
            name = "gross_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal grossAmount;

    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;

    @Column(
            name = "taxable_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal taxableAmount;

    @Column(
            name = "tax_rate",
            nullable = false,
            precision = 7,
            scale = 4
    )
    private BigDecimal taxRate;

    @Column(
            name = "tax_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal taxAmount;

    @Column(
            name = "final_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal finalAmount;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (quantity == null) {
            quantity = 1;
        }

        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO;
        }

        if (grossAmount == null) {
            grossAmount = BigDecimal.ZERO;
        }

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        if (taxableAmount == null) {
            taxableAmount = BigDecimal.ZERO;
        }

        if (taxRate == null) {
            taxRate = BigDecimal.ZERO;
        }

        if (taxAmount == null) {
            taxAmount = BigDecimal.ZERO;
        }

        if (finalAmount == null) {
            finalAmount = BigDecimal.ZERO;
        }

        if (displayOrder == null) {
            displayOrder = 0;
        }

        createdAt = LocalDateTime.now();
    }
}