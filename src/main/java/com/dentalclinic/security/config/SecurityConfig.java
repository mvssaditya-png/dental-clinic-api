package com.dentalclinic.security.config;

import com.dentalclinic.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import com.dentalclinic.security.handler.SecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorHandler securityErrorHandler;
    private final com.dentalclinic.auth.web.WebSessionSettings webSettings;
    private final com.dentalclinic.auth.web.WebSessionService webSessions;
    private final com.dentalclinic.user.repository.AppUserRepository users;
    private final com.dentalclinic.security.repository.UserRoleRepository roles;
    private final com.dentalclinic.security.repository.RolePermissionRepository permissions;
    private final com.dentalclinic.clinic.service.ClinicAccessPolicy clinics;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/api/auth/web/csrf", "/api/auth/web/send-otp", "/api/auth/web/verify-otp", "/api/auth/web/logout",
                                        "/api/auth/send-otp",
                                        "/api/auth/verify-otp"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        http.addFilterBefore(new com.dentalclinic.auth.web.WebSessionFilter(webSettings, webSessions, users, roles, permissions, clinics), com.dentalclinic.security.filter.JwtAuthenticationFilter.class);
        return http.build();
    }
}