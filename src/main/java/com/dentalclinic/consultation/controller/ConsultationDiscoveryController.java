package com.dentalclinic.consultation.controller;
import com.dentalclinic.consultation.dto.ConsultationPage;
import com.dentalclinic.consultation.entity.ConsultationStatus;
import com.dentalclinic.consultation.service.ConsultationDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.UUID;
import java.time.LocalDate;
@RestController @RequiredArgsConstructor
public class ConsultationDiscoveryController {
 private final ConsultationDiscoveryService discovery;
 @GetMapping("/api/consultations") @PreAuthorize("hasAuthority('CONSULTATION_VIEW')")
 public ConsultationPage list(@RequestParam(required=false) UUID clinicId,@RequestParam(defaultValue="0") int page,
 @RequestParam(defaultValue="20") int size,@RequestParam(required=false) String search,
 @RequestParam(required=false) ConsultationStatus status,
 @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
 @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
  return discovery.list(clinicId,page,size,search,status,from,to);
 }

 @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
 public org.springframework.http.ResponseEntity<java.util.Map<String,Object>> invalidParameter(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException error){
  return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("status",400,"message","Invalid query parameter: "+error.getName(),"timestamp",java.time.LocalDateTime.now()));
 }
}
