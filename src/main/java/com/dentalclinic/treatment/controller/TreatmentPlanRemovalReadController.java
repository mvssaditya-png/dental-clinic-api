package com.dentalclinic.treatment.controller;

import com.dentalclinic.treatment.service.TreatmentPlanRemovalReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/treatment-plans") @RequiredArgsConstructor
public class TreatmentPlanRemovalReadController {
    private final TreatmentPlanRemovalReadService service;

    @GetMapping("/{id}/removal-state")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_VIEW')")
    public TreatmentPlanRemovalReadService.RemovalState state(@PathVariable UUID id,
            @RequestParam(required = false) UUID clinicId) {
        return service.state(id, clinicId);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_HISTORY_VIEW')")
    public TreatmentPlanRemovalReadService.HistoryPage history(@PathVariable UUID id,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.history(id, clinicId, page, size);
    }
}
