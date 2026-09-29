package com.dentalclinic.security;

import com.dentalclinic.auth.service.JwtService;
import com.dentalclinic.appointment.controller.AppointmentController;
import com.dentalclinic.appointment.service.AppointmentService;
import com.dentalclinic.treatment.controller.TreatmentPlanController;
import com.dentalclinic.treatment.service.TreatmentPlanService;
import com.dentalclinic.security.authorization.*;
import com.dentalclinic.security.config.SecurityConfig;
import com.dentalclinic.security.filter.JwtAuthenticationFilter;
import com.dentalclinic.security.handler.SecurityErrorHandler;
import com.dentalclinic.security.repository.*;
import com.dentalclinic.user.repository.AppUserRepository;
import com.dentalclinic.common.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

class SecurityHttpErrorHandlingTest {
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, SecurityErrorHandler.class, GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class, AppointmentAuthorization.class, TreatmentPlanAuthorization.class,
        AppointmentController.class, TreatmentPlanController.class, ProbeController.class})
    static class Config {
        @Bean com.dentalclinic.clinic.service.ClinicAccessPolicy clinicAccessPolicy() { return new com.dentalclinic.clinic.service.ClinicAccessPolicy(); }
        @Bean ObjectMapper objectMapper() { return JsonMapper.builder().build(); }
        @Bean JwtService jwtService() { return mock(JwtService.class); }
        @Bean AppUserRepository users() { return mock(AppUserRepository.class); }
        @Bean UserRoleRepository roles() { return mock(UserRoleRepository.class); }
        @Bean RolePermissionRepository permissions() { return mock(RolePermissionRepository.class); }
        @Bean AppointmentService appointments() { return mock(AppointmentService.class); }
        @Bean TreatmentPlanService plans() { return mock(TreatmentPlanService.class); }
        // Exercise the configured handler for a filter-chain denial independently of MVC advice.
        @Bean @Order(0) SecurityFilterChain filterProbe(HttpSecurity http, SecurityErrorHandler handler) throws Exception {
            return http.securityMatcher("/filter-denied").csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a.anyRequest().hasAuthority("FILTER_PERMISSION"))
                .exceptionHandling(e -> e.authenticationEntryPoint(handler).accessDeniedHandler(handler)).build();
        }
    }
    @RestController
    static class ProbeController {
        record Input(@NotBlank String name) {}
        @GetMapping("/api/auth/me") Map<String,String> me() { return Map.of("result","ok"); }
        @PostMapping({"/api/auth/send-otp", "/api/auth/verify-otp"}) Map<String,String> otp() { return Map.of("result","public"); }
        @PostMapping("/probe/validation") void validate(@Valid @RequestBody Input input) {}
        @GetMapping("/probe/error/{type}") void error(@PathVariable String type) {
            switch(type) {
                case "argument" -> throw new IllegalArgumentException("Workflow rejected");
                case "state" -> throw new IllegalStateException("Existing state failure");
                case "authentication" -> throw new BadCredentialsException("Secret detail");
                default -> throw new RuntimeException("Secret detail");
            }
        }
    }
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test", Map.of("app.jwt.expiration-ms", "60000", "app.jwt.secret", "test-only-signing-key-at-least-thirty-two-characters-long")));
        context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void cleanup() { context.close(); org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    @Test void missingAndInvalidBearerReturnJson401() throws Exception {
        for (String header : List.of("", "Bearer invalid", "Bearer ", "Basic invalid")) {
            var request = get("/api/auth/me"); if (!header.isEmpty()) request.header("Authorization",header);
            mvc.perform(request).andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate","Bearer"))
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").isString()).andExpect(jsonPath("$.message").value("Authentication is required"));
        }
    }
    @Test void realAppointmentMethodDenialIs403AndNeverCallsService() throws Exception {
        mvc.perform(patch("/api/appointments/"+UUID.randomUUID()+"/status")
            .with(user("doctor").authorities(new SimpleGrantedAuthority("CONSULTATION_MANAGE")))
            .contentType("application/json").content("{\"status\":\"CHECKED_IN\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden")).andExpect(header().doesNotExist("WWW-Authenticate"));
        verifyNoInteractions(context.getBean(AppointmentService.class));
    }
    @Test void realTreatmentApprovalDenialIs403AndNeverCallsService() throws Exception {
        mvc.perform(patch("/api/treatment-plans/"+UUID.randomUUID()+"/status")
            .with(user("doctor").authorities(new SimpleGrantedAuthority("TREATMENT_PLAN_EDIT")))
            .contentType("application/json").content("{\"status\":\"APPROVED\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(context.getBean(TreatmentPlanService.class));
    }
    @Test void filterDenialUsesSameJson403() throws Exception {
        mvc.perform(get("/filter-denied").with(user("staff"))).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }
    @Test void permittedActualControllerStillSucceeds() throws Exception {
        mvc.perform(get("/api/appointments/booking-doctors").with(user("receptionist")
            .authorities(new SimpleGrantedAuthority("APPOINTMENT_CREATE")))).andExpect(status().isOk());
        verify(context.getBean(AppointmentService.class)).getBookingDoctors(null);
    }
    @Test void validationAndBusinessAndUnexpectedErrorsKeepMappings() throws Exception {
        mvc.perform(post("/probe/validation").with(user("staff")).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").exists());
        mvc.perform(get("/probe/error/argument").with(user("staff"))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Workflow rejected"));
        mvc.perform(get("/probe/error/state").with(user("staff"))).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Existing state failure"));
        mvc.perform(get("/probe/error/unexpected").with(user("staff"))).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
    @Test void authenticationExceptionFromMvcIs401() throws Exception {
        mvc.perform(get("/probe/error/authentication").with(user("staff"))).andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate","Bearer")).andExpect(jsonPath("$.status").value(401));
    }
    @Test void publicOtpRoutesRemainAccessible() throws Exception {
        for (String path : List.of("/api/auth/send-otp", "/api/auth/verify-otp"))
            mvc.perform(post(path).header("Authorization","Bearer invalid")).andExpect(status().isOk());
    }
    @Test void anonymousAdviceDenialIs401() throws Exception {
        var response = new MockHttpServletResponse();
        var auth = new org.springframework.security.authentication.AnonymousAuthenticationToken("key","anonymous",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        context.getBean(GlobalExceptionHandler.class).handleAccessDenied(new AccessDeniedException("denied"),new MockHttpServletRequest(),response);
        org.junit.jupiter.api.Assertions.assertEquals(401,response.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Bearer",response.getHeader("WWW-Authenticate"));
    }
}
