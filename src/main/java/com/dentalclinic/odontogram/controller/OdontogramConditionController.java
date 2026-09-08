package com.dentalclinic.odontogram.controller;

import com.dentalclinic.odontogram.dto.CreateOdontogramConditionRequest;
import com.dentalclinic.odontogram.dto.OdontogramConditionResponse;
import com.dentalclinic.odontogram.service.OdontogramService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/odontogram-conditions")
@RequiredArgsConstructor
public class OdontogramConditionController {

    private final OdontogramService odontogramService;

    @PostMapping
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<OdontogramConditionResponse>
    createCondition(
            @Valid
            @RequestBody
            CreateOdontogramConditionRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        odontogramService
                                .createCondition(request)
                );
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<List<OdontogramConditionResponse>>
    getConditions(
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                odontogramService
                        .getConditions(clinicId)
        );
    }
}