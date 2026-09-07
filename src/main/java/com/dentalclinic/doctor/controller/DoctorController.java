package com.dentalclinic.doctor.controller;

import com.dentalclinic.doctor.dto.CreateDoctorRequest;
import com.dentalclinic.doctor.dto.DoctorResponse;
import com.dentalclinic.doctor.service.DoctorService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<DoctorResponse> createDoctor(
            @Valid
            @RequestBody CreateDoctorRequest request
    ) {

        DoctorResponse response =
                doctorService.createDoctor(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<List<DoctorResponse>> getDoctors(
            @RequestParam(required = false) UUID clinicId
    ) {

        return ResponseEntity.ok(
                doctorService.getDoctors(clinicId)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<DoctorResponse> getDoctorById(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID clinicId
    ) {

        return ResponseEntity.ok(
                doctorService.getDoctorById(
                        id,
                        clinicId
                )
        );
    }
}