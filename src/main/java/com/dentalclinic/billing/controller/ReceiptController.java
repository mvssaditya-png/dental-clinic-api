package com.dentalclinic.billing.controller;

import com.dentalclinic.billing.dto.CreateReceiptRequest;
import com.dentalclinic.billing.dto.ReceiptResponse;
import com.dentalclinic.billing.service.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping
    @PreAuthorize("hasAuthority('RECEIPT_GENERATE')")
    public ReceiptResponse createReceipt(
            @Valid
            @RequestBody CreateReceiptRequest request
    ) {

        return receiptService.createReceipt(request);
    }

    @GetMapping("/{receiptId}")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public ReceiptResponse getReceipt(
            @PathVariable UUID receiptId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return receiptService.getReceipt(
                receiptId,
                clinicId
        );
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public List<ReceiptResponse> getPatientReceipts(
            @PathVariable UUID patientId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return receiptService.getPatientReceipts(
                patientId,
                clinicId
        );
    }

    @GetMapping("/payment/{paymentId}")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public ReceiptResponse getReceiptByPayment(
            @PathVariable UUID paymentId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return receiptService.getReceiptByPayment(
                paymentId,
                clinicId
        );
    }
}