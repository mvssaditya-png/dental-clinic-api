package com.dentalclinic.consultation.controller;

import com.dentalclinic.consultation.dto.CreateConsultationRequest;
import com.dentalclinic.consultation.dto.ConsultationResponse;
import com.dentalclinic.consultation.dto.UpdateConsultationStatusRequest;
import com.dentalclinic.consultation.service.ConsultationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    @PreAuthorize("hasAuthority('CONSULTATION_MANAGE')")
    public ResponseEntity<ConsultationResponse>
    createConsultation(
            @Valid
            @RequestBody CreateConsultationRequest request
    ) {

        ConsultationResponse response =
                consultationService
                        .createConsultation(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSULTATION_VIEW')")
    public ResponseEntity<ConsultationResponse>
    getConsultation(
            @PathVariable UUID id,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                consultationService
                        .getConsultation(
                                id,
                                clinicId
                        )
        );
    }

    @PatchMapping("/{consultationId}/status")
    @PreAuthorize("hasAuthority('CONSULTATION_MANAGE')")
    public ResponseEntity<ConsultationResponse> updateStatus(
            @PathVariable UUID consultationId,
            @Valid @RequestBody UpdateConsultationStatusRequest request
    ) {

        return ResponseEntity.ok(
                consultationService.updateStatus(
                        consultationId,
                        request
                )
        );
    }
}