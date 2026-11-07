package com.dentalclinic.odontogram.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record ManagedOdontogramConditionResponse(UUID id,UUID clinicId,String conditionCode,String conditionName,
 String description,String patientExplanation,boolean active,String colourHex,long version,LocalDateTime createdAt,LocalDateTime updatedAt) {}
