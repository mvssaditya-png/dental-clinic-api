package com.dentalclinic.auth.controller;

import com.dentalclinic.auth.dto.*;
import com.dentalclinic.auth.service.OtpService;
import com.dentalclinic.user.entity.AppUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {

        return ResponseEntity.ok(
                otpService.sendLoginOtp(request)
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {

        return ResponseEntity.ok(
                otpService.verifyLoginOtp(request)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(
            Authentication authentication
    ) {

        AppUser user = (AppUser) authentication.getPrincipal();

        List<String> authorities =
                authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .sorted()
                        .toList();

        AuthMeResponse response =
                AuthMeResponse.builder()
                        .userId(user.getId())

                        .clinicId(
                                user.getClinic() != null
                                        ? user.getClinic().getId()
                                        : null
                        )
                        .clinicCode(
                                user.getClinic() != null
                                        ? user.getClinic().getClinicCode()
                                        : null
                        )
                        .clinicName(
                                user.getClinic() != null
                                        ? user.getClinic().getClinicName()
                                        : null
                        )

                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phone(user.getPhone())
                        .email(user.getEmail())

                        .authorities(authorities)

                        .build();

        return ResponseEntity.ok(response);
    }
}