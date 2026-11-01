package com.dentalclinic.treatment.dto;

import com.dentalclinic.casesheet.validation.ValidRemovalReason;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RemoveTreatmentPlanRequest {
    @ValidRemovalReason
    private String reason;
}
