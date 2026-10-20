package com.dentalclinic.treatment;

import com.dentalclinic.treatment.service.TreatmentPlanService;
import com.dentalclinic.treatment.repository.TreatmentPlanRepository;
import com.dentalclinic.treatment.dto.*;
import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import com.dentalclinic.patient.repository.PatientRepository;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.casesheet.repository.CaseSheetRepository;
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.clinic.service.*;
import com.dentalclinic.user.entity.AppUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanDiscoveryTest {
 @Mock TreatmentPlanRepository plans;
 @Mock PatientRepository patients;
 @Mock CaseSheetRepository sheets;
 @Mock ClinicRepository clinics;
 @InjectMocks TreatmentPlanService service;
 final UUID clinicId=UUID.randomUUID(), parent=UUID.randomUUID();
 Clinic clinic(){return Clinic.builder().id(clinicId).active(true).build();}
 void login(Clinic clinic){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(AppUser.builder().clinic(clinic).build(),null,List.of()));}
 @BeforeEach void setup(){ReflectionTestUtils.setField(service,"tenantClinicResolver",new TenantClinicResolver(clinics,new ClinicAccessPolicy()));login(clinic());}
 @AfterEach void clean(){SecurityContextHolder.clearContext();}
 TreatmentPlanSummary summary(){return new TreatmentPlanSummary(UUID.randomUUID(),clinicId,"TP000001",null,parent,null,null,TreatmentPlanStatus.DRAFT,null,BigDecimal.ZERO,LocalDateTime.now(),LocalDateTime.now());}
 @Test void patientMultiplePlansAndPageMetadata(){when(patients.findByIdAndClinicId(parent,clinicId)).thenReturn(Optional.of(new Patient()));var content=List.of(summary(),summary());when(plans.discoverByPatient(parent,clinicId,PageRequest.of(0,2))).thenReturn(new PageImpl<>(content,PageRequest.of(0,2),3));var result=service.discoverByPatient(parent,null,0,2);assertEquals(content,result.content());assertEquals(3,result.totalElements());assertEquals(2,result.totalPages());assertEquals(2,result.size());}
 @Test void emptyPatientIsValid(){when(patients.findByIdAndClinicId(parent,clinicId)).thenReturn(Optional.of(new Patient()));when(plans.discoverByPatient(parent,clinicId,PageRequest.of(0,20))).thenReturn(Page.empty(PageRequest.of(0,20)));assertTrue(service.discoverByPatient(parent,null,0,20).content().isEmpty());}
 @Test void caseSheetPlansAndEmptyPage(){when(sheets.findByIdAndClinicIdWithDetails(parent,clinicId)).thenReturn(Optional.of(new CaseSheet()));when(plans.discoverByCaseSheet(parent,clinicId,PageRequest.of(0,20))).thenReturn(new PageImpl<>(List.of(summary())));assertEquals(1,service.discoverByCaseSheet(parent,null,0,20).content().size());when(plans.discoverByCaseSheet(parent,clinicId,PageRequest.of(1,20))).thenReturn(new PageImpl<>(List.of(),PageRequest.of(1,20),1));assertTrue(service.discoverByCaseSheet(parent,null,1,20).content().isEmpty());}
 @Test void emptyCaseSheet(){when(sheets.findByIdAndClinicIdWithDetails(parent,clinicId)).thenReturn(Optional.of(new CaseSheet()));when(plans.discoverByCaseSheet(parent,clinicId,PageRequest.of(0,20))).thenReturn(Page.empty(PageRequest.of(0,20)));assertEquals(0,service.discoverByCaseSheet(parent,null,0,20).totalElements());}
 @Test void nonexistentOrForeignParentsNeverQueryPlans(){assertThrows(IllegalArgumentException.class,()->service.discoverByPatient(parent,UUID.randomUUID(),0,20));assertThrows(IllegalArgumentException.class,()->service.discoverByCaseSheet(parent,UUID.randomUUID(),0,20));verify(patients).findByIdAndClinicId(parent,clinicId);verify(sheets).findByIdAndClinicIdWithDetails(parent,clinicId);verifyNoInteractions(plans,clinics);}
 @Test void nullParentsRejected(){assertThrows(IllegalArgumentException.class,()->service.discoverByPatient(null,null,0,20));assertThrows(IllegalArgumentException.class,()->service.discoverByCaseSheet(null,null,0,20));verifyNoInteractions(plans,patients,sheets);}
 @Test void platformRequiresContextAndValidActiveClinic(){login(null);assertThrows(IllegalArgumentException.class,()->service.discoverByPatient(parent,null,0,20));assertThrows(IllegalArgumentException.class,()->service.discoverByCaseSheet(parent,clinicId,0,20));when(clinics.findById(clinicId)).thenReturn(Optional.of(Clinic.builder().active(false).build()));assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.discoverByPatient(parent,clinicId,0,20));verifyNoInteractions(plans,patients,sheets);}
 @Test void platformExplicitClinicWorks(){login(null);when(clinics.findById(clinicId)).thenReturn(Optional.of(clinic()));when(patients.findByIdAndClinicId(parent,clinicId)).thenReturn(Optional.of(new Patient()));when(plans.discoverByPatient(parent,clinicId,PageRequest.of(0,20))).thenReturn(Page.empty(PageRequest.of(0,20)));assertEquals(0,service.discoverByPatient(parent,clinicId,0,20).totalElements());}
 @Test void paginationRejectsInvalidAndExcessiveOffset(){for(int[] p:List.of(new int[]{-1,20},new int[]{0,0},new int[]{0,-1},new int[]{Integer.MAX_VALUE,100})){assertThrows(IllegalArgumentException.class,()->service.discoverByPatient(parent,null,p[0],p[1]));assertThrows(IllegalArgumentException.class,()->service.discoverByCaseSheet(parent,null,p[0],p[1]));}verifyNoInteractions(plans,patients,sheets);}
 @Test void maximumSizeIsCapped(){when(patients.findByIdAndClinicId(parent,clinicId)).thenReturn(Optional.of(new Patient()));when(plans.discoverByPatient(parent,clinicId,PageRequest.of(1,100))).thenReturn(Page.empty(PageRequest.of(1,100)));assertEquals(100,service.discoverByPatient(parent,null,1,10000).size());}
 @Test void discoveryQueriesHaveDatabasePaginationStableTiebreakerAndTenantCount() throws Exception {for(String name:List.of("discoverByPatient","discoverByCaseSheet")){var method=TreatmentPlanRepository.class.getMethod(name,UUID.class,UUID.class,Pageable.class);var query=method.getAnnotation(org.springframework.data.jpa.repository.Query.class);assertTrue(query.value().contains("ORDER BY tp.createdAt DESC, tp.id DESC"));assertTrue(query.value().contains("tp.clinic.id = :clinicId"));assertTrue(query.countQuery().contains("tp.clinic.id = :clinicId"));assertTrue(query.countQuery().contains(":parentId"));assertFalse(query.value().contains("FETCH"));}}
}
