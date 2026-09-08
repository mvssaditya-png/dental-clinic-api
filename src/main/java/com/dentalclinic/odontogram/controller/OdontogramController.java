package com.dentalclinic.odontogram.controller;

import com.dentalclinic.odontogram.dto.OdontogramResponse;
import com.dentalclinic.odontogram.service.OdontogramService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class OdontogramController {

    private final OdontogramService odontogramService;

    @PostMapping("/{patientId}/odontogram")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<OdontogramResponse>
    createOdontogram(
            @PathVariable UUID patientId,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        odontogramService
                                .createOdontogram(
                                        patientId,
                                        clinicId
                                )
                );
    }

    @GetMapping("/{patientId}/odontogram")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<OdontogramResponse>
    getOdontogram(
            @PathVariable UUID patientId,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                odontogramService
                        .getOdontogramByPatient(
                                patientId,
                                clinicId
                        )
        );
    }
}