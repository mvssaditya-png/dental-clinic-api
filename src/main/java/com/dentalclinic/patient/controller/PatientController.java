package com.dentalclinic.patient.controller;

import com.dentalclinic.patient.dto.CreatePatientRequest;
import com.dentalclinic.patient.dto.PatientDetailResponse;
import com.dentalclinic.patient.dto.PatientResponse;
import com.dentalclinic.patient.dto.UpdatePatientRequest;
import com.dentalclinic.patient.service.PatientService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasAuthority('PATIENT_CREATE')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request
    ) {

        PatientResponse response =
                patientService.createPatient(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT_VIEW')")
    public ResponseEntity<Page<PatientResponse>> getPatients(

            @RequestParam(required = false)
            UUID clinicId,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ResponseEntity.ok(
                patientService.getPatients(
                        clinicId,
                        search,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PATIENT_VIEW')")
    public ResponseEntity<PatientDetailResponse> getPatientById(

            @PathVariable UUID id,

            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                patientService.getPatientById(
                        id,
                        clinicId
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PATIENT_EDIT')")
    public ResponseEntity<PatientDetailResponse> updatePatient(

            @PathVariable UUID id,

            @Valid
            @RequestBody UpdatePatientRequest request
    ) {

        return ResponseEntity.ok(
                patientService.updatePatient(
                        id,
                        request
                )
        );
    }
}