package com.dentalclinic.treatment.controller;

import com.dentalclinic.treatment.dto.TreatmentPlanLifecyclePage;
import com.dentalclinic.treatment.service.TreatmentPlanLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanLifecycleController {
    private final TreatmentPlanLifecycleService service;

    @GetMapping("/{id}/lifecycle-history")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_HISTORY_VIEW')")
    public TreatmentPlanLifecyclePage history(@PathVariable UUID id,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.read(id, clinicId, page, size);
    }
}
