package com.dentalclinic.odontogram;
import com.dentalclinic.odontogram.service.*;
import com.dentalclinic.odontogram.dto.*;
import com.dentalclinic.odontogram.controller.*;
import com.dentalclinic.casesheet.service.CaseSheetService;
import com.dentalclinic.casesheet.dto.*;
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
@EnabledIfEnvironmentVariable(named="ODONTOGRAM_TEST_DATABASE",matches="true")
class OdontogramDatabaseTest {
 @DynamicPropertySource static void database(DynamicPropertyRegistry registry){String url=System.getenv("SPRING_DATASOURCE_URL");if(url==null||!url.matches("jdbc:postgresql://localhost:5432/dental_clinic_v21_verification_[a-z0-9_]+"))throw new IllegalStateException("Isolated odontogram verification database required");registry.add("spring.datasource.url",()->url);}
 @Autowired JdbcTemplate jdbc; @Autowired OdontogramService service; @Autowired CaseSheetService sheets;
 @Autowired OdontogramToothController controller; @Autowired PlatformTransactionManager transactions;
 @Autowired org.flywaydb.core.Flyway flyway;
 UUID clinic,user,patient,doctor,appointment,consultation,parent; AppUser actor;
 @BeforeEach void fixture(){assertTrue(jdbc.queryForObject("select current_database()",String.class).startsWith("dental_clinic_v21_verification_"));clinic=UUID.randomUUID();user=UUID.randomUUID();patient=UUID.randomUUID();doctor=UUID.randomUUID();appointment=UUID.randomUUID();consultation=UUID.randomUUID();parent=UUID.randomUUID();
 jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",clinic,clinic.toString(),"Test clinic");
 jdbc.update("insert into app_user(id,clinic_id,first_name) values (?,?,?)",user,clinic,"Test actor");
 jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Patient','OTHER','123')",patient,clinic,patient.toString());
 jdbc.update("insert into doctor_profile(id,clinic_id,user_id) values (?,?,?)",doctor,clinic,user);
 jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'09:00','09:30')",appointment,clinic,appointment.toString(),patient,doctor);
 jdbc.update("insert into consultation(id,clinic_id,appointment_id,patient_id,doctor_id) values (?,?,?,?,?)",consultation,clinic,appointment,patient,doctor);
 jdbc.update("insert into case_sheet(id,clinic_id,consultation_id,appointment_id,patient_id,doctor_id,case_sheet_number,chief_complaint,treatment_plan_requirement) values (?,?,?,?,?,?,?,'Pain','NOT_REQUIRED')",parent,clinic,consultation,appointment,patient,doctor,parent.toString());
 actor=AppUser.builder().id(user).clinic(Clinic.builder().id(clinic).active(true).build()).build();login();}
 void login(String... permissions){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 UpdateOdontogramToothRequest request(String notes){var r=new UpdateOdontogramToothRequest();r.setClinicId(clinic);r.setCaseSheetId(parent);r.setNotes(notes);return r;}
 UUID chart(){return service.createOdontogram(patient,clinic).getId();}
 OdontogramToothResponse create(UUID chart){return service.updateTooth(chart,"46",request("Initial"),null,"*");}
 int count(UUID chart){return jdbc.queryForObject("select count(*) from odontogram_tooth_history where odontogram_id=?",Integer.class,chart);}
 void diagnosis(){var r=new CreateCaseSheetDiagnosisRequest();r.setDiagnosisName("Caries");sheets.addDiagnosis(parent,clinic,r);}
 void finalizeSheet(){var r=new FinalizeCaseSheetRequest();r.setClinicId(clinic);sheets.finalizeCaseSheet(parent,r,"\""+sheets.getCaseSheet(parent,clinic).getVersion()+"\"");}
 @Test void migrationAndVersions(){flyway.validate();var c=chart();var first=create(c);assertEquals(0,first.getVersion());var next=service.updateTooth(c,"46",request("Changed"),"\"0\"",null);assertEquals(1,next.getVersion());assertEquals(1,service.getTeeth(c,clinic).get(0).getVersion());assertEquals(2,count(c));}
 @Test void missingMalformedStaleAndWrongExistence(){var c=chart();create(c);for(String match:new String[]{null,"0","\"9\""}){var e=assertThrows(OdontogramMutationException.class,()->service.updateTooth(c,"46",request("Changed"),match,null));assertEquals(match==null?428:match.equals("0")?400:412,e.status);}assertThrows(OdontogramMutationException.class,()->create(c));assertThrows(OdontogramMutationException.class,()->service.updateTooth(c,"47",request("Changed"),"\"0\"",null));assertEquals(1,count(c));}
 @Test void allAdultNumbersAndInvalidInputs(){var c=chart();for(int q=1;q<=4;q++)for(int n=1;n<=8;n++)service.updateTooth(c,""+(q*10+n),request("Observed"),null,"*");assertEquals(32,count(c));for(String n:List.of("51","0","19","A"))assertEquals(400,assertThrows(OdontogramMutationException.class,()->service.updateTooth(c,n,request("Bad"),null,"*")).status);}
 @Test void historyProtectedAndParentsRetained(){var c=chart();var tooth=create(c);assertThrows(Exception.class,()->jdbc.update("update odontogram_tooth_history set change_reason='tamper' where odontogram_id=?",c));assertThrows(Exception.class,()->jdbc.update("delete from odontogram_tooth_history where odontogram_id=?",c));assertThrows(Exception.class,()->jdbc.update("delete from odontogram_tooth where id=?",tooth.getId()));assertThrows(Exception.class,()->jdbc.update("delete from odontogram where id=?",c));assertEquals(1,count(c));}
 @Test void historyFailureRollsBackTooth(){var c=chart();create(c);jdbc.execute("create function reject_test_history() returns trigger language plpgsql as $$ begin raise exception 'test rejection'; end $$");jdbc.execute("create trigger reject_test_history before insert on odontogram_tooth_history for each row execute function reject_test_history()");try{assertThrows(Exception.class,()->service.updateTooth(c,"46",request("Must rollback"),"\"0\"",null));assertEquals("Initial",service.getTeeth(c,clinic).get(0).getNotes());assertEquals(0,service.getTeeth(c,clinic).get(0).getVersion());assertEquals(1,count(c));}finally{jdbc.execute("drop trigger reject_test_history on odontogram_tooth_history");jdbc.execute("drop function reject_test_history()");}}
 @Test void clinicAndPatientAndLifecycleChecks(){var c=chart();UUID other=UUID.randomUUID();jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Other','OTHER','123')",other,clinic,other.toString());var otherChart=service.createOdontogram(other,clinic).getId();assertThrows(IllegalArgumentException.class,()->create(otherChart));jdbc.update("update odontogram set status='ARCHIVED' where id=?",c);assertThrows(IllegalArgumentException.class,()->create(c));assertNotEquals(c,service.createOdontogram(patient,clinic).getId());actor=AppUser.builder().id(user).clinic(Clinic.builder().id(UUID.randomUUID()).active(true).build()).build();login();assertThrows(IllegalArgumentException.class,()->create(c));}
 @Test void inactiveCondition(){var c=chart();UUID condition=UUID.randomUUID();jdbc.update("insert into odontogram_condition(id,clinic_id,condition_code,condition_name,is_active) values (?,?,'TEST','Test',false)",condition,clinic);var r=request("note");r.setConditionId(condition);assertThrows(IllegalArgumentException.class,()->service.updateTooth(c,"46",r,null,"*"));assertEquals(0,count(c));}
 @Test void runtimePermissionAndAttenderGrant(){var c=chart();login("ODONTOGRAM_VIEW");assertThrows(AccessDeniedException.class,()->controller.updateTooth(c,"46",request("Denied"),null,"*"));assertEquals(1,jdbc.queryForObject("select count(*) from role_permission rp join role r on r.id=rp.role_id join permission p on p.id=rp.permission_id where r.role_code='ATTENDER' and p.permission_code='ODONTOGRAM_EDIT'",Integer.class));login("ODONTOGRAM_EDIT");assertEquals(200,controller.updateTooth(c,"46",request("Allowed"),null,"*").getStatusCode().value());}
 List<Integer> race(Callable<?> action) throws Exception {var pool=Executors.newFixedThreadPool(2);var gate=new CyclicBarrier(2);try{Callable<Integer> worker=()->{login();try{gate.await(5,TimeUnit.SECONDS);action.call();return 200;}catch(OdontogramMutationException e){return e.status;}finally{SecurityContextHolder.clearContext();}};var a=pool.submit(worker);var b=pool.submit(worker);return List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));}finally{pool.shutdownNow();}}
 @Test void concurrentExistingUpdates() throws Exception {var c=chart();create(c);var results=race(()->service.updateTooth(c,"46",request(UUID.randomUUID().toString()),"\"0\"",null));assertTrue(results.contains(200));assertTrue(results.contains(412));assertEquals(2,count(c));assertEquals(1,service.getTeeth(c,clinic).get(0).getVersion());}
 @Test void concurrentFirstTooth() throws Exception {var c=chart();var results=race(()->create(c));assertTrue(results.contains(200));assertTrue(results.contains(412));assertEquals(1,count(c));}
 @Test void concurrentChart() throws Exception {var results=race(()->chart());assertTrue(results.contains(200));assertTrue(results.contains(409));assertEquals(1,jdbc.queryForObject("select count(*) from odontogram where patient_id=?",Integer.class,patient));}
 void orderedRace(boolean finalizationFirst) throws Exception {var c=chart();create(c);diagnosis();var held=new CountDownLatch(1);var release=new CountDownLatch(1);var started=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var first=pool.submit(()->{login();try{return new TransactionTemplate(transactions).execute(s->{if(finalizationFirst)finalizeSheet();else service.updateTooth(c,"46",request("Before finalization"),"\"0\"",null);held.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return true;});}finally{SecurityContextHolder.clearContext();}});assertTrue(held.await(10,TimeUnit.SECONDS));var second=pool.submit(()->{login();started.countDown();try{if(finalizationFirst){assertThrows(IllegalArgumentException.class,()->service.updateTooth(c,"46",request("Too late"),"\"0\"",null));}else finalizeSheet();return true;}finally{SecurityContextHolder.clearContext();}});assertTrue(started.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->second.get(200,TimeUnit.MILLISECONDS));release.countDown();assertTrue(first.get(10,TimeUnit.SECONDS));assertTrue(second.get(10,TimeUnit.SECONDS));assertEquals(finalizationFirst?1:2,count(c));}finally{release.countDown();pool.shutdownNow();}}
 @Test void finalizationWins() throws Exception {orderedRace(true);}
 @Test void toothWins() throws Exception {orderedRace(false);}
 @Autowired javax.sql.DataSource dataSource;
 @Test void legacyRowsMigrateWithoutRenumberingOrHistoryLoss() throws Exception {
  String schema="legacy_"+UUID.randomUUID().toString().replace("-","");
  var old=org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("20").load();old.migrate();
  UUID c=UUID.randomUUID(),u=UUID.randomUUID(),p=UUID.randomUUID(),o=UUID.randomUUID(),t=UUID.randomUUID(),h=UUID.randomUUID();
  try(var connection=dataSource.getConnection()){
   connection.setSchema(schema);var db=new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true));
   db.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,'Legacy')",c,c.toString());
   db.update("insert into app_user(id,clinic_id,first_name) values (?,?,'Legacy')",u,c);
   db.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Legacy','OTHER','123')",p,c,p.toString());
   db.update("insert into odontogram(id,clinic_id,patient_id) values (?,?,?)",o,c,p);
   db.update("insert into odontogram_tooth(id,clinic_id,odontogram_id,tooth_number,notes) values (?,?,?,'legacy-51','Preserve')",t,c,o);
   db.update("insert into odontogram_tooth_history(id,clinic_id,odontogram_id,odontogram_tooth_id,tooth_number,changed_by,new_notes) values (?,?,?,?,'legacy-51',?,'Preserve')",h,c,o,t,u);
   var latest=org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load();latest.migrate();latest.validate();
   assertEquals(0,db.queryForObject("select version from odontogram_tooth where id=?",Long.class,t));
   assertEquals("legacy-51",db.queryForObject("select tooth_number from odontogram_tooth where id=?",String.class,t));
   assertEquals("Preserve",db.queryForObject("select new_notes from odontogram_tooth_history where id=?",String.class,h));
   assertThrows(Exception.class,()->db.update("delete from odontogram_tooth_history where id=?",h));
   connection.setSchema("public");
  }
 }
 @Test void equalHistoryTimestampsUseIdTieBreaker(){var c=chart();create(c);service.updateTooth(c,"46",request("Second"),"\"0\"",null);String prefix=UUID.randomUUID().toString().substring(0,24);UUID a=UUID.fromString(prefix+"000000000001"),b=UUID.fromString(prefix+"000000000002");for(UUID h:List.of(a,b))jdbc.update("insert into odontogram_tooth_history(id,clinic_id,odontogram_id,odontogram_tooth_id,tooth_number,changed_by,changed_at) values (?,?,?,?, '46',?,'2099-01-01')",h,clinic,c,service.getTeeth(c,clinic).get(0).getId(),user);var history=service.getToothHistory(c,"46",clinic);assertEquals(b,history.get(0).getId());assertEquals(a,history.get(1).getId());} UUID anotherSheet(){UUID a=UUID.randomUUID(),c=UUID.randomUUID(),s=UUID.randomUUID();jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'10:00','10:30')",a,clinic,a.toString(),patient,doctor);jdbc.update("insert into consultation(id,clinic_id,appointment_id,patient_id,doctor_id) values (?,?,?,?,?)",c,clinic,a,patient,doctor);jdbc.update("insert into case_sheet(id,clinic_id,consultation_id,appointment_id,patient_id,doctor_id,case_sheet_number,chief_complaint,treatment_plan_requirement) values (?,?,?,?,?,?,?,'Pain','NOT_REQUIRED')",s,clinic,c,a,patient,doctor,s.toString());return s;}
 @Test void chartLockSerializesDifferentEncounters() throws Exception {var c=chart();UUID second=anotherSheet();var sequence=new java.util.concurrent.atomic.AtomicInteger();var results=race(()->{var r=request("Observation");r.setCaseSheetId(sequence.getAndIncrement()==0?parent:second);return service.updateTooth(c,"46",r,null,"*");});assertTrue(results.contains(200));assertTrue(results.contains(412));assertEquals(1,count(c));}
 @Test void existingUpdatesFromDifferentEncountersRejectStaleVersion() throws Exception {var c=chart();create(c);UUID second=anotherSheet();var sequence=new java.util.concurrent.atomic.AtomicInteger();var results=race(()->{var r=request(UUID.randomUUID().toString());r.setCaseSheetId(sequence.getAndIncrement()==0?parent:second);return service.updateTooth(c,"46",r,"\"0\"",null);});assertTrue(results.contains(200));assertTrue(results.contains(412));assertEquals(2,count(c));}
 @Autowired org.springframework.web.context.WebApplicationContext web;
 @Test void httpPreconditionContract() throws Exception {var c=chart();login("ODONTOGRAM_EDIT");var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();String path="/api/odontograms/"+c+"/teeth/46",body="{\"clinicId\":\""+clinic+"\",\"caseSheetId\":\""+parent+"\",\"notes\":\"Initial\"}";

 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(428));
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).header("If-Match","0").contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).header("If-None-Match","*").contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.version").value(0));
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).header("If-None-Match","*").contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(412));
 mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path).header("If-Match","\"0\"").contentType("application/json").content(body.replace("Initial","Updated"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.version").value(1));
 }
 @Test void foreignConditionAndCaseSheetAreRejected(){var c=chart();UUID foreign=UUID.randomUUID();jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,'Foreign')",foreign,foreign.toString());UUID condition=UUID.randomUUID();jdbc.update("insert into odontogram_condition(id,clinic_id,condition_code,condition_name) values (?,?,'TEST','Foreign')",condition,foreign);var r=request("note");r.setConditionId(condition);assertThrows(IllegalArgumentException.class,()->service.updateTooth(c,"46",r,null,"*"));jdbc.update("update case_sheet set clinic_id=? where id=?",foreign,parent);assertThrows(IllegalArgumentException.class,()->create(c));assertEquals(0,count(c));}
 @Test void truncateProtected(){assertThrows(Exception.class,()->jdbc.execute("truncate odontogram_tooth_history"));}}