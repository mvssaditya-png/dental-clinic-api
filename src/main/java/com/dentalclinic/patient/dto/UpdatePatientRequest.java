package com.dentalclinic.patient.dto;

import com.dentalclinic.patient.entity.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class UpdatePatientRequest {

    // Required only when platform SUPER_ADMIN performs the update
    private UUID clinicId;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @NotNull
    private Gender gender;

    @PastOrPresent
    private LocalDate dob;

    @Size(max = 10)
    private String bloodGroup;

    @Size(max = 30)
    private String maritalStatus;

    @Size(max = 150)
    private String occupation;

    @Size(max = 100)
    private String nationality;

    @NotBlank
    @Pattern(regexp = "^[6-9][0-9]{9}$")
    private String phone;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String aadhaarNumber;

    @Size(max = 150)
    private String emergencyContactName;

    @Size(max = 20)
    private String emergencyContactPhone;

    @Size(max = 255)
    private String reasonForVisit;

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

    @Valid
    private MedicalHistoryRequest medicalHistory;
}