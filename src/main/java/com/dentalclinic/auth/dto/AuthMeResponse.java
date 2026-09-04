package com.dentalclinic.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AuthMeResponse {

    private UUID userId;

    private UUID clinicId;
    private String clinicCode;
    private String clinicName;

    private String firstName;
    private String lastName;

    private String phone;
    private String email;

    private List<String> authorities;
}