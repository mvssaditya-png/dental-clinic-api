package com.dentalclinic.appointment.dto;

import com.dentalclinic.appointment.entity.AppointmentPriority;
import com.dentalclinic.appointment.entity.AppointmentStatus;
import com.dentalclinic.appointment.entity.AppointmentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
public class AppointmentResponse {

    private UUID id;
    private String appointmentNumber;

    private UUID clinicId;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private UUID doctorId;
    private String doctorName;
    private String doctorSpecialization;

    private UUID departmentId;
    private UUID chairId;

    private AppointmentType appointmentType;

    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private Integer estimatedDurationMinutes;

    private AppointmentPriority priority;

    private String reason;
    private String remarks;

    private AppointmentStatus status;

    private LocalDateTime createdAt;
}