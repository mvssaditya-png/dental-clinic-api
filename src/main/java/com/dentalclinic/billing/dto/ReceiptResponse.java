package com.dentalclinic.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReceiptResponse {

    private UUID id;

    private UUID clinicId;

    private UUID paymentId;
    private String paymentNumber;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private String receiptNumber;

    private BigDecimal amount;

    private LocalDateTime issuedAt;
    private UUID issuedById;

    private String pdfUrl;
    private String notes;

    private LocalDateTime createdAt;
}