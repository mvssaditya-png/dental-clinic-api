package com.dentalclinic.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InvoiceItemResponse {

    private UUID id;

    private UUID invoiceId;

    private UUID treatmentPlanItemId;

    private UUID procedureId;

    private String toothNumber;

    private String itemCode;

    private String itemName;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal grossAmount;

    private BigDecimal discountAmount;

    private BigDecimal taxableAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal finalAmount;

    private Integer displayOrder;

    private LocalDateTime createdAt;
}