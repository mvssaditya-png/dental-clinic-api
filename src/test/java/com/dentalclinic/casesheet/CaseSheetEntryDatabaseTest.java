package com.dentalclinic.casesheet;
import com.dentalclinic.casesheet.service.*;
import com.dentalclinic.casesheet.controller.CaseSheetEntryController;
import com.dentalclinic.casesheet.dto.*;
import com.dentalclinic.casesheet.entity.*;
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
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named="CASE_SHEET_TEST_DATABASE",matches="true")
class CaseSheetEntryDatabaseTest {
 @Autowired JdbcTemplate jdbc; @Autowired CaseSheetService sheets; @Autowired CaseSheetEntryService entries;
 @Autowired CaseSheetEntryController controller; @Autowired PlatformTransactionManager transactions;
 UUID clinic,user,patient,doctor,appointment,consultation,parent; AppUser actor;
 @BeforeEach void fixture(){assertTrue(jdbc.queryForObject("select current_database()",String.class).startsWith("dental_clinic_v18_verification_"));clinic=UUID.randomUUID();user=UUID.randomUUID();patient=UUID.randomUUID();doctor=UUID.randomUUID();appointment=UUID.randomUUID();consultation=UUID.randomUUID();parent=UUID.randomUUID();
 jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",clinic,clinic.toString(),"Test clinic");
 jdbc.update("insert into app_user(id,clinic_id,first_name) values (?,?,?)",user,clinic,"Test actor");
 jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Patient','OTHER','123')",patient,clinic,patient.toString());
 jdbc.update("insert into doctor_profile(id,clinic_id,user_id) values (?,?,?)",doctor,clinic,user);
 jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'09:00','09:30')",appointment,clinic,appointment.toString(),patient,doctor);
 jdbc.update("insert into consultation(id,clinic_id,appointment_id,patient_id,doctor_id) values (?,?,?,?,?)",consultation,clinic,appointment,patient,doctor);
 jdbc.update("insert into case_sheet(id,clinic_id,consultation_id,appointment_id,patient_id,doctor_id,case_sheet_number,chief_complaint) values (?,?,?,?,?,?,?,'Pain')",parent,clinic,consultation,appointment,patient,doctor,parent.toString());
 actor=AppUser.builder().id(user).clinic(Clinic.builder().id(clinic).active(true).build()).build();login();}
 void login(String... permissions){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 CaseSheetDiagnosisResponse diagnosis(){var r=new CreateCaseSheetDiagnosisRequest();r.setDiagnosisName("Caries");return sheets.addDiagnosis(parent,clinic,r);}
 UpdateDiagnosisRequest update(){var r=new UpdateDiagnosisRequest();r.setDiagnosisName("Corrected");r.setDiagnosisType(DiagnosisType.DIFFERENTIAL);r.setToothNumber("47");return r;}
 FinalizeCaseSheetRequest finalizeRequest(){var r=new FinalizeCaseSheetRequest();r.setClinicId(clinic);return r;}
 @Test void createEditRemoveHistoryAndFinalizationCount(){var d=diagnosis();assertEquals(0,d.getVersion());var updated=entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",update());assertEquals(1,updated.getVersion());entries.remove(parent,d.getId(),clinic,"\"1\"","Entered on wrong tooth",true);assertTrue(sheets.getDiagnoses(parent,clinic).isEmpty());assertEquals(1,jdbc.queryForObject("select count(*) from case_sheet_diagnosis where id=?",Integer.class,d.getId()));assertThrows(IllegalArgumentException.class,()->sheets.finalizeCaseSheet(parent,finalizeRequest()));var h=entries.history(parent,clinic,0,20);assertEquals(3,h.totalElements());var removed=h.content().get(0);assertEquals("REMOVE",removed.operation());assertEquals(user,removed.actorId());assertEquals("Entered on wrong tooth",removed.removalReason());assertEquals(1,removed.before().version());assertEquals(2,removed.after().version());assertEquals("47",removed.after().toothNumber());assertNotNull(removed.after().removedAt());assertEquals("CREATE",h.content().get(2).operation());assertNull(h.content().get(2).before());}
 @Test void findingsCreateEditRemove(){var r=new CreateCaseSheetClinicalFindingRequest();r.setFindingType("Swelling");var f=sheets.addClinicalFinding(parent,clinic,r);r.setFindingType("Tenderness");r.setFindingValue("Present");var updated=entries.updateFinding(parent,f.getId(),clinic,"\"0\"",r);assertEquals(1,updated.getVersion());entries.remove(parent,f.getId(),clinic,"\"1\"","Correction",false);assertTrue(sheets.getClinicalFindings(parent,clinic).isEmpty());assertEquals(3,entries.history(parent,clinic,0,20).totalElements());}
 @Test void auditFailureRollsBackClinicalChange(){var d=diagnosis();jdbc.execute("CREATE FUNCTION reject_entry_audit_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test audit failure'; END $$");jdbc.execute("CREATE TRIGGER reject_entry_audit_test BEFORE INSERT ON audit_log FOR EACH ROW EXECUTE FUNCTION reject_entry_audit_test()");try{assertThrows(RuntimeException.class,()->entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",update()));assertEquals("Caries",sheets.getDiagnoses(parent,clinic).get(0).getDiagnosisName());assertEquals(0,sheets.getDiagnoses(parent,clinic).get(0).getVersion());assertEquals(1,entries.history(parent,clinic,0,20).totalElements());}finally{jdbc.execute("DROP TRIGGER reject_entry_audit_test ON audit_log");jdbc.execute("DROP FUNCTION reject_entry_audit_test()");}}
 @Test void databaseRetentionGuards(){var d=diagnosis();assertThrows(RuntimeException.class,()->jdbc.update("delete from case_sheet_diagnosis where id=?",d.getId()));assertThrows(RuntimeException.class,()->jdbc.update("delete from case_sheet where id=?",parent));assertThrows(RuntimeException.class,()->jdbc.update("update audit_log set description='changed' where entity_id=?",d.getId()));assertThrows(RuntimeException.class,()->jdbc.update("delete from audit_log where entity_id=?",d.getId()));assertThrows(RuntimeException.class,()->jdbc.execute("truncate audit_log"));}
 @Test void actualRoleGrantsAndMethodSecurity(){for(String role:List.of("SUPER_ADMIN","ADMIN","DOCTOR","RECEPTIONIST","ATTENDER")){var permissions=jdbc.queryForList("select p.permission_code from role_permission rp join role r on r.id=rp.role_id join permission p on p.id=rp.permission_id where r.role_code=? and r.clinic_id is null",String.class,role);login(permissions.toArray(String[]::new));if(List.of("RECEPTIONIST","ATTENDER").contains(role))assertThrows(AccessDeniedException.class,()->controller.history(parent,clinic,0,20));else assertDoesNotThrow(()->controller.history(parent,clinic,0,20));}login("CASE_SHEET_VIEW");assertThrows(AccessDeniedException.class,()->controller.updateDiagnosis(parent,UUID.randomUUID(),clinic,"\"0\"",update()));assertThrows(AccessDeniedException.class,()->controller.removeFinding(parent,UUID.randomUUID(),clinic,"\"0\"","Reason"));}
 @Test void clinicIsolationAndWrongParent(){var d=diagnosis();UUID other=UUID.randomUUID();jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",other,other.toString(),"Other");actor=AppUser.builder().id(user).clinic(Clinic.builder().id(other).active(true).build()).build();login();assertEquals(404,assertThrows(EntryException.class,()->entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",update())).status);assertEquals(404,assertThrows(EntryException.class,()->entries.history(parent,clinic,0,20)).status);}
 // Hold the first transaction open after mutation; the second must wait on the parent row.
 void race(Runnable first,Runnable second,int expectedStatus) throws Exception {
 var pool=Executors.newFixedThreadPool(2);var changed=new CountDownLatch(1);var release=new CountDownLatch(1);var entered=new CountDownLatch(1);
 try{var a=pool.submit(()->{login();try{new TransactionTemplate(transactions).executeWithoutResult(tx->{first.run();changed.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("release timeout");}catch(InterruptedException e){throw new RuntimeException(e);}});}finally{SecurityContextHolder.clearContext();}});
 assertTrue(changed.await(10,TimeUnit.SECONDS));var b=pool.submit(()->{login();entered.countDown();try{second.run();return 0;}catch(EntryException e){return e.status;}catch(IllegalArgumentException e){return 400;}finally{SecurityContextHolder.clearContext();}});assertTrue(entered.await(10,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->b.get(200,TimeUnit.MILLISECONDS));release.countDown();a.get(10,TimeUnit.SECONDS);assertEquals(expectedStatus,b.get(10,TimeUnit.SECONDS));
 }finally{release.countDown();pool.shutdownNow();}}
 @Test void concurrentEditsRejectStaleVersion() throws Exception {var d=diagnosis();race(()->entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",update()),()->entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",update()),412);assertEquals(2,entries.history(parent,clinic,0,20).totalElements());}
 @Test void concurrentEditRemovalRejectsStaleVersion() throws Exception {var d=diagnosis();race(()->entries.updateDiagnosis(parent,d.getId(),clinic,"\"0\"",update()),()->entries.remove(parent,d.getId(),clinic,"\"0\"","Correction",true),412);assertEquals(1,sheets.getDiagnoses(parent,clinic).size());}
 @Test void removalBeforeFinalizationBlocksFinalization() throws Exception {var d=diagnosis();race(()->entries.remove(parent,d.getId(),clinic,"\"0\"","Correction",true),()->sheets.finalizeCaseSheet(parent,finalizeRequest()),400);}
 @Test void finalizationBeforeRemovalBlocksRemoval() throws Exception {var d=diagnosis();race(()->sheets.finalizeCaseSheet(parent,finalizeRequest()),()->entries.remove(parent,d.getId(),clinic,"\"0\"","Correction",true),409);assertEquals(CaseSheetStatus.FINALIZED,sheets.getCaseSheet(parent,clinic).getStatus());}

 @Autowired org.springframework.web.context.WebApplicationContext web;
 @Test void httpContractsAndValidation() throws Exception {
 var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();login("CASE_SHEET_EDIT","CASE_SHEET_HISTORY_VIEW");var d=diagnosis();String path="/api/case-sheets/"+parent+"/diagnoses/"+d.getId();
 String body="{\"diagnosisName\":\"Corrected\",\"diagnosisType\":\"PRIMARY\"}";
 var put=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).contentType("application/json").content(body);
 mvc.perform(put).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(428));
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).header("If-Match","\"0\"").contentType("application/json").content("{\"diagnosisName\":\"Test\",\"diagnosisType\":\"INVALID\"}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).header("If-Match","\"0\"").contentType("application/json").content("{\"diagnosisName\":\" \",\"diagnosisType\":\"PRIMARY\"}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).header("If-Match","\"0\"").contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.version").value(1));
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path).header("If-Match","\"1\"")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path).header("If-Match","\"1\"").header("X-Removal-Reason","Correction")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path).header("If-Match","\"1\"").header("X-Removal-Reason","Correction")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
 }
}
