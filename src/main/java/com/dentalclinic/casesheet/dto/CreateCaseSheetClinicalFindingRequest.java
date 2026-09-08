package com.dentalclinic.casesheet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCaseSheetClinicalFindingRequest {

    private String toothNumber;

    @NotBlank
    private String findingType;

    private String findingValue;

    private String notes;
}