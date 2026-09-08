package com.dentalclinic.casesheet.dto;

import com.dentalclinic.casesheet.entity.DiagnosisType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCaseSheetDiagnosisRequest {

    private String toothNumber;

    private String diagnosisCode;

    @NotBlank
    private String diagnosisName;

    private DiagnosisType diagnosisType;

    private String notes;
}