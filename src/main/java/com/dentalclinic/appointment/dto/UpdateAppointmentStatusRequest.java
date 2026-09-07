package com.dentalclinic.appointment.dto;

import com.dentalclinic.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateAppointmentStatusRequest {

    // Required for platform SUPER_ADMIN.
    private UUID clinicId;

    @NotNull
    private AppointmentStatus status;

    private String reason;
}