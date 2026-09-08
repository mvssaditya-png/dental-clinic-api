package com.dentalclinic.casesheet.controller;

import com.dentalclinic.casesheet.dto.*;
import com.dentalclinic.casesheet.service.CaseSheetService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/case-sheets")
@RequiredArgsConstructor
public class CaseSheetController {

    private final CaseSheetService caseSheetService;

    @PostMapping
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<CaseSheetResponse>
    createCaseSheet(
            @Valid
            @RequestBody CreateCaseSheetRequest request
    ) {

        CaseSheetResponse response =
                caseSheetService
                        .createCaseSheet(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<CaseSheetResponse>
    updateCaseSheet(
            @PathVariable UUID id,
            @RequestParam(required = false)
            UUID clinicId,
            @RequestBody UpdateCaseSheetRequest request
    ) {

        CaseSheetResponse response =
                caseSheetService
                        .updateCaseSheet(
                                id,
                                clinicId,
                                request
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<CaseSheetResponse>
    getCaseSheet(
            @PathVariable UUID id,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                caseSheetService
                        .getCaseSheet(
                                id,
                                clinicId
                        )
        );
    }

    @PostMapping("/{id}/diagnoses")
    @PreAuthorize("hasAuthority('APPOINTMENT_EDIT')")
    public ResponseEntity<CaseSheetDiagnosisResponse>
    addDiagnosis(
            @PathVariable UUID id,
            @RequestParam(required = false)
            UUID clinicId,
            @Valid
            @RequestBody CreateCaseSheetDiagnosisRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        caseSheetService.addDiagnosis(
                                id,
                                clinicId,
                                request
                        )
                );
    }

    @GetMapping("/{id}/diagnoses")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public ResponseEntity<List<CaseSheetDiagnosisResponse>>
    getDiagnoses(
            @PathVariable UUID id,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                caseSheetService.getDiagnoses(
                        id,
                        clinicId
                )
        );
    }

    @PostMapping("/{id}/findings")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<CaseSheetClinicalFindingResponse>
    addClinicalFinding(
            @PathVariable UUID id,
            @RequestParam(required = false)
            UUID clinicId,
            @Valid
            @RequestBody
            CreateCaseSheetClinicalFindingRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        caseSheetService
                                .addClinicalFinding(
                                        id,
                                        clinicId,
                                        request
                                )
                );
    }

    @GetMapping("/{id}/findings")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<List<CaseSheetClinicalFindingResponse>>
    getClinicalFindings(
            @PathVariable UUID id,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                caseSheetService
                        .getClinicalFindings(
                                id,
                                clinicId
                        )
        );
    }
}