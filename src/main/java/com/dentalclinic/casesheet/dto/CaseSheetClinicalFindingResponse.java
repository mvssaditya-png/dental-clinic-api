package com.dentalclinic.casesheet.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CaseSheetClinicalFindingResponse {

    private UUID id;
    private UUID caseSheetId;

    private String toothNumber;
    private String findingType;
    private String findingValue;
    private String notes;

    private LocalDateTime createdAt;
}