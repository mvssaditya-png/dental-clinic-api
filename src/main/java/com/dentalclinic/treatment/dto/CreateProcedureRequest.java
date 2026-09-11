package com.dentalclinic.treatment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateProcedureRequest {

    private UUID clinicId;

    @NotBlank
    private String procedureCode;

    @NotBlank
    private String procedureName;

    private String description;

    private String patientExplanation;

    private UUID departmentId;

    @Positive
    private Integer defaultDurationMinutes;

    @Positive
    private Integer defaultEstimatedVisits;

    private String defaultNotes;

    private String followUpInstructions;
}