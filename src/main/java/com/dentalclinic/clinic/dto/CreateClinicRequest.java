package com.dentalclinic.clinic.dto;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.LocalDateTime;
@Getter @Setter
public class CreateClinicRequest {
    @NotBlank @Size(max = 50)
    private String clinicCode;
    @NotBlank @Size(max = 150)
    private String clinicName;
    @Size(max = 20)
    private String phone;
    @Email @Size(max = 150)
    private String email;
    @Size(max = 255)
    private String addressLine1;
    @Size(max = 255)
    private String addressLine2;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String state;
    @Size(max = 100)
    private String country;
    @Size(max = 20)
    private String pincode;
    @Size(max = 100)
    private String timezone;
    private String logoUrl;
}
