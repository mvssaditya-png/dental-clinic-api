package com.dentalclinic.odontogram.controller;
import com.dentalclinic.odontogram.service.OdontogramArchiveService;
import com.dentalclinic.odontogram.dto.OdontogramResponse;
import com.dentalclinic.casesheet.dto.RemoveClinicalEntryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class OdontogramArchiveController {
 private final OdontogramArchiveService service;
 @PostMapping("/api/odontograms/{id}/archive") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
 @PreAuthorize("hasAuthority('ODONTOGRAM_ARCHIVE')")
 public void archive(@PathVariable UUID id,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody RemoveClinicalEntryRequest body){service.archive(id,clinicId,match,body.getReason());}
 @GetMapping("/api/odontograms/{id}") @PreAuthorize("hasAuthority('ODONTOGRAM_VIEW')")
 public OdontogramResponse read(@PathVariable UUID id,@RequestParam(required=false) UUID clinicId){return service.read(id,clinicId);}
 @GetMapping("/api/patients/{patientId}/odontograms/archived") @PreAuthorize("hasAuthority('ODONTOGRAM_VIEW')")
 public OdontogramArchiveService.ArchivePage archived(@PathVariable UUID patientId,@RequestParam(required=false) UUID clinicId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(patientId,clinicId,page,size);}
}
