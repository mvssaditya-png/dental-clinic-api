package com.dentalclinic.treatment.controller;

import com.dentalclinic.treatment.dto.ConditionProcedureMappingResponse;
import com.dentalclinic.treatment.dto.CreateConditionProcedureMappingRequest;
import com.dentalclinic.treatment.service.ConditionProcedureMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/odontogram-conditions")
@RequiredArgsConstructor
public class ConditionProcedureMappingController {

    private final ConditionProcedureMappingService
            conditionProcedureMappingService;

    @PostMapping("/{conditionId}/procedures")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<ConditionProcedureMappingResponse>
    createMapping(
            @PathVariable
            UUID conditionId,

            @Valid
            @RequestBody
            CreateConditionProcedureMappingRequest request
    ) {

        return ResponseEntity.ok(
                conditionProcedureMappingService
                        .createMapping(
                                conditionId,
                                request
                        )
        );
    }

    @GetMapping("/{conditionId}/procedures")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<List<ConditionProcedureMappingResponse>>
    getSuggestedProcedures(
            @PathVariable
            UUID conditionId,

            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                conditionProcedureMappingService
                        .getSuggestedProcedures(
                                conditionId,
                                clinicId
                        )
        );
    }
}