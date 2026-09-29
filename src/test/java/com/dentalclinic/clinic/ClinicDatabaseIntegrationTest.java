package com.dentalclinic.clinic;
import com.dentalclinic.clinic.dto.*;
import com.dentalclinic.clinic.service.*;
import com.dentalclinic.user.entity.AppUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@EnabledIfEnvironmentVariable(named="CLINIC_TEST_DATABASE", matches="true")
class ClinicDatabaseIntegrationTest {
 @Autowired ClinicService service; @Autowired JdbcTemplate jdbc;
 @BeforeEach void guard(){assertTrue(jdbc.queryForObject("select current_database()",String.class).startsWith("dental_clinic_v17_verification_"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(AppUser.builder().build(),null,List.of()));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 CreateClinicRequest request(String name){var r=new CreateClinicRequest();r.setClinicCode("TEST_"+UUID.randomUUID());r.setClinicName(name);return r;}
 @Test void defaultsAndSearchFilteringAndPaginationAndCaseSensitivity(){
  var r=request("Search_"+UUID.randomUUID());var created=service.create(r);assertTrue(created.getActive());assertEquals("India",created.getCountry());assertEquals("Asia/Kolkata",created.getTimezone());assertNotNull(created.getCreatedAt());
  var settings=service.getSettings(created.getId());assertEquals("INR",settings.getCurrency());assertEquals("DD-MM-YYYY",settings.getDateFormat());assertEquals("12_HOUR",settings.getTimeFormat());assertEquals(30,settings.getAppointmentSlotMinutes());assertTrue(settings.getAllowWalkIn());assertFalse(settings.getEnableWhatsapp());assertFalse(settings.getEnableSms());assertFalse(settings.getEnableEmail());assertFalse(settings.getEnableAiAssistant());assertNull(settings.getSettingsJson());
  var page=service.list(r.getClinicName(),true,-1,999);assertEquals(1,page.getTotalElements());assertEquals(100,page.getSize());assertEquals(0,page.getNumber());assertEquals(1,service.list(r.getClinicCode(),true,0,0).getSize());
  var status=new UpdateClinicStatusRequest();status.setActive(false);service.updateStatus(created.getId(),status);assertEquals(0,service.list(r.getClinicName(),true,0,20).getTotalElements());assertEquals(1,service.list(r.getClinicName(),false,0,20).getTotalElements());
  assertThrows(IllegalArgumentException.class,()->service.create(r));r.setClinicCode(r.getClinicCode().toLowerCase(java.util.Locale.ROOT));assertNotNull(service.create(r));
 }
 @Test void defaultSettingsFailureRollsBackClinic(){
  String code="ROLLBACK_"+UUID.randomUUID();
  jdbc.execute("CREATE FUNCTION reject_clinic_settings_test() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test-only settings failure'; END $$");
  jdbc.execute("CREATE TRIGGER reject_clinic_settings_test BEFORE INSERT ON clinic_settings FOR EACH ROW EXECUTE FUNCTION reject_clinic_settings_test()");
  try{var r=request("Rollback");r.setClinicCode(code);assertThrows(RuntimeException.class,()->service.create(r));assertEquals(0,jdbc.queryForObject("select count(*) from clinic where clinic_code=?",Integer.class,code));}
  finally{jdbc.execute("DROP TRIGGER reject_clinic_settings_test ON clinic_settings");jdbc.execute("DROP FUNCTION reject_clinic_settings_test()");}
 }
 @Test void legacySettingsGetIsReadOnlyPutCreatesAndReplacesJson(){
  var c=service.create(request("Legacy"));jdbc.update("delete from clinic_settings where clinic_id=?",c.getId());
  assertThrows(IllegalArgumentException.class,()->service.getSettings(c.getId()));assertEquals(0,jdbc.queryForObject("select count(*) from clinic_settings where clinic_id=?",Integer.class,c.getId()));
  var r=new UpdateClinicSettingsRequest();r.setCurrency("INR");r.setDateFormat("DD-MM-YYYY");r.setTimeFormat("24_HOUR");r.setAppointmentSlotMinutes(15);r.setAllowWalkIn(true);r.setEnableWhatsapp(false);r.setEnableSms(false);r.setEnableEmail(false);r.setEnableAiAssistant(false);r.setSettingsJson(Map.of("test",1));
  assertNotNull(service.updateSettings(c.getId(),r).getId());r.setSettingsJson(Map.of("replacement",2));assertEquals(Set.of("replacement"),service.updateSettings(c.getId(),r).getSettingsJson().keySet());r.setSettingsJson(null);assertNull(service.updateSettings(c.getId(),r).getSettingsJson());
 }
 @Test void migrationHasExactlyFourGlobalSuperAdminGrants(){
  assertEquals(52,jdbc.queryForObject("select count(*) from permission",Integer.class));
  String codes="('CLINIC_VIEW','CLINIC_CREATE','CLINIC_EDIT','CLINIC_STATUS_MANAGE')";
  assertEquals(4,jdbc.queryForObject("select count(*) from role_permission rp join permission p on p.id=rp.permission_id where p.permission_code in "+codes,Integer.class));
  assertEquals(4,jdbc.queryForObject("select count(*) from role_permission rp join permission p on p.id=rp.permission_id join role r on r.id=rp.role_id where p.permission_code in "+codes+" and r.role_code='SUPER_ADMIN' and r.clinic_id is null and r.is_system_role=true",Integer.class));
 }
}
