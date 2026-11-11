package com.dentalclinic.odontogram.service;
import com.dentalclinic.odontogram.entity.*;
import com.dentalclinic.odontogram.dto.OdontogramResponse;
import com.dentalclinic.odontogram.repository.OdontogramRepository;
import com.dentalclinic.patient.repository.PatientRepository;
import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.casesheet.validation.RemovalReasonValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class OdontogramArchiveService {
 private final OdontogramRepository charts; private final PatientRepository patients;
 private final TenantClinicResolver clinics; private final OdontogramService mapping; private final JdbcTemplate jdbc;
 private UUID clinic(UUID requested){return clinics.resolve(SecurityUtils.getCurrentUser(),requested,"clinicId is required for platform users").getId();}
 @Transactional(readOnly=true) public OdontogramResponse read(UUID id,UUID requested){return mapping.mapToResponse(charts.findByIdAndClinicId(id,clinic(requested)).orElseThrow(()->new OdontogramMutationException(404,"Odontogram not found")));}
 public record ArchivePage(List<OdontogramResponse> content,int number,int size,long totalElements,int totalPages){}
 @Transactional(readOnly=true) public ArchivePage list(UUID patient,UUID requested,int page,int size){
  if(page<0||size<1||(long)page*Math.min(size,100)>Integer.MAX_VALUE)throw new OdontogramMutationException(400,"Invalid page or size");
  UUID clinic=clinic(requested);patients.findByIdAndClinicId(patient,clinic).orElseThrow(()->new OdontogramMutationException(404,"Patient not found"));
  var result=charts.findByPatientIdAndClinicIdAndStatus(patient,clinic,OdontogramStatus.ARCHIVED,PageRequest.of(page,Math.min(size,100),Sort.by(Sort.Order.desc("archivedAt"),Sort.Order.desc("id"))));
  return new ArchivePage(result.map(mapping::mapToResponse).getContent(),page,result.getSize(),result.getTotalElements(),result.getTotalPages());
 }
 @Transactional public void archive(UUID id,UUID requested,String match,String reason){
  UUID clinic=clinic(requested);var expected=ToothPrecondition.parse(match,null);
  if(!RemovalReasonValidator.valid(reason))throw new OdontogramMutationException(400,"A nonblank reason of at most 1000 Unicode code points without control characters is required");
  // Only chart lock: never acquire Case Sheet/consultation locks after it.
  var chart=charts.lockByIdAndClinicId(id,clinic).orElseThrow(()->new OdontogramMutationException(404,"Odontogram not found"));
  if(!OdontogramArchiveAuthorization.allowed(chart))throw new org.springframework.security.access.AccessDeniedException("Access denied");
  if(chart.getStatus()==OdontogramStatus.ARCHIVED)throw new OdontogramMutationException(409,"Odontogram is already archived");
  if(!expected.version().equals(chart.getVersion()))throw new OdontogramMutationException(412,"Odontogram changed. Reload and review before archiving");
  long before=chart.getVersion();var now=LocalDateTime.now();var actor=SecurityUtils.getCurrentUser();
  chart.setStatus(OdontogramStatus.ARCHIVED);chart.setArchivedAt(now);chart.setArchivedBy(actor.getId());chart.setArchiveReason(reason);chart.setVersion(before+1);chart.setUpdatedBy(actor);
  charts.saveAndFlush(chart);
  jdbc.update("insert into odontogram_archive_audit(clinic_id,odontogram_id,patient_id,actor_id,archived_at,reason,previous_version,archived_version) values (?,?,?,?,?,?,?,?)",clinic,id,chart.getPatient().getId(),actor.getId(),now,reason,before,before+1);
 }
}
