package com.dentalclinic.treatment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProcedureResponse {

    private UUID id;
    private UUID clinicId;

    private String procedureCode;
    private String procedureName;

    private String description;
    private String patientExplanation;

    private UUID departmentId;

    private Integer defaultDurationMinutes;
    private Integer defaultEstimatedVisits;

    private String defaultNotes;
    private String followUpInstructions;

    private Boolean active;

    private BigDecimal currentPrice;
    private LocalDate priceEffectiveFrom;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}