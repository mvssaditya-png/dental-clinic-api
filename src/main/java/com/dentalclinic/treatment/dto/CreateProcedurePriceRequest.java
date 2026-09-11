package com.dentalclinic.treatment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateProcedurePriceRequest {

    private UUID clinicId;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal price;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}