package com.dentalclinic.treatment.controller;

import com.dentalclinic.treatment.dto.CreateProcedurePriceRequest;
import com.dentalclinic.treatment.dto.CreateProcedureRequest;
import com.dentalclinic.treatment.dto.ProcedurePriceResponse;
import com.dentalclinic.treatment.dto.ProcedureResponse;
import com.dentalclinic.treatment.service.ProcedureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService procedureService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROCEDURE_MANAGE')")
    public ResponseEntity<ProcedureResponse>
    createProcedure(
            @Valid
            @RequestBody
            CreateProcedureRequest request
    ) {

        return ResponseEntity.ok(
                procedureService
                        .createProcedure(
                                request
                        )
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROCEDURE_VIEW')")
    public ResponseEntity<List<ProcedureResponse>>
    getProcedures(
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                procedureService
                        .getProcedures(
                                clinicId
                        )
        );
    }

    @GetMapping("/{procedureId}")
    @PreAuthorize("hasAuthority('PROCEDURE_VIEW')")
    public ResponseEntity<ProcedureResponse>
    getProcedure(
            @PathVariable
            UUID procedureId,

            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                procedureService
                        .getProcedure(
                                procedureId,
                                clinicId
                        )
        );
    }

    @PostMapping("/{procedureId}/prices")
    @PreAuthorize("hasAuthority('PROCEDURE_MANAGE')")
    public ResponseEntity<ProcedurePriceResponse>
    addPrice(
            @PathVariable
            UUID procedureId,

            @Valid
            @RequestBody
            CreateProcedurePriceRequest request
    ) {

        return ResponseEntity.ok(
                procedureService
                        .addPrice(
                                procedureId,
                                request
                        )
        );
    }

    @GetMapping("/{procedureId}/prices")
    @PreAuthorize("hasAuthority('PROCEDURE_MANAGE')")
    public ResponseEntity<List<ProcedurePriceResponse>>
    getPriceHistory(
            @PathVariable
            UUID procedureId,

            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                procedureService
                        .getPriceHistory(
                                procedureId,
                                clinicId
                        )
        );
    }

    @GetMapping("/{procedureId}/current-price")
    @PreAuthorize("hasAuthority('PROCEDURE_VIEW')")
    public ResponseEntity<ProcedurePriceResponse>
    getCurrentPrice(
            @PathVariable
            UUID procedureId,

            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                procedureService
                        .getCurrentPrice(
                                procedureId,
                                clinicId
                        )
        );
    }
}