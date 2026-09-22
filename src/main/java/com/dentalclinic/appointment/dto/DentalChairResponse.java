package com.dentalclinic.appointment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DentalChairResponse {

    private UUID id;

    private UUID clinicId;

    private String chairCode;

    private String chairName;

    private String location;

    private String description;

    private Boolean active;
}