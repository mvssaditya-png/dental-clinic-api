package com.dentalclinic.casesheet.service;
import com.dentalclinic.audit.entity.AuditLog;
import com.dentalclinic.audit.repository.AuditLogRepository;
import com.dentalclinic.casesheet.entity.*;
import com.dentalclinic.casesheet.dto.EntrySnapshot;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.time.LocalDateTime;
@Component @RequiredArgsConstructor
public class EntryAudit {
 private final AuditLogRepository logs;
 public static EntrySnapshot snapshot(CaseSheetDiagnosis d){return new EntrySnapshot(d.getId(),d.getCaseSheet().getId(),d.getToothNumber(),d.getDiagnosisName(),d.getDiagnosisType().name(),d.getDiagnosisCode(),null,null,d.getNotes(),d.getVersion(),d.getCreatedAt(),d.getUpdatedAt(),d.getRemovedAt(),d.getRemovedBy()==null?null:d.getRemovedBy().getId());}
 public static EntrySnapshot snapshot(CaseSheetClinicalFinding f){return new EntrySnapshot(f.getId(),f.getCaseSheet().getId(),f.getToothNumber(),null,null,null,f.getFindingType(),f.getFindingValue(),f.getNotes(),f.getVersion(),f.getCreatedAt(),f.getUpdatedAt(),f.getRemovedAt(),f.getRemovedBy()==null?null:f.getRemovedBy().getId());}
 private static String str(Object value){return value==null?null:value.toString();}
 public static Map<String,Object> values(EntrySnapshot s){if(s==null)return null;Map<String,Object> m=new LinkedHashMap<>();
 m.put("id",str(s.id()));m.put("caseSheetId",str(s.caseSheetId()));m.put("toothNumber",s.toothNumber());m.put("diagnosisName",s.diagnosisName());m.put("diagnosisType",s.diagnosisType());m.put("diagnosisCode",s.diagnosisCode());m.put("findingType",s.findingType());m.put("findingValue",s.findingValue());m.put("notes",s.notes());m.put("version",Long.toString(s.version()));m.put("createdAt",str(s.createdAt()));m.put("updatedAt",str(s.updatedAt()));m.put("removedAt",str(s.removedAt()));m.put("removedBy",str(s.removedBy()));return m;}
 private static String text(Map<String,Object> m,String key){return str(m.get(key));}
 private static UUID uuid(Map<String,Object> m,String key){String v=text(m,key);return v==null?null:UUID.fromString(v);}
 private static LocalDateTime time(Map<String,Object> m,String key){String v=text(m,key);return v==null?null:LocalDateTime.parse(v);}
 public static EntrySnapshot read(Map<String,Object> m){if(m==null)return null;return new EntrySnapshot(uuid(m,"id"),uuid(m,"caseSheetId"),text(m,"toothNumber"),text(m,"diagnosisName"),text(m,"diagnosisType"),text(m,"diagnosisCode"),text(m,"findingType"),text(m,"findingValue"),text(m,"notes"),Long.parseLong(text(m,"version")),time(m,"createdAt"),time(m,"updatedAt"),time(m,"removedAt"),uuid(m,"removedBy"));}
 @Transactional(propagation=Propagation.MANDATORY)
 public void write(Clinic clinic,AppUser actor,String type,String operation,EntrySnapshot before,EntrySnapshot after,String reason){
 logs.save(AuditLog.builder().clinic(clinic).user(actor).module("CASE_SHEET_ENTRIES").entityType(type).entityId(after.id()).action(operation).oldValue(values(before)).newValue(values(after)).description(reason).build());
 }
}
