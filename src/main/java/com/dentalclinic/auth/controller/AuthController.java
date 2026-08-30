package com.dentalclinic.auth.controller;

import com.dentalclinic.auth.dto.SendOtpRequest;
import com.dentalclinic.auth.dto.SendOtpResponse;
import com.dentalclinic.auth.dto.VerifyOtpRequest;
import com.dentalclinic.auth.dto.VerifyOtpResponse;
import com.dentalclinic.auth.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}