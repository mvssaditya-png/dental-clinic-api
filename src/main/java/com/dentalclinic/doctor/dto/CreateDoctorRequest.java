package com.dentalclinic.doctor.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateDoctorRequest {

    // Required for platform SUPER_ADMIN.
    // Normal clinic admins will automatically use their own clinic.
    private UUID clinicId;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Invalid phone number"
    )
    private String phone;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 100)
    private String registrationNumber;

    @Size(max = 255)
    private String qualification;

    @Size(max = 255)
    private String specialization;

    @Size(max = 100)
    private String designation;

    @Min(0)
    private Integer experienceYears;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal consultationFee;

    private String bio;
}