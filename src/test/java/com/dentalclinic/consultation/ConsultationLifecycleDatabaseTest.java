package com.dentalclinic.consultation;
import com.dentalclinic.consultation.service.*;
import com.dentalclinic.consultation.dto.*;
import com.dentalclinic.consultation.entity.*;
import com.dentalclinic.consultation.controller.ConsultationController;
import com.dentalclinic.casesheet.service.*;
import com.dentalclinic.casesheet.dto.*;
import com.dentalclinic.casesheet.controller.CaseSheetController;
import com.dentalclinic.odontogram.service.OdontogramService;
import com.dentalclinic.odontogram.dto.*;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named="CONSULTATION_LIFECYCLE_TEST_DATABASE",matches="true")
class ConsultationLifecycleDatabaseTest {
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){String u=System.getenv("SPRING_DATASOURCE_URL");if(u==null||!u.matches("jdbc:postgresql://localhost:5432/dental_clinic_v18_verification_10h2_[a-z0-9_]+"))throw new IllegalStateException("Isolated 10H2 database required");r.add("spring.datasource.url",()->u);}
 @Autowired JdbcTemplate jdbc; @Autowired ConsultationService consultations; @Autowired CaseSheetService sheets; @Autowired OdontogramService teeth;
 @Autowired ConsultationController controller; @Autowired CaseSheetController sheetController; @Autowired PlatformTransactionManager transactions;
 UUID clinic,user,patient,doctor,appointment,consultation,parent; AppUser actor;
 @BeforeEach void fixture(){clinic=UUID.randomUUID();user=UUID.randomUUID();patient=UUID.randomUUID();doctor=UUID.randomUUID();appointment=UUID.randomUUID();consultation=UUID.randomUUID();parent=UUID.randomUUID();
 jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",clinic,clinic.toString(),"Test clinic");
 jdbc.update("insert into app_user(id,clinic_id,first_name) values (?,?,?)",user,clinic,"Test actor");
 jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Patient','OTHER','123')",patient,clinic,patient.toString());
 jdbc.update("insert into doctor_profile(id,clinic_id,user_id) values (?,?,?)",doctor,clinic,user);
 jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'09:00','09:30')",appointment,clinic,appointment.toString(),patient,doctor);
 jdbc.update("insert into consultation(id,clinic_id,appointment_id,patient_id,doctor_id) values (?,?,?,?,?)",consultation,clinic,appointment,patient,doctor);
 jdbc.update("insert into case_sheet(id,clinic_id,consultation_id,appointment_id,patient_id,doctor_id,case_sheet_number,chief_complaint) values (?,?,?,?,?,?,?,'Pain')",parent,clinic,consultation,appointment,patient,doctor,parent.toString());
 actor=AppUser.builder().id(user).clinic(Clinic.builder().id(clinic).active(true).build()).build();login();}
 void login(String... p){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,Arrays.stream(p).map(SimpleGrantedAuthority::new).toList()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 UpdateConsultationStatusRequest status(ConsultationStatus value){var r=new UpdateConsultationStatusRequest();r.setClinicId(clinic);r.setStatus(value);return r;}
 void change(ConsultationStatus value){consultations.updateStatus(consultation,status(value));}
 long auditCount(){return jdbc.queryForObject("select count(*) from consultation_status_audit where consultation_id=?",Long.class,consultation);}
 void diagnosis(){var r=new CreateCaseSheetDiagnosisRequest();r.setDiagnosisName("Caries");sheets.addDiagnosis(parent,clinic,r);}
 void finalizeSheet(){var r=new FinalizeCaseSheetRequest();r.setClinicId(clinic);sheets.finalizeCaseSheet(parent,r,"\""+sheets.getCaseSheet(parent,clinic).getVersion()+"\"");}
 UpdateCaseSheetRequest text(String value){var r=new UpdateCaseSheetRequest();r.setChiefComplaint(value);return r;}
 void write(String value,String version){sheets.updateCaseSheet(parent,clinic,text(value),version);}
 UUID chart(){return teeth.createOdontogram(patient,clinic).getId();}
 void tooth(UUID id){var r=new UpdateOdontogramToothRequest();r.setClinicId(clinic);r.setCaseSheetId(parent);r.setNotes("Observed");teeth.updateTooth(id,"46",r,null,"*");}
 @Test void validProgressionTimestampsAndAudit(){var started=consultations.getConsultation(consultation,clinic).getStartedAt();change(ConsultationStatus.IN_PROGRESS);assertNull(consultations.getConsultation(consultation,clinic).getEndedAt());diagnosis();finalizeSheet();change(ConsultationStatus.COMPLETED);var c=consultations.getConsultation(consultation,clinic);assertEquals(started,c.getStartedAt());assertNotNull(c.getEndedAt());assertEquals(2,auditCount());var row=jdbc.queryForMap("select * from consultation_status_audit where consultation_id=? and to_status='COMPLETED'",consultation);assertEquals(user,row.get("actor_id"));assertEquals(clinic,row.get("clinic_id"));assertEquals(c.getEndedAt(),((java.sql.Timestamp)row.get("changed_at")).toLocalDateTime());assertEquals("SCHEDULED",jdbc.queryForObject("select status from appointment where id=?",String.class,appointment));}
 @Test void cancelledTimestampAndTerminalRejection(){change(ConsultationStatus.CANCELLED);var ended=consultations.getConsultation(consultation,clinic).getEndedAt();assertNotNull(ended);assertEquals(409,assertThrows(EntryException.class,()->change(ConsultationStatus.IN_PROGRESS)).status);assertEquals(ended,consultations.getConsultation(consultation,clinic).getEndedAt());assertEquals(1,auditCount());}
 @Test void invalidAndDuplicateTransitions(){assertEquals(409,assertThrows(EntryException.class,()->change(ConsultationStatus.COMPLETED)).status);assertEquals(409,assertThrows(EntryException.class,()->change(ConsultationStatus.STARTED)).status);change(ConsultationStatus.IN_PROGRESS);assertEquals(409,assertThrows(EntryException.class,()->change(ConsultationStatus.COMPLETED)).status);assertEquals(1,auditCount());}
 @Test void textVersionAndStaleReplacement(){var before=sheets.getCaseSheet(parent,clinic);assertEquals(0,before.getVersion());write("First","\"0\"");assertEquals(1,sheets.getCaseSheet(parent,clinic).getVersion());assertEquals(412,assertThrows(EntryException.class,()->write("Lost","\"0\"")).status);assertEquals("First",sheets.getCaseSheet(parent,clinic).getChiefComplaint());}
 @Test void missingAndMalformedVersions(){assertEquals(428,assertThrows(EntryException.class,()->write("Lost",null)).status);for(String v:List.of("0","*","W/\"0\"","\"-1\"","\"99999999999999999999999\""))assertEquals(400,assertThrows(EntryException.class,()->write("Lost",v)).status);assertEquals("Pain",sheets.getCaseSheet(parent,clinic).getChiefComplaint());}
 @Test void cancelledBlocksTextEntriesAndTooth(){var id=chart();change(ConsultationStatus.CANCELLED);assertEquals(409,assertThrows(EntryException.class,()->write("Late","\"0\"")).status);assertEquals(409,assertThrows(EntryException.class,this::diagnosis).status);assertEquals(409,assertThrows(EntryException.class,()->tooth(id)).status);}
 @Test void cancelledBlocksSheetCreation(){jdbc.update("delete from case_sheet where id=?",parent);change(ConsultationStatus.CANCELLED);var r=new CreateCaseSheetRequest();r.setConsultationId(consultation);r.setClinicId(clinic);assertEquals(409,assertThrows(EntryException.class,()->sheets.createCaseSheet(r)).status);}
 @Test void auditRollback(){assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(tx->{change(ConsultationStatus.CANCELLED);throw new IllegalStateException("rollback");}));assertEquals(0,auditCount());assertEquals(ConsultationStatus.STARTED,consultations.getConsultation(consultation,clinic).getStatus());assertNull(consultations.getConsultation(consultation,clinic).getEndedAt());}
 @Test void auditInsertFailureRollsBack(){jdbc.execute("create function reject_lifecycle_test() returns trigger language plpgsql as $$ begin raise exception 'test'; end $$");jdbc.execute("create trigger reject_lifecycle_test before insert on consultation_status_audit for each row execute function reject_lifecycle_test()");try{assertThrows(RuntimeException.class,()->change(ConsultationStatus.CANCELLED));assertEquals(ConsultationStatus.STARTED,consultations.getConsultation(consultation,clinic).getStatus());assertEquals(0,auditCount());}finally{jdbc.execute("drop trigger reject_lifecycle_test on consultation_status_audit");jdbc.execute("drop function reject_lifecycle_test()");}}
 @Test void appendOnly(){change(ConsultationStatus.CANCELLED);assertThrows(RuntimeException.class,()->jdbc.update("update consultation_status_audit set to_status='COMPLETED' where consultation_id=?",consultation));assertThrows(RuntimeException.class,()->jdbc.update("delete from consultation_status_audit where consultation_id=?",consultation));assertThrows(RuntimeException.class,()->jdbc.execute("truncate consultation_status_audit"));assertThrows(RuntimeException.class,()->jdbc.update("delete from consultation where id=?",consultation));}
 @Test void permissions(){login("CONSULTATION_VIEW");assertThrows(AccessDeniedException.class,()->controller.updateStatus(consultation,status(ConsultationStatus.CANCELLED)));assertThrows(AccessDeniedException.class,()->sheetController.updateCaseSheet(parent,clinic,text("No"),"\"0\""));login("CASE_SHEET_EDIT");assertDoesNotThrow(()->sheetController.updateCaseSheet(parent,clinic,text("Allowed"),"\"0\""));login("CONSULTATION_MANAGE");assertDoesNotThrow(()->controller.updateStatus(consultation,status(ConsultationStatus.CANCELLED)));}
 @Test void foreignClinic(){UUID foreign=UUID.randomUUID();jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",foreign,foreign.toString(),"Other");actor=AppUser.builder().id(user).clinic(Clinic.builder().id(foreign).active(true).build()).build();login();assertEquals(404,assertThrows(EntryException.class,()->change(ConsultationStatus.CANCELLED)).status);assertThrows(IllegalArgumentException.class,()->write("No","\"0\""));assertEquals(0,auditCount());}
 void race(Runnable first,Runnable second,int result)throws Exception{
  var held=new CountDownLatch(1);var release=new CountDownLatch(1);var entered=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try{var a=pool.submit(()->{login();try{new TransactionTemplate(transactions).executeWithoutResult(tx->{first.run();held.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}});}finally{clear();}});
   assertTrue(held.await(10,TimeUnit.SECONDS));var b=pool.submit(()->{login();entered.countDown();try{second.run();return 200;}catch(EntryException e){return e.status;}finally{clear();}});
   assertTrue(entered.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->b.get(200,TimeUnit.MILLISECONDS));release.countDown();a.get(10,TimeUnit.SECONDS);assertEquals(result,b.get(10,TimeUnit.SECONDS));
  }finally{release.countDown();pool.shutdownNow();}
 }
 @Test void concurrentProgression()throws Exception{race(()->change(ConsultationStatus.IN_PROGRESS),()->change(ConsultationStatus.IN_PROGRESS),409);assertEquals(1,auditCount());}
 @Test void concurrentText()throws Exception{race(()->write("Winner","\"0\""),()->write("Loser","\"0\""),412);assertEquals("Winner",sheets.getCaseSheet(parent,clinic).getChiefComplaint());}
 @Test void cancellationBeforeFinalization()throws Exception{diagnosis();race(()->change(ConsultationStatus.CANCELLED),this::finalizeSheet,409);}
 @Test void finalizationBeforeCancellation()throws Exception{diagnosis();race(this::finalizeSheet,()->change(ConsultationStatus.CANCELLED),200);assertEquals("FINALIZED",sheets.getCaseSheet(parent,clinic).getStatus().name());}
 @Test void cancellationBeforeTooth()throws Exception{UUID id=chart();race(()->change(ConsultationStatus.CANCELLED),()->tooth(id),409);assertEquals(0,jdbc.queryForObject("select count(*) from odontogram_tooth where odontogram_id=?",Integer.class,id));}
 @Test void toothBeforeCancellation()throws Exception{UUID id=chart();race(()->tooth(id),()->change(ConsultationStatus.CANCELLED),200);assertEquals(1,jdbc.queryForObject("select count(*) from odontogram_tooth where odontogram_id=?",Integer.class,id));}

 @Autowired CaseSheetEntryService entries;
 String token(){return "\""+sheets.getCaseSheet(parent,clinic).getVersion()+"\"";}
 void finalizeVersion(String v){var r=new FinalizeCaseSheetRequest();r.setClinicId(clinic);sheets.finalizeCaseSheet(parent,r,v);}
 void stale(String v){assertEquals(412,assertThrows(EntryException.class,()->finalizeVersion(v)).status);assertEquals("DRAFT",sheets.getCaseSheet(parent,clinic).getStatus().name());}
 CreateCaseSheetClinicalFindingRequest finding(){var r=new CreateCaseSheetClinicalFindingRequest();r.setFindingType("Tenderness");return r;}
 UpdateDiagnosisRequest diagnosisEdit(){var r=new UpdateDiagnosisRequest();r.setDiagnosisName("Updated caries");r.setDiagnosisType(com.dentalclinic.casesheet.entity.DiagnosisType.PRIMARY);return r;}
 @Test void finalizationPreconditions(){diagnosis();assertEquals(428,assertThrows(EntryException.class,()->finalizeVersion(null)).status);for(String v:List.of("1","*","W/\"1\"","\"-1\""))assertEquals(400,assertThrows(EntryException.class,()->finalizeVersion(v)).status);stale("\"0\"");finalizeVersion(token());assertEquals("FINALIZED",sheets.getCaseSheet(parent,clinic).getStatus().name());}
 @Test void diagnosisMutationsInvalidateReview(){String v=token();diagnosis();stale(v);var d=sheets.getDiagnoses(parent,clinic).get(0);v=token();entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",diagnosisEdit());stale(v);v=token();entries.remove(parent,d.getId(),clinic,"\"1\"","Correction",true);stale(v);}
 @Test void findingMutationsInvalidateReview(){diagnosis();String v=token();var f=sheets.addClinicalFinding(parent,clinic,finding());stale(v);v=token();entries.updateFinding(parent,f.getId(),clinic,"\"0\"",finding());stale(v);v=token();var r=new RemoveClinicalEntryRequest();r.setReason("Correction");entries.removeJson(parent,f.getId(),clinic,"\"1\"",r,false);stale(v);}
 @Test void aggregateRollback(){diagnosis();String v=token();assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(tx->{sheets.addClinicalFinding(parent,clinic,finding());throw new IllegalStateException();}));assertEquals(v,token());assertTrue(sheets.getClinicalFindings(parent,clinic).isEmpty());assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(tx->{finalizeVersion(v);throw new IllegalStateException();}));assertEquals(v,token());assertEquals("DRAFT",sheets.getCaseSheet(parent,clinic).getStatus().name());}
 @Test void finalizationPermissions(){diagnosis();var r=new FinalizeCaseSheetRequest();r.setClinicId(clinic);login("CASE_SHEET_EDIT");assertThrows(AccessDeniedException.class,()->sheetController.finalizeCaseSheet(parent,r,token()));login("CASE_SHEET_FINALIZE");assertDoesNotThrow(()->sheetController.finalizeCaseSheet(parent,r,token()));}
 @Test void textBeforeFinalizationIsStale()throws Exception{diagnosis();String v=token();race(()->write("Changed",v),()->finalizeVersion(v),412);assertEquals("DRAFT",sheets.getCaseSheet(parent,clinic).getStatus().name());}
 @Test void diagnosisAddBeforeFinalizationIsStale()throws Exception{diagnosis();String v=token();race(this::diagnosis,()->finalizeVersion(v),412);}
 @Test void diagnosisEditBeforeFinalizationIsStale()throws Exception{diagnosis();var d=sheets.getDiagnoses(parent,clinic).get(0);String v=token();race(()->entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",diagnosisEdit()),()->finalizeVersion(v),412);}
 @Test void diagnosisRemoveBeforeFinalizationIsStale()throws Exception{diagnosis();var d=sheets.getDiagnoses(parent,clinic).get(0);String v=token();race(()->entries.remove(parent,d.getId(),clinic,"\"0\"","Correction",true),()->finalizeVersion(v),412);}
 @Test void findingBeforeFinalizationIsStale()throws Exception{diagnosis();String v=token();race(()->sheets.addClinicalFinding(parent,clinic,finding()),()->finalizeVersion(v),412);}


 @Test void finalizationWrongClinic(){diagnosis();String v=token();UUID other=UUID.randomUUID();jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",other,other.toString(),"Other clinic");actor=AppUser.builder().id(user).clinic(Clinic.builder().id(other).active(true).build()).build();login();assertThrows(IllegalArgumentException.class,()->finalizeVersion(v));assertEquals("DRAFT",jdbc.queryForObject("select status from case_sheet where id=?",String.class,parent));}
 @Test void patientChartDoesNotInvalidateSheetReview(){diagnosis();String v=token();UUID id=chart();tooth(id);assertEquals(v,token());}

}
