package com.dentalclinic.casesheet.controller;

import com.dentalclinic.casesheet.dto.CaseSheetResponse;
import com.dentalclinic.casesheet.service.CaseSheetService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationCaseSheetController {

    private final CaseSheetService caseSheetService;

    @GetMapping("/{consultationId}/case-sheet")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<CaseSheetResponse>
    getCaseSheetByConsultation(
            @PathVariable UUID consultationId,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                caseSheetService
                        .getCaseSheetByConsultation(
                                consultationId,
                                clinicId
                        )
        );
    }
}