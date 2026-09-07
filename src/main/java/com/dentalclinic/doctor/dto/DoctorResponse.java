package com.dentalclinic.doctor.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DoctorResponse {

    private UUID id;

    private UUID userId;

    private UUID clinicId;

    private String firstName;

    private String lastName;

    private String phone;

    private String email;

    private String registrationNumber;

    private String qualification;

    private String specialization;

    private String designation;

    private Integer experienceYears;

    private BigDecimal consultationFee;

    private String bio;

    private Boolean active;

    private LocalDateTime createdAt;
}