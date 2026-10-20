package com.dentalclinic.treatment.controller;

import com.dentalclinic.treatment.dto.TreatmentPlanPage;
import com.dentalclinic.treatment.service.TreatmentPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TreatmentPlanDiscoveryController {
    private final TreatmentPlanService service;

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public java.util.Map<String, Object> invalidParameter() {
        return java.util.Map.of("status", 400, "message", "Invalid discovery request parameter");
    }

    @GetMapping("/patients/{patientId}/treatment-plans")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_VIEW')")
    public TreatmentPlanPage byPatient(@PathVariable UUID patientId,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.discoverByPatient(patientId, clinicId, page, size);
    }

    @GetMapping("/case-sheets/{caseSheetId}/treatment-plans")
    @PreAuthorize("hasAuthority('TREATMENT_PLAN_VIEW')")
    public TreatmentPlanPage byCaseSheet(@PathVariable UUID caseSheetId,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.discoverByCaseSheet(caseSheetId, clinicId, page, size);
    }
}
