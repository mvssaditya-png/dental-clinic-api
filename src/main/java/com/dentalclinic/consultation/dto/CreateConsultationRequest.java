package com.dentalclinic.consultation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateConsultationRequest {

    // Required only for platform SUPER_ADMIN.
    private UUID clinicId;

    @NotNull
    private UUID appointmentId;
}