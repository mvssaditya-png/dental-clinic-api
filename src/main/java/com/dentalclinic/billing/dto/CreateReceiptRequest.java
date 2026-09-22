package com.dentalclinic.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateReceiptRequest {

    private UUID clinicId;

    @NotNull
    private UUID paymentId;

    private String notes;
}