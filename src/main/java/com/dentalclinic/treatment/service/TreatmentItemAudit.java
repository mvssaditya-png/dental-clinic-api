package com.dentalclinic.treatment.service;
import com.dentalclinic.treatment.entity.*;import com.dentalclinic.treatment.repository.*;import com.dentalclinic.user.entity.AppUser;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import java.util.*;
@Component @RequiredArgsConstructor public class TreatmentItemAudit {
 private final TreatmentPlanItemAuditRepository repository;
 public static Map<String,Object> snapshot(TreatmentPlanItem i){var m=new LinkedHashMap<String,Object>();
 m.put("id",i.getId());m.put("clinicId",i.getClinic().getId());m.put("treatmentPlanId",i.getTreatmentPlan().getId());m.put("procedureId",i.getProcedure().getId());
 m.put("procedureCode",i.getProcedureCodeSnapshot());m.put("procedureName",i.getProcedureNameSnapshot());m.put("toothNumber",i.getToothNumber());m.put("quantity",i.getQuantity());m.put("estimatedVisits",i.getEstimatedVisits());m.put("sequenceNumber",i.getSequenceNumber());
 m.put("unitPrice",i.getUnitPrice());m.put("grossAmount",i.getGrossAmount());m.put("discountType",i.getDiscountType()==null?null:i.getDiscountType().name());m.put("discountValue",i.getDiscountValue());m.put("discountAmount",i.getDiscountAmount());m.put("finalAmount",i.getFinalAmount());
 m.put("description",i.getDescription());m.put("clinicalNotes",i.getClinicalNotes());m.put("patientExplanation",i.getPatientExplanation());m.put("followUpInstructions",i.getFollowUpInstructions());m.put("status",i.getStatus().name());m.put("version",i.getVersion());
 m.put("createdAt",Objects.toString(i.getCreatedAt(),null));m.put("updatedAt",Objects.toString(i.getUpdatedAt(),null));m.put("removedAt",Objects.toString(i.getRemovedAt(),null));m.put("removedBy",i.getRemovedBy()==null?null:i.getRemovedBy().getId());
 m.put("startedAt",Objects.toString(i.getStartedAt(),null));m.put("completedAt",Objects.toString(i.getCompletedAt(),null));m.put("cancelledAt",Objects.toString(i.getCancelledAt(),null));m.put("cancellationReason",i.getCancellationReason());return m; }
 @Transactional(propagation=Propagation.MANDATORY)
 public void write(TreatmentPlanItem item,AppUser actor,String operation,Map<String,Object> before,String reason){var row=new TreatmentPlanItemAudit();row.setClinicId(item.getClinic().getId());row.setTreatmentPlanId(item.getTreatmentPlan().getId());row.setItemId(item.getId());row.setActorId(actor.getId());row.setOperation(operation);row.setBeforeSnapshot(before);row.setAfterSnapshot(snapshot(item));row.setRemovalReason(reason);repository.saveAndFlush(row);}
}
