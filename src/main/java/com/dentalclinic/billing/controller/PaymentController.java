package com.dentalclinic.billing.controller;

import com.dentalclinic.billing.dto.CreatePaymentRequest;
import com.dentalclinic.billing.dto.PaymentResponse;
import com.dentalclinic.billing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_EDIT')")
    public PaymentResponse createPayment(
            @Valid
            @RequestBody CreatePaymentRequest request
    ) {

        return paymentService.createPayment(request);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public PaymentResponse getPayment(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return paymentService.getPayment(
                paymentId,
                clinicId
        );
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public List<PaymentResponse> getPatientPayments(
            @PathVariable UUID patientId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return paymentService.getPatientPayments(
                patientId,
                clinicId
        );
    }
}