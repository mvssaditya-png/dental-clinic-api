package com.dentalclinic.casesheet.service;
import com.dentalclinic.casesheet.dto.*;
import com.dentalclinic.casesheet.entity.*;
import com.dentalclinic.casesheet.repository.*;
import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class CaseSheetEntryService {
 private final TenantClinicResolver clinics;
 private final CaseSheetRepository sheets;
 private final CaseSheetDiagnosisRepository diagnoses;
 private final CaseSheetClinicalFindingRepository findings;
 private final EntryHistoryRepository history;
 private final EntryAudit audit;
 public static long version(String value){if(value==null)throw new EntryException(428,"If-Match version is required");if(!value.matches("\"[0-9]+\""))throw new EntryException(400,"If-Match must contain one quoted nonnegative version");try{return Long.parseLong(value.substring(1,value.length()-1));}catch(NumberFormatException e){throw new EntryException(400,"Invalid If-Match version");}}
 public static String reason(String value){if(value==null||value.isBlank()||value.length()>1000||value.chars().anyMatch(Character::isISOControl))throw new EntryException(400,"X-Removal-Reason must contain 1-1000 characters without control characters");return value.trim();}
 private static String normalize(String value){return value==null||value.isBlank()?null:value.trim();}
 private CaseSheet parent(UUID id,UUID requested,AppUser actor){var clinic=clinics.resolve(actor,requested,"clinicId is required for platform users");var parent=sheets.lockByIdAndClinicId(id,clinic.getId()).orElseThrow(()->new EntryException(404,"Case sheet not found"));if(parent.getStatus()!=CaseSheetStatus.DRAFT)throw new EntryException(409,"Only DRAFT case sheets can be changed");return parent;}
 private static void active(LocalDateTime removed,long actual,long expected){if(removed!=null)throw new EntryException(409,"Entry has already been removed");if(actual!=expected)throw new EntryException(412,"Entry changed; reload before trying again");}
 @Transactional
 public CaseSheetDiagnosisResponse updateDiagnosis(UUID parentId,UUID id,UUID clinicId,String match,UpdateDiagnosisRequest request){
 long expected=version(match);var actor=SecurityUtils.getCurrentUser();var parent=parent(parentId,clinicId,actor);var d=diagnoses.findByIdAndCaseSheetIdAndClinicId(id,parentId,parent.getClinic().getId()).orElseThrow(()->new EntryException(404,"Diagnosis not found"));active(d.getRemovedAt(),d.getVersion(),expected);var before=EntryAudit.snapshot(d);
 d.setDiagnosisName(request.getDiagnosisName().trim());d.setDiagnosisType(request.getDiagnosisType());d.setDiagnosisCode(normalize(request.getDiagnosisCode()));d.setToothNumber(normalize(request.getToothNumber()));d.setNotes(normalize(request.getNotes()));d.setUpdatedAt(LocalDateTime.now());d.setUpdatedBy(actor);diagnoses.saveAndFlush(d);audit.write(parent.getClinic(),actor,"DIAGNOSIS","UPDATE",before,EntryAudit.snapshot(d),null);return diagnosisResponse(d);
 }
 @Transactional
 public CaseSheetClinicalFindingResponse updateFinding(UUID parentId,UUID id,UUID clinicId,String match,CreateCaseSheetClinicalFindingRequest request){
 long expected=version(match);var actor=SecurityUtils.getCurrentUser();var parent=parent(parentId,clinicId,actor);var f=findings.findByIdAndCaseSheetIdAndClinicId(id,parentId,parent.getClinic().getId()).orElseThrow(()->new EntryException(404,"Finding not found"));active(f.getRemovedAt(),f.getVersion(),expected);var before=EntryAudit.snapshot(f);
 f.setFindingType(request.getFindingType().trim());f.setFindingValue(normalize(request.getFindingValue()));f.setToothNumber(normalize(request.getToothNumber()));f.setNotes(normalize(request.getNotes()));f.setUpdatedAt(LocalDateTime.now());f.setUpdatedBy(actor);findings.saveAndFlush(f);audit.write(parent.getClinic(),actor,"FINDING","UPDATE",before,EntryAudit.snapshot(f),null);return findingResponse(f);
 }
 @Transactional
 public void remove(UUID parentId,UUID id,UUID clinicId,String match,String removalReason,boolean diagnosis){
 removeValidated(parentId,id,clinicId,version(match),reason(removalReason),diagnosis);
 }
 @Transactional
 public void removeJson(UUID parentId,UUID id,UUID clinicId,String match,RemoveClinicalEntryRequest request,boolean diagnosis){
 long expected=version(match);
 if(request==null||!com.dentalclinic.casesheet.validation.RemovalReasonValidator.valid(request.getReason()))throw new EntryException(400,"Invalid removal reason: provide nonblank Unicode text, at most 1000 code points, without control characters");
 removeValidated(parentId,id,clinicId,expected,request.getReason(),diagnosis);
 }
 private void removeValidated(UUID parentId,UUID id,UUID clinicId,long expected,String reason,boolean diagnosis){
 var actor=SecurityUtils.getCurrentUser();var parent=parent(parentId,clinicId,actor);LocalDateTime now=LocalDateTime.now();
 if(diagnosis){var d=diagnoses.findByIdAndCaseSheetIdAndClinicId(id,parentId,parent.getClinic().getId()).orElseThrow(()->new EntryException(404,"Diagnosis not found"));active(d.getRemovedAt(),d.getVersion(),expected);var before=EntryAudit.snapshot(d);d.setRemovedAt(now);d.setRemovedBy(actor);d.setUpdatedAt(now);d.setUpdatedBy(actor);diagnoses.saveAndFlush(d);audit.write(parent.getClinic(),actor,"DIAGNOSIS","REMOVE",before,EntryAudit.snapshot(d),reason);}
 else{var f=findings.findByIdAndCaseSheetIdAndClinicId(id,parentId,parent.getClinic().getId()).orElseThrow(()->new EntryException(404,"Finding not found"));active(f.getRemovedAt(),f.getVersion(),expected);var before=EntryAudit.snapshot(f);f.setRemovedAt(now);f.setRemovedBy(actor);f.setUpdatedAt(now);f.setUpdatedBy(actor);findings.saveAndFlush(f);audit.write(parent.getClinic(),actor,"FINDING","REMOVE",before,EntryAudit.snapshot(f),reason);}
 }
 public record HistoryPage(List<EntryHistoryResponse> content,int number,int size,long totalElements,int totalPages){}
 @Transactional(readOnly=true)
 public HistoryPage history(UUID parentId,UUID requested,int page,int size){
 if(page<0||size<1)throw new EntryException(400,"Invalid page or size");var clinic=clinics.resolve(SecurityUtils.getCurrentUser(),requested,"clinicId is required for platform users");sheets.findByIdAndClinicIdWithDetails(parentId,clinic.getId()).orElseThrow(()->new EntryException(404,"Case sheet not found"));var result=history.history(clinic.getId(),parentId.toString(),PageRequest.of(page,Math.min(size,100)));
 var content=result.getContent().stream().map(a->new EntryHistoryResponse(a.getId(),parentId,a.getEntityType(),a.getEntityId(),a.getAction(),a.getUser()==null?null:a.getUser().getId(),a.getCreatedAt(),EntryAudit.read(a.getOldValue()),EntryAudit.read(a.getNewValue()),"REMOVE".equals(a.getAction())?a.getDescription():null)).toList();return new HistoryPage(content,result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
 }
 static CaseSheetDiagnosisResponse diagnosisResponse(CaseSheetDiagnosis d){return CaseSheetDiagnosisResponse.builder().id(d.getId()).caseSheetId(d.getCaseSheet().getId()).diagnosisName(d.getDiagnosisName()).diagnosisType(d.getDiagnosisType()).diagnosisCode(d.getDiagnosisCode()).toothNumber(d.getToothNumber()).notes(d.getNotes()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).version(d.getVersion()).build();}
 static CaseSheetClinicalFindingResponse findingResponse(CaseSheetClinicalFinding f){return CaseSheetClinicalFindingResponse.builder().id(f.getId()).caseSheetId(f.getCaseSheet().getId()).findingType(f.getFindingType()).findingValue(f.getFindingValue()).toothNumber(f.getToothNumber()).notes(f.getNotes()).createdAt(f.getCreatedAt()).updatedAt(f.getUpdatedAt()).version(f.getVersion()).build();}
}
