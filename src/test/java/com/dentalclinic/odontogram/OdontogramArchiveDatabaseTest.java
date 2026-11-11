package com.dentalclinic.odontogram;
import com.dentalclinic.odontogram.service.*;
import com.dentalclinic.odontogram.controller.OdontogramArchiveController;
import com.dentalclinic.casesheet.dto.RemoveClinicalEntryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class OdontogramArchiveDatabaseTest extends OdontogramDatabaseTest {
 @Override void login(String... permissions){
  var values=new java.util.ArrayList<String>(java.util.Arrays.asList(permissions));
  if(values.isEmpty())values.add("ODONTOGRAM_ARCHIVE");
  values.add("ROLE_ADMIN");super.login(values.toArray(String[]::new));
 }
 @Autowired OdontogramArchiveService archive;
 @Autowired OdontogramArchiveController endpoint;
 @Test void archiveAndToothWriteSerializeInEitherOrder() throws Exception {
  for(boolean archiveFirst:new boolean[]{true,false}){
   var c=chart();create(c);var held=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
   try{var first=pool.submit(()->{login();return new TransactionTemplate(transactions).execute(tx->{if(archiveFirst)archive.archive(c,clinic,"\"1\"","first");else service.updateTooth(c,"46",request("edit first"),"\"0\"",null);held.countDown();try{assertTrue(release.await(10,java.util.concurrent.TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}return true;});});assertTrue(held.await(10,java.util.concurrent.TimeUnit.SECONDS));
    var second=pool.submit(()->{login();if(archiveFirst)assertThrows(IllegalArgumentException.class,()->service.updateTooth(c,"46",request("too late"),"\"0\"",null));else assertEquals(412,assertThrows(OdontogramMutationException.class,()->archive.archive(c,clinic,"\"1\"","stale")).status);return true;});
    assertThrows(java.util.concurrent.TimeoutException.class,()->second.get(150,java.util.concurrent.TimeUnit.MILLISECONDS));release.countDown();assertTrue(first.get(10,java.util.concurrent.TimeUnit.SECONDS));assertTrue(second.get(10,java.util.concurrent.TimeUnit.SECONDS));assertEquals(archiveFirst?1:2,count(c));if(!archiveFirst)archive.archive(c,clinic,"\"2\"","cleanup active chart for next scenario");
   }finally{release.countDown();pool.shutdownNow();}
  }
 }
 @Test void auditFailureRollsBackArchive(){var c=chart();jdbc.execute("create function reject_archive_test() returns trigger language plpgsql as $$ begin raise exception 'audit unavailable'; end $$");jdbc.execute("create trigger reject_archive_test before insert on odontogram_archive_audit for each row execute function reject_archive_test()");try{assertThrows(Exception.class,()->archive.archive(c,clinic,"\"0\"","reason"));assertEquals("ACTIVE",archive.read(c,clinic).getStatus().name());assertEquals(0,archive.read(c,clinic).getVersion());}finally{jdbc.execute("drop trigger reject_archive_test on odontogram_archive_audit");jdbc.execute("drop function reject_archive_test()");}}
 @Test void realCrossClinicAndPlatformContext(){var c=chart();var local=actor;actor=com.dentalclinic.user.entity.AppUser.builder().id(user).clinic(null).build();login("ODONTOGRAM_ARCHIVE","ODONTOGRAM_VIEW");assertThrows(IllegalArgumentException.class,()->archive.read(c,null));assertEquals(c,archive.read(c,clinic).getId());UUID other=UUID.randomUUID();jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,'Other')",other,other.toString());assertEquals(404,assertThrows(OdontogramMutationException.class,()->archive.archive(c,other,"\"0\"","wrong clinic")).status);assertEquals(404,assertThrows(OdontogramMutationException.class,()->archive.list(patient,other,0,20)).status);actor=local;login();}
 @Test void archivePreservesHistoryAndAllowsExplicitReplacement(){var c=chart();create(c);archive.archive(c,clinic,"\"1\"","తప్పు chart");assertEquals("ARCHIVED",archive.read(c,clinic).getStatus().name());assertEquals("తప్పు chart",archive.read(c,clinic).getArchiveReason());assertEquals(1,count(c));assertEquals(1,service.getTeeth(c,clinic).size());assertThrows(IllegalArgumentException.class,()->service.getOdontogramByPatient(patient,clinic));assertThrows(IllegalArgumentException.class,()->service.updateTooth(c,"46",request("Bad"),"\"0\"",null));assertNotEquals(c,chart());assertEquals(1,archive.list(patient,clinic,0,20).totalElements());}
 @Test void preconditionsAndReasons(){var c=chart();for(String match:new String[]{null,"0","\"9\""})assertEquals(match==null?428:match.equals("0")?400:412,assertThrows(OdontogramMutationException.class,()->archive.archive(c,clinic,match,"Reason")).status);for(String reason:Arrays.asList(null,"","  ","a".repeat(1001),"a\n"))assertEquals(400,assertThrows(OdontogramMutationException.class,()->archive.archive(c,clinic,"\"0\"",reason)).status);archive.archive(c,clinic,"\"0\"","😀".repeat(1000));assertEquals(409,assertThrows(OdontogramMutationException.class,()->archive.archive(c,clinic,"\"0\"","again")).status);}
 @Test void permissionsAndCrossClinic(){var c=chart();var r=new RemoveClinicalEntryRequest();r.setReason("reason");login("ODONTOGRAM_EDIT","ODONTOGRAM_VIEW");assertThrows(org.springframework.security.access.AccessDeniedException.class,()->endpoint.archive(c,clinic,"\"0\"",r));login("ODONTOGRAM_ARCHIVE");assertThrows(OdontogramMutationException.class,()->endpoint.archive(UUID.randomUUID(),clinic,"\"0\"",r));endpoint.archive(c,clinic,"\"0\"",r);assertEquals(3,jdbc.queryForObject("select count(*) from role_permission rp join permission p on p.id=rp.permission_id where p.permission_code='ODONTOGRAM_ARCHIVE'",Integer.class));}
 @Test void auditAtomicAndAppendOnly(){var c=chart();new TransactionTemplate(transactions).execute(s->{archive.archive(c,clinic,"\"0\"","rollback");s.setRollbackOnly();return null;});assertEquals("ACTIVE",archive.read(c,clinic).getStatus().name());assertEquals(0,jdbc.queryForObject("select count(*) from odontogram_archive_audit where odontogram_id=?",Integer.class,c));archive.archive(c,clinic,"\"0\"","reason");assertThrows(Exception.class,()->jdbc.update("delete from odontogram_archive_audit where odontogram_id=?",c));assertThrows(Exception.class,()->jdbc.update("update odontogram set status='ACTIVE' where id=?",c));}
 @Test void concurrentArchive() throws Exception {var c=chart();var results=race(()->{archive.archive(c,clinic,"\"0\"","reason");return null;});assertTrue(results.contains(200));assertTrue(results.contains(409));assertEquals(1,jdbc.queryForObject("select count(*) from odontogram_archive_audit where odontogram_id=?",Integer.class,c));}
 @Test void toothChangesInvalidateArchiveReview(){var c=chart();create(c);assertEquals(1,archive.read(c,clinic).getVersion());service.updateTooth(c,"46",request("second"),"\"0\"",null);assertEquals(2,archive.read(c,clinic).getVersion());assertEquals(412,assertThrows(OdontogramMutationException.class,()->archive.archive(c,clinic,"\"1\"","reason")).status);assertEquals("ACTIVE",archive.read(c,clinic).getStatus().name());}

 @Test void doctorCanArchiveOnlyOwnChart(){
  var c=chart();super.login("ROLE_DOCTOR","ODONTOGRAM_ARCHIVE");assertTrue(archive.read(c,clinic).isArchiveAllowed());
  archive.archive(c,clinic,"\"0\"","Own chart");assertFalse(archive.read(c,clinic).isArchiveAllowed());
 }
 @Test void doctorCannotArchiveAnotherOrUnknownCreator(){
  for(boolean unknown:new boolean[]{false,true}){
   var c=chart();var other=UUID.randomUUID();jdbc.update("insert into app_user(id,clinic_id,first_name) values (?,?,'Other')",other,clinic);
   jdbc.update("update odontogram set created_by=? where id=?",unknown?null:other,c);
   super.login("ROLE_DOCTOR","ODONTOGRAM_ARCHIVE");assertFalse(archive.read(c,clinic).isArchiveAllowed());
   assertThrows(org.springframework.security.access.AccessDeniedException.class,()->archive.archive(c,clinic,"\"0\"","Denied"));
   assertEquals("ACTIVE",archive.read(c,clinic).getStatus().name());assertEquals(0,archive.read(c,clinic).getVersion());
   assertEquals(0,jdbc.queryForObject("select count(*) from odontogram_archive_audit where odontogram_id=?",Integer.class,c));
   login();archive.archive(c,clinic,"\"0\"","Admin cleanup");
  }
 }
 @Test void administrativeRolesNeedPermissionAndRespectClinicScope(){
  for(String role:List.of("ROLE_ADMIN","ROLE_SUPER_ADMIN")){
   var c=chart();jdbc.update("update odontogram set created_by=null where id=?",c);
   super.login(role);assertFalse(archive.read(c,clinic).isArchiveAllowed());assertThrows(org.springframework.security.access.AccessDeniedException.class,()->archive.archive(c,clinic,"\"0\"","Denied"));
   super.login(role,"ODONTOGRAM_ARCHIVE");assertTrue(archive.read(c,clinic).isArchiveAllowed());archive.archive(c,clinic,"\"0\"","Allowed");
  }
 }
 @Test void nonclinicalRolesDeniedEvenWithAccidentalArchivePermission(){
  var c=chart();for(String role:List.of("ROLE_RECEPTIONIST","ROLE_ATTENDER")){
   super.login(role,"ODONTOGRAM_ARCHIVE");assertFalse(archive.read(c,clinic).isArchiveAllowed());
   assertThrows(org.springframework.security.access.AccessDeniedException.class,()->archive.archive(c,clinic,"\"0\"","Denied"));
  }
 }
 @Test void forgedCreatorCannotOverridePersistedOwnership() throws Exception {
  var c=chart();jdbc.update("update odontogram set created_by=null where id=?",c);super.login("ROLE_DOCTOR","ODONTOGRAM_ARCHIVE");
  var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/odontograms/"+c+"/archive").param("clinicId",clinic.toString()).header("If-Match","\"0\"").contentType("application/json").content("{\"reason\":\"test\",\"createdBy\":\""+user+"\",\"archiveAllowed\":true}"))
   .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
  assertEquals("ACTIVE",archive.read(c,clinic).getStatus().name());
 }
}
