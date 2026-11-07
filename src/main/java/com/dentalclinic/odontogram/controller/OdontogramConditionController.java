package com.dentalclinic.odontogram.controller;
import com.dentalclinic.odontogram.dto.*;
import com.dentalclinic.odontogram.service.OdontogramConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/odontogram-conditions") @RequiredArgsConstructor
public class OdontogramConditionController {
 private final OdontogramConfigurationService service;
 @PostMapping @PreAuthorize("hasAuthority('ODONTOGRAM_CONFIGURE')")
 public ResponseEntity<ManagedOdontogramConditionResponse> createCondition(@Valid @RequestBody CreateOdontogramConditionRequest request){return ResponseEntity.status(201).body(service.create(request));}
 @GetMapping @PreAuthorize("hasAuthority('ODONTOGRAM_CONFIGURE') or (!#includeInactive and hasAuthority('ODONTOGRAM_VIEW'))")
 public List<ManagedOdontogramConditionResponse> getConditions(@RequestParam(required=false) UUID clinicId,@RequestParam(defaultValue="false") boolean includeInactive){return service.list(clinicId,includeInactive);}
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('ODONTOGRAM_VIEW','ODONTOGRAM_CONFIGURE')")
 public ManagedOdontogramConditionResponse get(@PathVariable UUID id,@RequestParam(required=false) UUID clinicId){return service.get(id,clinicId);}
 @PutMapping("/{id}") @PreAuthorize("hasAuthority('ODONTOGRAM_CONFIGURE')")
 public ManagedOdontogramConditionResponse update(@PathVariable UUID id,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody UpdateOdontogramConditionRequest request){return service.update(id,clinicId,match,request);}
}
