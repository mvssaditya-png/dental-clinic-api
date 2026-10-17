package com.dentalclinic.casesheet;
import com.dentalclinic.casesheet.service.*;
import com.dentalclinic.casesheet.dto.*;
import com.dentalclinic.casesheet.entity.*;
import com.dentalclinic.casesheet.repository.*;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.user.entity.AppUser;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CaseSheetEntryTest {
 final UUID clinicId=UUID.randomUUID(),parentId=UUID.randomUUID(),id=UUID.randomUUID();
 final Clinic clinic=Clinic.builder().id(clinicId).active(true).build();
 final AppUser actor=AppUser.builder().id(UUID.randomUUID()).clinic(clinic).build();
 final CaseSheet parent=CaseSheet.builder().id(parentId).clinic(clinic).status(CaseSheetStatus.DRAFT).build();
 final TenantClinicResolver resolver=mock(TenantClinicResolver.class);
 final CaseSheetRepository sheets=mock(CaseSheetRepository.class);
 final CaseSheetDiagnosisRepository diagnoses=mock(CaseSheetDiagnosisRepository.class);
 final CaseSheetClinicalFindingRepository findings=mock(CaseSheetClinicalFindingRepository.class);
 final EntryHistoryRepository history=mock(EntryHistoryRepository.class);
 final EntryAudit audit=mock(EntryAudit.class);
 final CaseSheetEntryService service=new CaseSheetEntryService(resolver,sheets,diagnoses,findings,history,audit);
 final CaseSheetDiagnosis d=CaseSheetDiagnosis.builder().id(id).clinic(clinic).caseSheet(parent).diagnosisName("Old").diagnosisType(DiagnosisType.PRIMARY).build();
 @BeforeEach void setup(){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));when(resolver.resolve(eq(actor),any(),anyString())).thenReturn(clinic);when(sheets.lockByIdAndClinicId(parentId,clinicId)).thenReturn(Optional.of(parent));when(diagnoses.findByIdAndCaseSheetIdAndClinicId(id,parentId,clinicId)).thenReturn(Optional.of(d));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 UpdateDiagnosisRequest request(){var r=new UpdateDiagnosisRequest();r.setDiagnosisName(" New ");r.setDiagnosisType(DiagnosisType.SECONDARY);r.setToothNumber(" 46 ");return r;}
 @Test void updateNormalizesAndAuditsBeforeAfter(){var result=service.updateDiagnosis(parentId,id,clinicId,"\"0\"",request());assertEquals("New",result.getDiagnosisName());assertEquals("46",result.getToothNumber());assertEquals(DiagnosisType.SECONDARY,result.getDiagnosisType());assertNotNull(result.getUpdatedAt());verify(audit).write(eq(clinic),eq(actor),eq("DIAGNOSIS"),eq("UPDATE"),argThat(s->s.diagnosisName().equals("Old")),argThat(s->s.diagnosisName().equals("New")),isNull());}
 @Test void removeRetainsValuesAndRecordsActorReason(){service.remove(parentId,id,clinicId,"\"0\""," Correction ",true);assertEquals("Old",d.getDiagnosisName());assertNotNull(d.getRemovedAt());assertEquals(actor,d.getRemovedBy());verify(diagnoses,never()).delete(any());verify(audit).write(eq(clinic),eq(actor),eq("DIAGNOSIS"),eq("REMOVE"),any(),any(),eq("Correction"));}
 @Test void staleVersionRejected(){assertEquals(412,assertThrows(EntryException.class,()->service.updateDiagnosis(parentId,id,clinicId,"\"1\"",request())).status);verifyNoInteractions(audit);}
 @Test void repeatedRemovalRejected(){service.remove(parentId,id,clinicId,"\"0\"","Correction",true);assertEquals(409,assertThrows(EntryException.class,()->service.remove(parentId,id,clinicId,"\"0\"","Correction",true)).status);}
 @Test void nonDraftRejected(){for(var status:List.of(CaseSheetStatus.FINALIZED,CaseSheetStatus.AMENDED)){parent.setStatus(status);assertEquals(409,assertThrows(EntryException.class,()->service.updateDiagnosis(parentId,id,clinicId,"\"0\"",request())).status);}verifyNoInteractions(audit);}
 @Test void wrongParentAndClinicLookupAreScoped(){when(diagnoses.findByIdAndCaseSheetIdAndClinicId(id,parentId,clinicId)).thenReturn(Optional.empty());assertEquals(404,assertThrows(EntryException.class,()->service.updateDiagnosis(parentId,id,clinicId,"\"0\"",request())).status);verify(diagnoses).findByIdAndCaseSheetIdAndClinicId(id,parentId,clinicId);verifyNoInteractions(audit);}
 @Test void missingParentRejected(){when(sheets.lockByIdAndClinicId(parentId,clinicId)).thenReturn(Optional.empty());assertEquals(404,assertThrows(EntryException.class,()->service.updateDiagnosis(parentId,id,clinicId,"\"0\"",request())).status);verifyNoInteractions(diagnoses,audit);}
 @Test void preconditions(){assertEquals(428,assertThrows(EntryException.class,()->CaseSheetEntryService.version(null)).status);for(String value:List.of("0","*","W/\"1\"","\"-1\"","\"999999999999999999999\"","\"1\",\"2\""))assertEquals(400,assertThrows(EntryException.class,()->CaseSheetEntryService.version(value)).status);assertEquals(3,CaseSheetEntryService.version("\"3\""));}
 @Test void removalReasonValidation(){for(String value:List.of("","  ","a".repeat(1001),"bad\nvalue"))assertEquals(400,assertThrows(EntryException.class,()->CaseSheetEntryService.reason(value)).status);assertThrows(EntryException.class,()->CaseSheetEntryService.reason(null));}
 @Test void diagnosisValidation(){try(var factory=Validation.buildDefaultValidatorFactory()){var v=factory.getValidator();var r=request();assertTrue(v.validate(r).isEmpty());r.setDiagnosisName(" ");assertFalse(v.validate(r).isEmpty());r=request();r.setDiagnosisType(null);assertFalse(v.validate(r).isEmpty());r=request();r.setDiagnosisName("x".repeat(256));assertFalse(v.validate(r).isEmpty());r=request();r.setDiagnosisCode("x".repeat(51));assertFalse(v.validate(r).isEmpty());r=request();r.setToothNumber("x".repeat(11));assertFalse(v.validate(r).isEmpty());}}
 @Test void findingValidation(){try(var factory=Validation.buildDefaultValidatorFactory()){var v=factory.getValidator();var r=new CreateCaseSheetClinicalFindingRequest();assertFalse(v.validate(r).isEmpty());r.setFindingType("Free text");assertTrue(v.validate(r).isEmpty());r.setFindingType("x".repeat(101));assertFalse(v.validate(r).isEmpty());r.setFindingType("Type");r.setFindingValue("x".repeat(256));assertFalse(v.validate(r).isEmpty());}}
 @Test void findingUpdateAndRemove(){var f=CaseSheetClinicalFinding.builder().id(id).clinic(clinic).caseSheet(parent).findingType("Old").build();when(findings.findByIdAndCaseSheetIdAndClinicId(id,parentId,clinicId)).thenReturn(Optional.of(f));var r=new CreateCaseSheetClinicalFindingRequest();r.setFindingType(" Swelling ");r.setFindingValue("  ");assertEquals("Swelling",service.updateFinding(parentId,id,clinicId,"\"0\"",r).getFindingType());assertNull(f.getFindingValue());service.remove(parentId,id,clinicId,"\"0\"","Correction",false);assertNotNull(f.getRemovedAt());verify(findings,never()).delete(any());}
 @Test void historyPaginationAndScope(){when(sheets.findByIdAndClinicIdWithDetails(parentId,clinicId)).thenReturn(Optional.of(parent));when(history.history(eq(clinicId),eq(parentId.toString()),any())).thenAnswer(i->new org.springframework.data.domain.PageImpl<>(List.of(),i.getArgument(2),0));assertEquals(100,service.history(parentId,clinicId,0,999).size());assertThrows(EntryException.class,()->service.history(parentId,clinicId,-1,20));}
}
