package com.dentalclinic.odontogram.controller;

import com.dentalclinic.odontogram.dto.OdontogramToothHistoryResponse;
import com.dentalclinic.odontogram.dto.OdontogramToothResponse;
import com.dentalclinic.odontogram.dto.UpdateOdontogramToothRequest;
import com.dentalclinic.odontogram.service.OdontogramService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/odontograms")
@RequiredArgsConstructor
public class OdontogramToothController {

    private final OdontogramService odontogramService;

    @PutMapping(
            "/{odontogramId}/teeth/{toothNumber}"
    )
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<OdontogramToothResponse>
    updateTooth(
            @PathVariable UUID odontogramId,
            @PathVariable String toothNumber,
            @Valid
            @RequestBody
            UpdateOdontogramToothRequest request
    ) {

        return ResponseEntity.ok(
                odontogramService
                        .updateTooth(
                                odontogramId,
                                toothNumber,
                                request
                        )
        );
    }

    @GetMapping("/{odontogramId}/teeth")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<List<OdontogramToothResponse>>
    getTeeth(
            @PathVariable UUID odontogramId,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                odontogramService.getTeeth(
                        odontogramId,
                        clinicId
                )
        );
    }

    @GetMapping(
            "/{odontogramId}/teeth/{toothNumber}/history"
    )
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<List<OdontogramToothHistoryResponse>>
    getToothHistory(
            @PathVariable UUID odontogramId,
            @PathVariable String toothNumber,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                odontogramService
                        .getToothHistory(
                                odontogramId,
                                toothNumber,
                                clinicId
                        )
        );
    }
}