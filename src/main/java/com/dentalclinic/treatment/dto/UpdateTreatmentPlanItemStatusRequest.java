package com.dentalclinic.treatment.dto;

import com.dentalclinic.treatment.entity.TreatmentPlanItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateTreatmentPlanItemStatusRequest {

    private UUID clinicId;

    @NotNull
    private TreatmentPlanItemStatus status;

    private String cancellationReason;
}