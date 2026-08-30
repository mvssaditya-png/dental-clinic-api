package com.dentalclinic.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SendOtpResponse {

    private boolean success;
    private String message;
    private int expiresInSeconds;
}