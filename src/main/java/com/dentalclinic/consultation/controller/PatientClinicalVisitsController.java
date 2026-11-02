package com.dentalclinic.consultation.controller;

import com.dentalclinic.consultation.dto.ClinicalVisitPage;
import com.dentalclinic.consultation.service.ClinicalVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientClinicalVisitsController {
    private final ClinicalVisitService service;

    @GetMapping("/{patientId}/clinical-visits")
    @PreAuthorize("hasAuthority('CONSULTATION_VIEW') and hasAuthority('CASE_SHEET_VIEW')")
    public ClinicalVisitPage visits(@PathVariable UUID patientId,
            @RequestParam(required = false) UUID clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID excludeConsultationId) {
        return service.read(patientId, clinicId, page, size, excludeConsultationId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> unavailable(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "status", exception.getStatusCode().value(), "message", exception.getReason(),
                "timestamp", LocalDateTime.now()));
    }
}
