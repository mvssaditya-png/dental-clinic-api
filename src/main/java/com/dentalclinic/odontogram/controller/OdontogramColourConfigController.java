package com.dentalclinic.odontogram.controller;

import com.dentalclinic.odontogram.dto.OdontogramColourConfigResponse;
import com.dentalclinic.odontogram.dto.SaveOdontogramColourConfigRequest;
import com.dentalclinic.odontogram.service.OdontogramColourConfigService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/odontogram-colour-configs")
@RequiredArgsConstructor
public class OdontogramColourConfigController {

    private final OdontogramColourConfigService colourConfigService;

    @PostMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_EDIT')")
    public ResponseEntity<OdontogramColourConfigResponse>
    saveColourConfig(
            @Valid
            @RequestBody
            SaveOdontogramColourConfigRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        colourConfigService
                                .saveColourConfig(request)
                );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public ResponseEntity<List<OdontogramColourConfigResponse>>
    getColourConfigs(
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                colourConfigService
                        .getColourConfigs(clinicId)
        );
    }
}