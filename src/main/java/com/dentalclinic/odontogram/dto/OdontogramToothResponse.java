package com.dentalclinic.odontogram.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OdontogramToothResponse {

    private UUID id;

    private UUID odontogramId;

    private String toothNumber;

    private UUID conditionId;
    private String conditionCode;
    private String conditionName;

    private String notes;

    private UUID lastCaseSheetId;
    private UUID lastAppointmentId;

    private LocalDateTime lastChangedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}