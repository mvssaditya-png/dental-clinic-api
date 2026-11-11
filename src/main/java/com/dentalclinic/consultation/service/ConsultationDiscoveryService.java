package com.dentalclinic.consultation.service;
import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.consultation.dto.ConsultationPage;
import com.dentalclinic.consultation.entity.ConsultationStatus;
import com.dentalclinic.consultation.repository.ConsultationRepository;
import com.dentalclinic.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
import java.util.Locale;
import java.time.LocalDate;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ConsultationDiscoveryService {
 private final TenantClinicResolver clinics;
 private final ConsultationRepository consultations;
 public ConsultationPage list(UUID clinicId,int page,int size,String search,ConsultationStatus status,LocalDate from,LocalDate to){
  if(page<0||size<1||(long)page*Math.min(size,100)>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid page or size");
  String term=search==null?"":search.trim();
  if(term.length()>100)throw new IllegalArgumentException("Search must not exceed 100 characters");
  if(from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException("From date must not be after to date");
  var clinic=clinics.resolve(SecurityUtils.getCurrentUser(),clinicId,"clinicId is required");
  String pattern=term.isEmpty()?null:"%"+term.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
  var result=consultations.discover(clinic.getId(),pattern,status,from,to,PageRequest.of(page,Math.min(size,100)));
  return new ConsultationPage(result.getContent(),result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
 }
}
