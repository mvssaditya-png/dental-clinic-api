package com.dentalclinic.appointment;

import com.dentalclinic.appointment.service.AppointmentService;
import com.dentalclinic.appointment.repository.*;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.doctor.entity.DoctorProfile;
import com.dentalclinic.doctor.repository.DoctorProfileRepository;
import com.dentalclinic.user.entity.AppUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentLookupServiceTest {
    @Mock AppointmentRepository appointments;
    @Mock DoctorProfileRepository doctors;
    @Mock ClinicRepository clinics;
    @InjectMocks AppointmentService service;
    private final UUID clinicId = UUID.randomUUID(), userId = UUID.randomUUID();
    private Clinic clinic() { return Clinic.builder().id(clinicId).build(); }
    private void login(Clinic clinic) {
        AppUser user = AppUser.builder().id(userId).clinic(clinic).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void bookingIgnoresForeignClinicOverrideForClinicBoundUser() {
        login(clinic()); when(doctors.findBookingDoctorsByClinicId(clinicId)).thenReturn(List.of());
        assertTrue(service.getBookingDoctors(UUID.randomUUID()).isEmpty());
        verify(doctors).findBookingDoctorsByClinicId(clinicId); verifyNoInteractions(clinics);
    }
    @Test void platformBookingRequiresExplicitValidClinic() {
        login(null);
        assertThrows(IllegalArgumentException.class, () -> service.getBookingDoctors(null));
        verifyNoInteractions(doctors);
        when(clinics.findById(clinicId)).thenReturn(Optional.of(clinic()));
        when(doctors.findBookingDoctorsByClinicId(clinicId)).thenReturn(List.of());
        service.getBookingDoctors(clinicId); verify(doctors).findBookingDoctorsByClinicId(clinicId);
    }
    @Test void mineUsesPrincipalUserIdRatherThanProfileId() {
        login(clinic()); LocalDate date = LocalDate.of(2026, 9, 26);
        when(doctors.findByUserIdAndClinicId(userId, clinicId)).thenReturn(Optional.of(DoctorProfile.builder().id(UUID.randomUUID()).build()));
        when(appointments.findOwnDailyAppointmentsWithDetails(clinicId, userId, date)).thenReturn(List.of());
        assertTrue(service.getMyAppointments(date).isEmpty());
        verify(appointments).findOwnDailyAppointmentsWithDetails(clinicId, userId, date);
        verifyNoMoreInteractions(appointments);
    }
    @Test void mineDoesNotFallBackWhenProfileMissingOrFromAnotherClinic() {
        login(clinic()); when(doctors.findByUserIdAndClinicId(userId, clinicId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getMyAppointments(LocalDate.now()));
        verifyNoInteractions(appointments);
    }
    @Test void mineRejectsMissingClinicAndDate() {
        login(null); assertThrows(IllegalArgumentException.class, () -> service.getMyAppointments(LocalDate.now()));
        login(clinic()); assertThrows(IllegalArgumentException.class, () -> service.getMyAppointments(null));
        verifyNoInteractions(appointments, doctors);
    }
}
