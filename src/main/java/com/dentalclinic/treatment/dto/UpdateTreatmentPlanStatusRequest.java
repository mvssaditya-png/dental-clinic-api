package com.dentalclinic.treatment.dto;

import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateTreatmentPlanStatusRequest {

    private UUID clinicId;

    @NotNull
    private TreatmentPlanStatus status;

    private String reason;

    private String approvalNotes;

    private String declineReason;

    private String cancellationReason;
}