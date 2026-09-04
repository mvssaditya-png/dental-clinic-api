package com.dentalclinic.patient.dto;

import com.dentalclinic.patient.entity.Gender;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PatientResponse {

    private UUID id;
    private String patientNumber;

    private String firstName;
    private String lastName;

    private Gender gender;
    private LocalDate dob;

    private String bloodGroup;
    private String maritalStatus;
    private String occupation;
    private String nationality;

    private String phone;
    private String email;
    private String aadhaarNumber;

    private String emergencyContactName;
    private String emergencyContactPhone;

    private String reasonForVisit;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pincode;

    private Boolean active;

    private UUID clinicId;

    private LocalDateTime createdAt;
}