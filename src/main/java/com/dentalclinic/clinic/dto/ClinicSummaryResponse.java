package com.dentalclinic.clinic.dto;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.LocalDateTime;
@Getter @Builder
public class ClinicSummaryResponse {
    private UUID id;
    private String clinicCode;
    private String clinicName;
    private String city;
    private String state;
    private Boolean active;
}
