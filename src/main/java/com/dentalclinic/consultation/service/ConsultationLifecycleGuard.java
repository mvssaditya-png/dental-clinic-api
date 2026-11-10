package com.dentalclinic.consultation.service;

import com.dentalclinic.consultation.entity.*;
import com.dentalclinic.consultation.repository.ConsultationRepository;
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.casesheet.service.EntryException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;

/** Caller holds the Case Sheet lock first, when a sheet exists. */
@Component @RequiredArgsConstructor
public class ConsultationLifecycleGuard {
 private final ConsultationRepository consultations;
 private final EntityManager entityManager;
 public Consultation lock(UUID id, UUID clinic) {
  Consultation c=consultations.lockByIdAndClinicId(id,clinic)
      .orElseThrow(()->new EntryException(404,"Consultation not found"));
  // A prior relationship traversal may have populated the persistence context.
  entityManager.refresh(c,LockModeType.PESSIMISTIC_WRITE);
  return c;
 }
 public Consultation active(UUID id,UUID clinic) {
  Consultation c=lock(id,clinic);
  if(c.getStatus()!=ConsultationStatus.STARTED&&c.getStatus()!=ConsultationStatus.IN_PROGRESS)
   throw new EntryException(409,"Consultation is no longer active");
  return c;
 }
 public void active(CaseSheet sheet) {active(sheet.getConsultation().getId(),sheet.getClinic().getId());}
}
