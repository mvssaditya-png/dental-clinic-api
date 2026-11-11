package com.dentalclinic.casesheet.service;

import com.dentalclinic.casesheet.entity.*;
import com.dentalclinic.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class TreatmentDecisionService {
 private final JdbcTemplate jdbc;
 public static boolean blank(String value){return value==null||value.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c));}
 public static void validateDraft(TreatmentPlanRequirement requirement,String reason){
  if(requirement==TreatmentPlanRequirement.DEFERRED&&blank(reason))throw new EntryException(400,"A reason is required when the treatment decision is deferred");
 }
 private List<Map<String,Object>> plans(CaseSheet sheet,boolean lock){
  // Existing plan/item writers lock the plan first. We lock sheet -> consultation -> plans;
  // no plan writer acquires a sheet lock afterwards. Stable ID ordering avoids plan deadlocks.
  return jdbc.queryForList("""
   select id,treatment_plan_number,status,version from treatment_plan
   where case_sheet_id=? and consultation_id=? and patient_id=? and clinic_id=?
   and removed_at is null and status in ('PRESENTED','APPROVED','IN_PROGRESS','COMPLETED','DECLINED')
   order by id
   """+(lock?" for update":""),sheet.getId(),sheet.getConsultation().getId(),sheet.getPatient().getId(),sheet.getClinic().getId());
 }
 private Map<String,Object> qualifying(CaseSheet sheet,boolean lock){
  for(var plan:plans(sheet,lock))if(Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from treatment_plan_item where treatment_plan_id=? and clinic_id=? and removed_at is null and status <> 'CANCELLED')",Boolean.class,plan.get("id"),sheet.getClinic().getId())))return plan;
  return null;
 }
 public String blocker(CaseSheet sheet){
  if(sheet.getStatus()!=CaseSheetStatus.DRAFT)return null;
  if(sheet.getTreatmentPlanRequirement()==null)return "Select a treatment decision before finalizing the Case Sheet.";
  if(sheet.getTreatmentPlanRequirement()==TreatmentPlanRequirement.DEFERRED&&blank(sheet.getTreatmentDecisionReason()))return "Record a reason for deferring the treatment decision.";
  if(sheet.getTreatmentPlanRequirement()==TreatmentPlanRequirement.REQUIRED&&qualifying(sheet,false)==null)return "A linked Treatment Plan with saved procedures must be presented before finalization. Approval is not required.";
  return null;
 }
 public void recordFinalization(CaseSheet sheet){
  var requirement=sheet.getTreatmentPlanRequirement();
  if(requirement==null)throw new EntryException(409,"Select a treatment decision before finalizing the Case Sheet");
  validateDraft(requirement,sheet.getTreatmentDecisionReason());
  var plan=requirement==TreatmentPlanRequirement.REQUIRED?qualifying(sheet,true):null;
  if(requirement==TreatmentPlanRequirement.REQUIRED&&plan==null)throw new EntryException(409,"A qualifying Treatment Plan for this Case Sheet is required before finalization");
  jdbc.update("""
   insert into case_sheet_treatment_finalization(case_sheet_id,clinic_id,consultation_id,patient_id,requirement,reason,actor_id,validated_at,treatment_plan_id,treatment_plan_number,treatment_plan_status,treatment_plan_version)
   values (?,?,?,?,?,?,?,?,?,?,?,?)
   """,sheet.getId(),sheet.getClinic().getId(),sheet.getConsultation().getId(),sheet.getPatient().getId(),requirement.name(),sheet.getTreatmentDecisionReason(),SecurityUtils.getCurrentUserId(),LocalDateTime.now(),plan==null?null:plan.get("id"),plan==null?null:plan.get("treatment_plan_number"),plan==null?null:plan.get("status"),plan==null?null:plan.get("version"));
 }
 public void validateCompletion(CaseSheet sheet){
  // Historical finalized records with no decision remain historical; never fabricate a decision.
  if(sheet.getTreatmentPlanRequirement()==null)return;
  if(!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from case_sheet_treatment_finalization where case_sheet_id=? and clinic_id=? and consultation_id=? and patient_id=? and requirement=? and reason is not distinct from cast(? as text))",Boolean.class,sheet.getId(),sheet.getClinic().getId(),sheet.getConsultation().getId(),sheet.getPatient().getId(),sheet.getTreatmentPlanRequirement().name(),sheet.getTreatmentDecisionReason())))throw new EntryException(409,"Finalized treatment decision evidence is unavailable");
 }
}
