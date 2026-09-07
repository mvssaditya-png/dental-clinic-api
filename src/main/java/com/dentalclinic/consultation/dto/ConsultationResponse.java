package com.dentalclinic.consultation.dto;

import com.dentalclinic.consultation.entity.ConsultationStatus;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConsultationResponse {

    private UUID id;

    private UUID clinicId;

    private UUID appointmentId;
    private String appointmentNumber;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private UUID doctorId;
    private String doctorName;

    private ConsultationStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private LocalDateTime createdAt;
}