package com.dentalclinic.auth.web;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
@Configuration @EnableScheduling @RequiredArgsConstructor
public class WebSessionMaintenance {
 private final WebSessionSettings settings;private final WebSessionService sessions;
 @Scheduled(fixedDelayString="${app.web-session.cleanup-delay-ms:86400000}",initialDelayString="${app.web-session.cleanup-delay-ms:86400000}")
 public void cleanup(){if(settings.enabled)sessions.cleanup();}
}
