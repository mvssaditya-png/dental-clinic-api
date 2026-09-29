package com.dentalclinic.clinic;
import com.dentalclinic.clinic.controller.ClinicController;
import com.dentalclinic.clinic.service.ClinicService;
import com.dentalclinic.clinic.service.ClinicAccessPolicy;
import com.dentalclinic.security.config.SecurityConfig;
import com.dentalclinic.security.filter.JwtAuthenticationFilter;
import com.dentalclinic.security.handler.SecurityErrorHandler;
import com.dentalclinic.security.repository.*;
import com.dentalclinic.auth.service.JwtService;
import com.dentalclinic.user.repository.AppUserRepository;
import com.dentalclinic.common.exception.GlobalExceptionHandler;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.*;
import com.dentalclinic.security.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClinicControllerSecurityTest {
 @Configuration @EnableWebMvc @EnableWebSecurity
 @Import({SecurityConfig.class,JwtAuthenticationFilter.class,SecurityErrorHandler.class,GlobalExceptionHandler.class,ClinicController.class,ClinicAccessPolicy.class,ProbeController.class})
 static class Config {
  @Bean ObjectMapper mapper(){return JsonMapper.builder().build();}
  @Bean JwtService jwt(){return mock(JwtService.class);}
  @Bean AppUserRepository users(){return mock(AppUserRepository.class);}
  @Bean UserRoleRepository roles(){return mock(UserRoleRepository.class);}
  @Bean RolePermissionRepository permissions(){return mock(RolePermissionRepository.class);}
  @Bean ClinicService service(){return mock(ClinicService.class);}
 }
 @org.springframework.web.bind.annotation.RestController
 static class ProbeController {
  @org.springframework.web.bind.annotation.GetMapping({"/api/auth/me","/api/patients"})
  public Map<String,String> probe(){return Map.of("result","ok");}
 }
 AnnotationConfigWebApplicationContext context; MockMvc mvc;
 String id=UUID.randomUUID().toString();
 @BeforeEach void setup(){context=new AnnotationConfigWebApplicationContext();context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test",Map.of("app.jwt.expiration-ms","60000","app.jwt.secret","test-only-signing-key-at-least-thirty-two-characters-long")));context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @AfterEach void close(){context.close();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 MockHttpServletRequestBuilder request(int i){return switch(i){
  case 0 -> get("/api/clinics"); case 1 -> get("/api/clinics/"+id);
  case 2 -> post("/api/clinics").contentType("application/json").content("{\"clinicCode\":\"TEST\",\"clinicName\":\"Test\"}");
  case 3 -> put("/api/clinics/"+id).contentType("application/json").content("{\"clinicName\":\"Test\"}");
  case 4 -> patch("/api/clinics/"+id+"/status").contentType("application/json").content("{\"active\":false}");
  case 5 -> get("/api/clinics/"+id+"/settings");
  default -> put("/api/clinics/"+id+"/settings").contentType("application/json").content("{\"currency\":\"INR\",\"dateFormat\":\"DD-MM-YYYY\",\"timeFormat\":\"12_HOUR\",\"appointmentSlotMinutes\":30,\"allowWalkIn\":true,\"enableWhatsapp\":false,\"enableSms\":false,\"enableEmail\":false,\"enableAiAssistant\":false}");};}
 @Test void everyEndpointRequiresExactAuthorityNotRoleAlone() throws Exception {
  String[] permissions={"CLINIC_VIEW","CLINIC_VIEW","CLINIC_CREATE","CLINIC_EDIT","CLINIC_STATUS_MANAGE","CLINIC_SETTINGS_MANAGE","CLINIC_SETTINGS_MANAGE"};
  for(int i=0;i<permissions.length;i++){
   mvc.perform(request(i)).andExpect(status().isUnauthorized());
   mvc.perform(request(i).with(user("platform").roles("SUPER_ADMIN"))).andExpect(status().isForbidden());
   mvc.perform(request(i).with(user("allowed").authorities(new SimpleGrantedAuthority(permissions[i])))).andExpect(status().is(i==2?201:200));
  }
 }
 @Test void clinicPermissionsDoNotGrantSettingsAndStatusRequiresBoolean() throws Exception {
  mvc.perform(request(5).with(user("platform").authorities(new SimpleGrantedAuthority("CLINIC_EDIT")))).andExpect(status().isForbidden());
  mvc.perform(patch("/api/clinics/"+id+"/status").contentType("application/json").content("{}").with(user("allowed").authorities(new SimpleGrantedAuthority("CLINIC_STATUS_MANAGE")))).andExpect(status().isBadRequest());
 }
 @Test void existingTokenRechecksClinicForMeAndTenantAndWorksAfterReactivation() throws Exception {
  var users=context.getBean(AppUserRepository.class);var jwt=context.getBean(JwtService.class);
  UUID uid=UUID.randomUUID();Clinic clinic=Clinic.builder().active(true).build();
  AppUser account=AppUser.builder().id(uid).clinic(clinic).status(UserStatus.ACTIVE).build();
  when(jwt.isTokenValid("existing")).thenReturn(true);when(jwt.extractSubject("existing")).thenReturn(uid.toString());when(users.findByIdWithClinic(uid)).thenReturn(Optional.of(account));
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer existing")).andExpect(status().isOk());
  clinic.setActive(false);
  for(String path:List.of("/api/auth/me","/api/patients"))mvc.perform(get(path).header("Authorization","Bearer existing")).andExpect(status().isForbidden()).andExpect(jsonPath("status").value(403));
  clinic.setActive(true);mvc.perform(get("/api/auth/me").header("Authorization","Bearer existing")).andExpect(status().isOk());
  account.setClinic(null);mvc.perform(get("/api/auth/me").header("Authorization","Bearer existing")).andExpect(status().isOk());
 }
}
