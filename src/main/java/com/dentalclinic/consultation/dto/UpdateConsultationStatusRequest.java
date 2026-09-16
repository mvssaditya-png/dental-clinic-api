package com.dentalclinic.consultation.dto;

import com.dentalclinic.consultation.entity.ConsultationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateConsultationStatusRequest {

    private UUID clinicId;

    @NotNull
    private ConsultationStatus status;
}