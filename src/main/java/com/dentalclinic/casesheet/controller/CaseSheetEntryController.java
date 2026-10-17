package com.dentalclinic.casesheet.controller;
import com.dentalclinic.casesheet.dto.*;
import com.dentalclinic.casesheet.service.CaseSheetEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/case-sheets/{caseSheetId}") @RequiredArgsConstructor
public class CaseSheetEntryController {
 private final CaseSheetEntryService service;
 @PutMapping("/diagnoses/{diagnosisId}") @PreAuthorize("hasAuthority('CASE_SHEET_EDIT')")
 public CaseSheetDiagnosisResponse updateDiagnosis(@PathVariable UUID caseSheetId,@PathVariable UUID diagnosisId,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String version,@Valid @RequestBody UpdateDiagnosisRequest request){return service.updateDiagnosis(caseSheetId,diagnosisId,clinicId,version,request);}
 @PutMapping("/findings/{findingId}") @PreAuthorize("hasAuthority('CASE_SHEET_EDIT')")
 public CaseSheetClinicalFindingResponse updateFinding(@PathVariable UUID caseSheetId,@PathVariable UUID findingId,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String version,@Valid @RequestBody CreateCaseSheetClinicalFindingRequest request){return service.updateFinding(caseSheetId,findingId,clinicId,version,request);}
 @DeleteMapping("/diagnoses/{diagnosisId}") @PreAuthorize("hasAuthority('CASE_SHEET_EDIT')")
 public ResponseEntity<Void> removeDiagnosis(@PathVariable UUID caseSheetId,@PathVariable UUID diagnosisId,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String version,@RequestHeader(value="X-Removal-Reason",required=false) String reason){service.remove(caseSheetId,diagnosisId,clinicId,version,reason,true);return ResponseEntity.noContent().build();}
 @DeleteMapping("/findings/{findingId}") @PreAuthorize("hasAuthority('CASE_SHEET_EDIT')")
 public ResponseEntity<Void> removeFinding(@PathVariable UUID caseSheetId,@PathVariable UUID findingId,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String version,@RequestHeader(value="X-Removal-Reason",required=false) String reason){service.remove(caseSheetId,findingId,clinicId,version,reason,false);return ResponseEntity.noContent().build();}
 @GetMapping("/entry-history") @PreAuthorize("hasAuthority('CASE_SHEET_HISTORY_VIEW')")
 public CaseSheetEntryService.HistoryPage history(@PathVariable UUID caseSheetId,@RequestParam(required=false) UUID clinicId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.history(caseSheetId,clinicId,page,size);}
}
