package com.dentalclinic.casesheet.dto;

import com.dentalclinic.casesheet.entity.DiagnosisType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CaseSheetDiagnosisResponse {

    private UUID id;
    private UUID caseSheetId;

    private String toothNumber;
    private String diagnosisCode;
    private String diagnosisName;
    private DiagnosisType diagnosisType;
    private String notes;

    private LocalDateTime createdAt;
}