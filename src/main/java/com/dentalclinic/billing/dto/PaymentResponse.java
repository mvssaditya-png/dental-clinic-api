package com.dentalclinic.billing.dto;

import com.dentalclinic.billing.entity.PaymentMode;
import com.dentalclinic.billing.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PaymentResponse {

    private UUID id;

    private UUID clinicId;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private String paymentNumber;

    private LocalDateTime paymentDate;

    private BigDecimal amount;

    private PaymentMode paymentMode;
    private PaymentStatus status;

    private String transactionReference;
    private String notes;

    private UUID receivedById;

    private List<PaymentAllocationResponse> allocations;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}