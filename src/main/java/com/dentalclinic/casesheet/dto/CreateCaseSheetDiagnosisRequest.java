package com.dentalclinic.casesheet.dto;

import com.dentalclinic.casesheet.entity.DiagnosisType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCaseSheetDiagnosisRequest {

    @Size(max = 10)
    private String toothNumber;

    @Size(max = 50)
    private String diagnosisCode;

    @NotBlank
    @Size(max = 255)
    private String diagnosisName;

    private DiagnosisType diagnosisType;

    private String notes;
}