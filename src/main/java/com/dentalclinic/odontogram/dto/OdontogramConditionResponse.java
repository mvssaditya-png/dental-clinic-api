package com.dentalclinic.odontogram.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OdontogramConditionResponse {

    private UUID id;
    private UUID clinicId;

    private String conditionCode;
    private String conditionName;

    private String description;
    private String patientExplanation;

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}