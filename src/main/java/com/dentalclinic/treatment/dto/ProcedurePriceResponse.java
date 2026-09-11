package com.dentalclinic.treatment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProcedurePriceResponse {

    private UUID id;

    private UUID procedureId;

    private BigDecimal price;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Boolean active;

    private LocalDateTime createdAt;
}