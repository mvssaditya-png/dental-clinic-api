package com.dentalclinic.clinic.dto;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.LocalDateTime;
@Getter @Builder
public class ClinicSettingsResponse {
    private UUID id;
    private UUID clinicId;
    private String currency;
    private String dateFormat;
    private String timeFormat;
    private Integer appointmentSlotMinutes;
    private Boolean allowWalkIn;
    private Boolean enableWhatsapp;
    private Boolean enableSms;
    private Boolean enableEmail;
    private Boolean enableAiAssistant;
    private Map<String, Object> settingsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
