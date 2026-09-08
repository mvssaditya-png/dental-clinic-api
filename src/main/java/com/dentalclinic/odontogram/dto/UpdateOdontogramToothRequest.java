package com.dentalclinic.odontogram.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateOdontogramToothRequest {

    // Required only for platform SUPER_ADMIN.
    private UUID clinicId;

    /*
     * Null conditionId means clear the condition.
     */
    private UUID conditionId;

    private String notes;

    @NotNull
    private UUID caseSheetId;

    private String changeReason;
}