package com.dentalclinic.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateInvoiceRequest {

    private UUID clinicId;

    @NotNull
    private UUID treatmentPlanId;

    private LocalDate dueDate;

    private String notes;
}