package com.dentalclinic.billing.dto;

import com.dentalclinic.billing.entity.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreatePaymentRequest {

    private UUID clinicId;

    @NotNull
    private UUID invoiceId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private PaymentMode paymentMode;

    private String transactionReference;

    private String notes;
}