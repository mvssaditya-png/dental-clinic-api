package com.dentalclinic.clinic.controller;
import com.dentalclinic.clinic.dto.*;
import com.dentalclinic.clinic.service.ClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/clinics") @RequiredArgsConstructor
public class ClinicController {
    private final ClinicService service;
    @GetMapping @PreAuthorize("hasAuthority('CLINIC_VIEW')")
    public Page<ClinicSummaryResponse> list(@RequestParam(required=false) String search,
            @RequestParam(required=false) Boolean active, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return service.list(search, active, page, size); }
    @GetMapping("/{clinicId}") @PreAuthorize("hasAuthority('CLINIC_VIEW')")
    public ClinicResponse get(@PathVariable UUID clinicId) { return service.get(clinicId); }
    @PostMapping @PreAuthorize("hasAuthority('CLINIC_CREATE')")
    public ResponseEntity<ClinicResponse> create(@Valid @RequestBody CreateClinicRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
    @PutMapping("/{clinicId}") @PreAuthorize("hasAuthority('CLINIC_EDIT')")
    public ClinicResponse update(@PathVariable UUID clinicId, @Valid @RequestBody UpdateClinicRequest request) {
        return service.update(clinicId, request);
    }
    @PatchMapping("/{clinicId}/status") @PreAuthorize("hasAuthority('CLINIC_STATUS_MANAGE')")
    public ClinicResponse status(@PathVariable UUID clinicId, @Valid @RequestBody UpdateClinicStatusRequest request) {
        return service.updateStatus(clinicId, request);
    }
    @GetMapping("/{clinicId}/settings") @PreAuthorize("hasAuthority('CLINIC_SETTINGS_MANAGE')")
    public ClinicSettingsResponse settings(@PathVariable UUID clinicId) { return service.getSettings(clinicId); }
    @PutMapping("/{clinicId}/settings") @PreAuthorize("hasAuthority('CLINIC_SETTINGS_MANAGE')")
    public ClinicSettingsResponse settings(@PathVariable UUID clinicId, @Valid @RequestBody UpdateClinicSettingsRequest request) {
        return service.updateSettings(clinicId, request);
    }
}

