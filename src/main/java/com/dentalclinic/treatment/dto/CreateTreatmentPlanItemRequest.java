package com.dentalclinic.treatment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateTreatmentPlanItemRequest {

    private UUID clinicId;

    @NotNull
    private UUID procedureId;

    private String toothNumber;

    @Positive
    private Integer quantity;

    @Positive
    private Integer estimatedVisits;

    @Min(0)
    private Integer sequenceNumber;

    private String description;

    private String clinicalNotes;

    private String patientExplanation;

    private String followUpInstructions;
}