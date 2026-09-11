package com.dentalclinic.treatment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateTreatmentPlanRequest {

    private UUID clinicId;

    @NotNull
    private UUID caseSheetId;

    private String title;

    private Integer estimatedTotalVisits;

    private String notes;

    private String patientNotes;
}