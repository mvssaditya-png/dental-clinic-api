package com.dentalclinic.billing.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PaymentAllocationResponse {

    private UUID id;

    private UUID invoiceId;
    private String invoiceNumber;

    private BigDecimal allocatedAmount;

    private LocalDateTime createdAt;
}