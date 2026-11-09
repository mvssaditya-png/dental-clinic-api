package com.dentalclinic.auth.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Set;

/** Optional interactive development delivery; never an API response or redirected log. */
@Component
public class LocalOtpDelivery {
 private final boolean enabled; private final Environment environment;
 public LocalOtpDelivery(@Value("${app.local-otp-console.enabled:false}") boolean enabled,Environment environment){this.enabled=enabled;this.environment=environment;}
 @PostConstruct public void validate(){
  if(enabled&&(!Arrays.equals(environment.getActiveProfiles(),new String[]{"local-web"})||!Set.of("127.0.0.1","::1","localhost").contains(environment.getProperty("server.address",""))))throw new IllegalStateException("Interactive OTP delivery requires exclusive local-web profile and explicit loopback server binding");
 }
 public void deliver(String value){
  if(!enabled)return;
  // A redirected process, service, IDE output panel or automated test cannot receive codes.
  var console=System.console();
  if(console==null)throw new IllegalStateException("Interactive local OTP console is unavailable; no OTP was delivered");
  console.writer().println("Local development login code: "+value);
  console.writer().flush();
 }
}
