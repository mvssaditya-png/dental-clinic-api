package com.dentalclinic.odontogram.dto;

import com.dentalclinic.odontogram.entity.OdontogramStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OdontogramResponse {

    private UUID id;

    private UUID clinicId;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private OdontogramStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}