package com.dentalclinic.billing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CancelInvoiceRequest {

    private UUID clinicId;

    @NotBlank
    private String reason;
}