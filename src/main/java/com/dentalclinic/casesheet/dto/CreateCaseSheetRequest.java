package com.dentalclinic.casesheet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateCaseSheetRequest {

    // Required for platform SUPER_ADMIN.
    private UUID clinicId;

    @NotNull
    private UUID consultationId;
}