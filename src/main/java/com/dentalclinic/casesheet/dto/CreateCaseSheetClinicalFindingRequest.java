package com.dentalclinic.casesheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCaseSheetClinicalFindingRequest {

    @Size(max = 10)
    private String toothNumber;

    @NotBlank
    @Size(max = 100)
    private String findingType;

    @Size(max = 255)
    private String findingValue;

    private String notes;
}