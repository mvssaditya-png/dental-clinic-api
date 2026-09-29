package com.dentalclinic.clinic;
import com.dentalclinic.clinic.dto.*;
import com.dentalclinic.clinic.entity.*;
import com.dentalclinic.clinic.repository.*;
import com.dentalclinic.clinic.service.*;
import com.dentalclinic.user.entity.AppUser;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ClinicServiceTest {
 ClinicRepository clinics = mock(ClinicRepository.class); ClinicSettingsRepository settings = mock(ClinicSettingsRepository.class);
 ClinicService service = new ClinicService(clinics,settings,new ClinicAccessPolicy());
 Clinic clinic = Clinic.builder().id(UUID.randomUUID()).clinicCode("IMMUTABLE").clinicName("Old").active(false).build();
 @BeforeEach void setup() { when(clinics.findById(clinic.getId())).thenReturn(Optional.of(clinic)); when(clinics.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0)); }
 @AfterEach void clear() { SecurityContextHolder.clearContext(); }
 void login(Clinic c) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(AppUser.builder().clinic(c).build(),null,List.of())); }
 @Test void updatePreservesCodeAndStatusAndClearsOptionalFields() {
  clinic.setPhone("123"); var r = new UpdateClinicRequest(); r.setClinicName("New");
  var result=service.update(clinic.getId(),r); assertEquals("IMMUTABLE",result.getClinicCode()); assertFalse(result.getActive()); assertNull(result.getPhone());
 }
 @Test void invalidTimezoneRejected() { var r = new UpdateClinicRequest(); r.setClinicName("New"); r.setTimezone("invalid/zone"); assertThrows(IllegalArgumentException.class,()->service.update(clinic.getId(),r)); }
 @Test void unchangedStatusDoesNotWriteAndInactiveClinicCanBeReactivated() {
  var r = new UpdateClinicStatusRequest(); r.setActive(false); service.updateStatus(clinic.getId(),r); verify(clinics,never()).saveAndFlush(any());
  r.setActive(true); assertTrue(service.updateStatus(clinic.getId(),r).getActive());
 }
 @Test void settingsCrossClinicAndInactiveTenantAreForbidden() {
  login(Clinic.builder().id(UUID.randomUUID()).active(true).build()); assertThrows(AccessDeniedException.class,()->service.getSettings(clinic.getId()));
  login(clinic); assertThrows(AccessDeniedException.class,()->service.getSettings(clinic.getId())); verifyNoInteractions(settings);
 }
 @Test void platformCanReadInactiveSettingsAndMissingGetNeverCreates() {
  login(null); when(settings.findByClinicId(clinic.getId())).thenReturn(Optional.empty());
  assertThrows(IllegalArgumentException.class,()->service.getSettings(clinic.getId())); verify(settings,never()).saveAndFlush(any());
  when(settings.findByClinicId(clinic.getId())).thenReturn(Optional.of(ClinicSettings.builder().clinic(clinic).currency("INR").build()));
  assertEquals("INR",service.getSettings(clinic.getId()).getCurrency());
 }
 @Test void codePrecheckUsesTrimmedCaseSensitiveValue() {
  var r = new CreateClinicRequest();r.setClinicCode(" Test ");r.setClinicName("Test"); when(clinics.existsByClinicCode("Test")).thenReturn(true);
  assertThrows(IllegalArgumentException.class,()->service.create(r));verify(clinics).existsByClinicCode("Test");verifyNoInteractions(settings);
 }
}

