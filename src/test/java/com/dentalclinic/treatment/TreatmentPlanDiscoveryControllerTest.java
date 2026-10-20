package com.dentalclinic.treatment;

import com.dentalclinic.treatment.controller.*;
import com.dentalclinic.treatment.service.TreatmentPlanService;
import com.dentalclinic.treatment.dto.*;
import com.dentalclinic.security.authorization.TreatmentPlanAuthorization;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TreatmentPlanDiscoveryControllerTest {
 @Configuration @EnableMethodSecurity static class Config {
  @Bean TreatmentPlanService service(){return mock(TreatmentPlanService.class);}
  @Bean TreatmentPlanDiscoveryController discovery(TreatmentPlanService s){return new TreatmentPlanDiscoveryController(s);}
  @Bean TreatmentPlanController existing(TreatmentPlanService s){return new TreatmentPlanController(s);}
  @Bean TreatmentPlanAuthorization treatmentPlanAuthorization(){return new TreatmentPlanAuthorization();}
 }
 AnnotationConfigApplicationContext context;TreatmentPlanDiscoveryController controller;TreatmentPlanService service;MockMvc mvc;
 UUID id=UUID.randomUUID(),clinic=UUID.randomUUID();
 void login(String... permissions){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user",null,Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));}
 @BeforeEach void setup(){context=new AnnotationConfigApplicationContext(Config.class);controller=context.getBean(TreatmentPlanDiscoveryController.class);service=context.getBean(TreatmentPlanService.class);mvc=MockMvcBuilders.standaloneSetup(controller,context.getBean(TreatmentPlanController.class)).build();}
 @AfterEach void clean(){SecurityContextHolder.clearContext();context.close();}
 @Test void exactPermissionRequiredForBothReads(){for(String role:List.of("ROLE_SUPER_ADMIN","ROLE_DOCTOR","ROLE_RECEPTIONIST","CASE_SHEET_VIEW","TREATMENT_PLAN_EDIT")){login(role);assertThrows(AccessDeniedException.class,()->controller.byPatient(id,clinic,0,20));assertThrows(AccessDeniedException.class,()->controller.byCaseSheet(id,clinic,0,20));}verifyNoInteractions(service);login("TREATMENT_PLAN_VIEW");assertDoesNotThrow(()->controller.byPatient(id,clinic,0,20));assertDoesNotThrow(()->controller.byCaseSheet(id,clinic,0,20));}
 @Test void defaultsAndExplicitPaginationBindForBothRoutes() throws Exception {login("TREATMENT_PLAN_VIEW");var empty=new TreatmentPlanPage(List.of(),0,20,0,0);when(service.discoverByPatient(id,null,0,20)).thenReturn(empty);mvc.perform(get("/api/patients/"+id+"/treatment-plans")).andExpect(status().isOk()).andExpect(jsonPath("content").isEmpty()).andExpect(jsonPath("size").value(20));when(service.discoverByCaseSheet(id,clinic,2,5)).thenReturn(new TreatmentPlanPage(List.of(),2,5,0,0));mvc.perform(get("/api/case-sheets/"+id+"/treatment-plans").param("clinicId",clinic.toString()).param("page","2").param("size","5")).andExpect(status().isOk()).andExpect(jsonPath("number").value(2));verify(service).discoverByCaseSheet(id,clinic,2,5);}
 @Test void compactSummarySerializesReferencesAndNoClinicalNotes() throws Exception {
  login("TREATMENT_PLAN_VIEW");
  var now=java.time.LocalDateTime.of(2026,10,20,10,30);
  var summary=new TreatmentPlanSummary(id,clinic,"TP000001","Restorative care",id,null,null,
      com.dentalclinic.treatment.entity.TreatmentPlanStatus.DRAFT,2,new java.math.BigDecimal("2500.00"),now,now);
  when(service.discoverByPatient(id,null,0,20)).thenReturn(new TreatmentPlanPage(List.of(summary),0,20,1,1));
  mvc.perform(get("/api/patients/"+id+"/treatment-plans")).andExpect(status().isOk())
      .andExpect(jsonPath("content[0].id").value(id.toString()))
      .andExpect(jsonPath("content[0].patientId").value(id.toString()))
      .andExpect(jsonPath("content[0].status").value("DRAFT"))
      .andExpect(jsonPath("content[0].finalAmount").value(2500.00))
      .andExpect(jsonPath("content[0].notes").doesNotExist())
      .andExpect(jsonPath("content[0].patientNotes").doesNotExist())
      .andExpect(jsonPath("content[0].items").doesNotExist());
 }
 @Test void malformedIdsAndPaginationAreBadRequests() throws Exception {login("TREATMENT_PLAN_VIEW");for(String parent:List.of("patients","case-sheets")){mvc.perform(get("/api/"+parent+"/invalid/treatment-plans")).andExpect(status().isBadRequest());mvc.perform(get("/api/"+parent+"/"+id+"/treatment-plans").param("clinicId","invalid")).andExpect(status().isBadRequest());mvc.perform(get("/api/"+parent+"/"+id+"/treatment-plans").param("page","invalid")).andExpect(status().isBadRequest());mvc.perform(get("/api/"+parent+"//treatment-plans")).andExpect(status().isNotFound());}verifyNoInteractions(service);}
 @Test void existingDetailAndItemsRemainRoutedAndPermissionProtected() throws Exception {login("TREATMENT_PLAN_VIEW");when(service.getTreatmentPlan(id,clinic)).thenReturn(TreatmentPlanResponse.builder().id(id).clinicId(clinic).notes("Existing detail remains unchanged").build());when(service.getTreatmentPlanItems(id,clinic)).thenReturn(List.of());mvc.perform(get("/api/treatment-plans/"+id).param("clinicId",clinic.toString())).andExpect(status().isOk()).andExpect(jsonPath("notes").value("Existing detail remains unchanged"));mvc.perform(get("/api/treatment-plans/"+id+"/items").param("clinicId",clinic.toString())).andExpect(status().isOk()).andExpect(content().json("[]"));login("ROLE_SUPER_ADMIN");assertThrows(AccessDeniedException.class,()->context.getBean(TreatmentPlanController.class).getTreatmentPlan(id,clinic));}
}
