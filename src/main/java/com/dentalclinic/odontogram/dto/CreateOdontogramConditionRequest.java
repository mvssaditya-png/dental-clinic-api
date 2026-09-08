package com.dentalclinic.odontogram.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOdontogramConditionRequest {

    // Required only for platform SUPER_ADMIN.
    private java.util.UUID clinicId;

    @NotBlank
    private String conditionCode;

    @NotBlank
    private String conditionName;

    private String description;

    private String patientExplanation;
}