package com.dentalclinic.treatment.controller;

import com.dentalclinic.treatment.dto.*;
import com.dentalclinic.treatment.service.TreatmentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanController {

    private final TreatmentPlanService
            treatmentPlanService;

    @PostMapping
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<TreatmentPlanResponse>
    createTreatmentPlan(
            @Valid
            @RequestBody
            CreateTreatmentPlanRequest request
    ) {

        return ResponseEntity.ok(
                treatmentPlanService
                        .createTreatmentPlan(
                                request
                        )
        );
    }

    @GetMapping("/{treatmentPlanId}")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<TreatmentPlanResponse>
    getTreatmentPlan(
            @PathVariable
            UUID treatmentPlanId,

            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                treatmentPlanService
                        .getTreatmentPlan(
                                treatmentPlanId,
                                clinicId
                        )
        );
    }

    @PostMapping("/{treatmentPlanId}/items")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<TreatmentPlanItemResponse>
    addTreatmentPlanItem(
            @PathVariable
            UUID treatmentPlanId,

            @Valid
            @RequestBody
            CreateTreatmentPlanItemRequest request
    ) {

        return ResponseEntity.ok(
                treatmentPlanService
                        .addTreatmentPlanItem(
                                treatmentPlanId,
                                request
                        )
        );
    }

    @GetMapping("/{treatmentPlanId}/items")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<List<TreatmentPlanItemResponse>>
    getTreatmentPlanItems(
            @PathVariable
            UUID treatmentPlanId,

            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                treatmentPlanService
                        .getTreatmentPlanItems(
                                treatmentPlanId,
                                clinicId
                        )
        );
    }

    @PutMapping("/{treatmentPlanId}")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<TreatmentPlanResponse>
    updateTreatmentPlan(
            @PathVariable
            UUID treatmentPlanId,

            @Valid
            @RequestBody
            UpdateTreatmentPlanRequest request
    ) {

        return ResponseEntity.ok(
                treatmentPlanService
                        .updateTreatmentPlan(
                                treatmentPlanId,
                                request
                        )
        );
    }

    @PatchMapping("/{treatmentPlanId}/status")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<TreatmentPlanResponse>
    updateTreatmentPlanStatus(
            @PathVariable
            UUID treatmentPlanId,

            @Valid
            @RequestBody
            UpdateTreatmentPlanStatusRequest request
    ) {

        return ResponseEntity.ok(
                treatmentPlanService
                        .updateTreatmentPlanStatus(
                                treatmentPlanId,
                                request
                        )
        );
    }

    @PatchMapping(
            "/{treatmentPlanId}/items/{itemId}/status"
    )
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_EDIT')"
    )
    public ResponseEntity<TreatmentPlanItemResponse>
    updateTreatmentPlanItemStatus(
            @PathVariable
            UUID treatmentPlanId,

            @PathVariable
            UUID itemId,

            @Valid
            @RequestBody
            UpdateTreatmentPlanItemStatusRequest request
    ) {

        return ResponseEntity.ok(
                treatmentPlanService
                        .updateTreatmentPlanItemStatus(
                                treatmentPlanId,
                                itemId,
                                request
                        )
        );
    }
}