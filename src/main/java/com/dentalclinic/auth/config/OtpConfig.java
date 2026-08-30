package com.dentalclinic.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class OtpConfig {

    @Bean
    public PasswordEncoder otpPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}