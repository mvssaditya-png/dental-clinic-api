package com.dentalclinic.security;

import com.dentalclinic.appointment.controller.AppointmentController;
import com.dentalclinic.appointment.service.AppointmentService;
import com.dentalclinic.appointment.dto.UpdateAppointmentStatusRequest;
import com.dentalclinic.appointment.entity.AppointmentStatus;
import com.dentalclinic.treatment.controller.TreatmentPlanController;
import com.dentalclinic.treatment.service.TreatmentPlanService;
import com.dentalclinic.treatment.dto.UpdateTreatmentPlanStatusRequest;
import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import com.dentalclinic.security.authorization.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationMethodSecurityTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean AppointmentAuthorization appointmentAuthorization() { return new AppointmentAuthorization(); }
        @Bean TreatmentPlanAuthorization treatmentPlanAuthorization() { return new TreatmentPlanAuthorization(); }
        @Bean AppointmentService appointmentService() { return mock(AppointmentService.class); }
        @Bean TreatmentPlanService treatmentPlanService() { return mock(TreatmentPlanService.class); }
        @Bean AppointmentController appointmentController(AppointmentService s) { return new AppointmentController(s); }
        @Bean TreatmentPlanController treatmentPlanController(TreatmentPlanService s) { return new TreatmentPlanController(s); }
    }
    private AnnotationConfigApplicationContext context;
    @BeforeEach void setup() { context = new AnnotationConfigApplicationContext(Config.class); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); context.close(); }
    private void login(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", null,
                Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }
    @Test void actualAppointmentAnnotationResolvesBeanAndRequestArgument() {
        var controller = context.getBean(AppointmentController.class);
        var request = new UpdateAppointmentStatusRequest(); request.setStatus(AppointmentStatus.IN_CONSULTATION);
        login("ROLE_DOCTOR", "CONSULTATION_MANAGE");
        assertDoesNotThrow(() -> controller.updateAppointmentStatus(UUID.randomUUID(), request));
        request.setStatus(AppointmentStatus.CANCELLED);
        assertThrows(AccessDeniedException.class, () -> controller.updateAppointmentStatus(UUID.randomUUID(), request));
        login("APPOINTMENT_CANCEL"); assertDoesNotThrow(() -> controller.updateAppointmentStatus(UUID.randomUUID(), request));
    }
    @Test void actualTreatmentAnnotationProtectsApproval() {
        var controller = context.getBean(TreatmentPlanController.class);
        var request = new UpdateTreatmentPlanStatusRequest(); request.setStatus(TreatmentPlanStatus.APPROVED);
        login("TREATMENT_PLAN_EDIT");
        assertThrows(AccessDeniedException.class, () -> controller.updateTreatmentPlanStatus(UUID.randomUUID(), request));
        login("TREATMENT_PLAN_APPROVE"); assertDoesNotThrow(() -> controller.updateTreatmentPlanStatus(UUID.randomUUID(), request));
    }
    @Test void bookingLookupDoesNotRequireUserViewAndReadOnlyStaffCannotUseIt() {
        var controller = context.getBean(AppointmentController.class);
        login("ROLE_RECEPTIONIST", "APPOINTMENT_CREATE"); assertDoesNotThrow(() -> controller.getBookingDoctors(null));
        login("APPOINTMENT_VIEW"); assertThrows(AccessDeniedException.class, () -> controller.getBookingDoctors(null));
    }
    @Test void mineRequiresBothDoctorRoleAndViewPermission() {
        var controller = context.getBean(AppointmentController.class);
        login("ROLE_DOCTOR", "APPOINTMENT_VIEW"); assertDoesNotThrow(() -> controller.getMyAppointments(LocalDate.now()));
        login("ROLE_DOCTOR"); assertThrows(AccessDeniedException.class, () -> controller.getMyAppointments(LocalDate.now()));
        login("ROLE_RECEPTIONIST", "APPOINTMENT_VIEW"); assertThrows(AccessDeniedException.class, () -> controller.getMyAppointments(LocalDate.now()));
    }
}
