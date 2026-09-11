package com.dentalclinic.treatment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConditionProcedureMappingResponse {

    private UUID id;

    private UUID conditionId;
    private String conditionCode;
    private String conditionName;

    private UUID procedureId;
    private String procedureCode;
    private String procedureName;

    private Boolean primary;
    private Integer displayOrder;

    private String defaultNotes;
    private String patientExplanation;

    private BigDecimal currentPrice;

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}