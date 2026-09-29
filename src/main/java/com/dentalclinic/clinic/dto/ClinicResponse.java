package com.dentalclinic.clinic.dto;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.LocalDateTime;
@Getter @Builder
public class ClinicResponse {
    private UUID id;
    private String clinicCode;
    private String clinicName;
    private String phone;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String timezone;
    private String logoUrl;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
