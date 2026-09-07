package com.dentalclinic.appointment.dto;

import com.dentalclinic.appointment.entity.AppointmentPriority;
import com.dentalclinic.appointment.entity.AppointmentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class CreateAppointmentRequest {

    // Required for platform SUPER_ADMIN.
    // Normal clinic users automatically use their clinic.
    private UUID clinicId;

    @NotNull
    private UUID patientId;

    @NotNull
    private UUID doctorId;

    private UUID departmentId;

    private UUID chairId;

    private AppointmentType appointmentType;

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @Positive
    private Integer estimatedDurationMinutes;

    private AppointmentPriority priority;

    private String reason;

    private String remarks;
}