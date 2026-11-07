package com.dentalclinic.odontogram.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOdontogramConditionRequest {

    // Required only for platform SUPER_ADMIN.
    private java.util.UUID clinicId;

    @Size(max=100)
    private String conditionCode;

    @NotBlank
    @Size(max=150)
    private String conditionName;

    @Size(max=4000)
    private String description;

    @Size(max=4000)
    private String patientExplanation;

    @Pattern(regexp="^#[0-9a-fA-F]{6}$")
    private String colourHex;
}