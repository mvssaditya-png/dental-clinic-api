package com.dentalclinic.billing.dto;

import com.dentalclinic.billing.entity.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {

    private UUID id;

    private UUID clinicId;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private UUID treatmentPlanId;
    private String treatmentPlanNumber;

    private UUID appointmentId;
    private String appointmentNumber;

    private String invoiceNumber;

    private LocalDate invoiceDate;
    private LocalDate dueDate;

    private InvoiceStatus status;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;

    private String notes;

    private LocalDateTime issuedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    private List<InvoiceItemResponse> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}