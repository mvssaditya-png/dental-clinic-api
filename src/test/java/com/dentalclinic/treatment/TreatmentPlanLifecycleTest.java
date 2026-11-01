package com.dentalclinic.treatment;
import com.dentalclinic.treatment.service.*;import com.dentalclinic.treatment.entity.*;import com.dentalclinic.treatment.repository.*;import com.dentalclinic.clinic.entity.Clinic;import com.dentalclinic.clinic.service.TenantClinicResolver;import com.dentalclinic.user.entity.AppUser;
import org.junit.jupiter.api.Test;import org.springframework.data.domain.*;import java.util.*;import java.time.LocalDateTime;import static org.mockito.Mockito.*;import static org.junit.jupiter.api.Assertions.*;
class TreatmentPlanLifecycleTest {
 @Test void nullableActorAndName(){
  var clinics=mock(TenantClinicResolver.class);var plans=mock(TreatmentPlanRepository.class);var history=mock(TreatmentPlanStatusHistoryRepository.class);
  var service=new TreatmentPlanLifecycleService(clinics,plans,history);UUID clinic=UUID.randomUUID(),id=UUID.randomUUID();var user=AppUser.builder().id(UUID.randomUUID()).build();
  org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,null,List.of()));
  try{
   when(clinics.resolve(any(),any(),any())).thenReturn(Clinic.builder().id(clinic).build());when(plans.findByIdAndClinicId(id,clinic)).thenReturn(Optional.of(new TreatmentPlan()));
   var event=TreatmentPlanStatusHistory.builder().id(UUID.randomUUID()).toStatus(TreatmentPlanStatus.DRAFT).changedAt(LocalDateTime.now()).build();
   when(history.readLifecycle(clinic,id,PageRequest.of(0,20))).thenReturn(new PageImpl<>(List.of(event)));
   assertNull(service.read(id,clinic,0,20).content().get(0).actorId());event.setChangedBy(user);assertNull(service.read(id,clinic,0,20).content().get(0).actorName());
  }finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 }
}
