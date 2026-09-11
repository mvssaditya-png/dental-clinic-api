package com.dentalclinic.treatment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateConditionProcedureMappingRequest {

    private UUID clinicId;

    @NotNull
    private UUID procedureId;

    private Boolean primary;

    @Min(0)
    private Integer displayOrder;

    private String defaultNotes;

    private String patientExplanation;
}