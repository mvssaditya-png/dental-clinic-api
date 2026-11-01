package com.dentalclinic.treatment.controller;

import com.dentalclinic.treatment.dto.RemoveTreatmentPlanRequest;
import com.dentalclinic.treatment.service.TreatmentPlanRemovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanRemovalController {
    private final TreatmentPlanRemovalService service;

    @PostMapping("/{id}/remove")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_EDIT')")
    public ResponseEntity<Void> remove(@PathVariable UUID id,
            @RequestParam(required = false) UUID clinicId,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody RemoveTreatmentPlanRequest request) {
        service.remove(id, clinicId, match, request);
        return ResponseEntity.noContent().build();
    }
}
