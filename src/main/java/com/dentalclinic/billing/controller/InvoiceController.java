package com.dentalclinic.billing.controller;

import com.dentalclinic.billing.dto.CancelInvoiceRequest;
import com.dentalclinic.billing.dto.CreateInvoiceRequest;
import com.dentalclinic.billing.dto.InvoiceResponse;
import com.dentalclinic.billing.dto.IssueInvoiceRequest;
import com.dentalclinic.billing.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_EDIT')")
    public InvoiceResponse createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request
    ) {

        return invoiceService.createInvoice(request);
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public InvoiceResponse getInvoice(
            @PathVariable UUID invoiceId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return invoiceService.getInvoice(
                invoiceId,
                clinicId
        );
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public List<InvoiceResponse> getPatientInvoices(
            @PathVariable UUID patientId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return invoiceService.getPatientInvoices(
                patientId,
                clinicId
        );
    }

    @PatchMapping("/{invoiceId}/issue")
    @PreAuthorize("hasAuthority('APPOINTMENT_EDIT')")
    public InvoiceResponse issueInvoice(
            @PathVariable UUID invoiceId,
            @RequestBody(required = false) IssueInvoiceRequest request
    ) {

        UUID clinicId =
                request != null
                        ? request.getClinicId()
                        : null;

        return invoiceService.issueInvoice(
                invoiceId,
                clinicId
        );
    }

    @PatchMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasAuthority('APPOINTMENT_EDIT')")
    public InvoiceResponse cancelInvoice(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody CancelInvoiceRequest request
    ) {

        return invoiceService.cancelInvoice(
                invoiceId,
                request.getClinicId(),
                request.getReason()
        );
    }
}