package com.dentalclinic.clinic.dto;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.LocalDateTime;
@Getter @Setter
public class UpdateClinicSettingsRequest {
    @NotBlank @Size(max = 10)
    private String currency;
    @NotBlank @Size(max = 30)
    private String dateFormat;
    @NotNull @Pattern(regexp = "12_HOUR|24_HOUR")
    private String timeFormat;
    @NotNull @Positive
    private Integer appointmentSlotMinutes;
    @NotNull
    private Boolean allowWalkIn;
    @NotNull
    private Boolean enableWhatsapp;
    @NotNull
    private Boolean enableSms;
    @NotNull
    private Boolean enableEmail;
    @NotNull
    private Boolean enableAiAssistant;
    private Map<String, Object> settingsJson;
}
