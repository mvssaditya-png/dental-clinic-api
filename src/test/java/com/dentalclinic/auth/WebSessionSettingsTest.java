package com.dentalclinic.auth;
import com.dentalclinic.auth.web.WebSessionSettings;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WebSessionSettingsTest {
 private final String key="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
 @Test void disabledByDefaultNeedsNoSecret(){assertFalse(new WebSessionSettings(false,true,"","",new MockEnvironment()).enabled);}
 @Test void productionCannotUseInsecureCookies(){assertThrows(IllegalStateException.class,()->new WebSessionSettings(true,false,"http://localhost:8081",key,new MockEnvironment()));}
 @Test void localProfileCannotEnableInsecureExternalOrigins(){var e=new MockEnvironment();e.setActiveProfiles("local-web");assertThrows(IllegalStateException.class,()->new WebSessionSettings(true,false,"https://example.invalid",key,e));}
 @Test void productionSecureNamesAndExactOrigins(){var s=new WebSessionSettings(true,true,"https://example.invalid",key,new MockEnvironment());assertEquals("__Host-dental-web-session",s.cookie());assertTrue(s.secure);assertThrows(IllegalStateException.class,()->new WebSessionSettings(true,true,"*",key,new MockEnvironment()));}
 @Test void noMissingOrShortKey(){assertThrows(IllegalStateException.class,()->new WebSessionSettings(true,true,"https://example.invalid","",new MockEnvironment()));}
 @Test void previousKeyKeepsExistingSessionCsrfStable(){var env=new MockEnvironment();var old=new WebSessionSettings(true,true,"https://example.invalid",key,env);var next=new WebSessionSettings(true,true,"https://example.invalid","AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=",2,key,1,env);var session=new com.dentalclinic.auth.web.WebSessionService.Session(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),java.time.Instant.now().plusSeconds(60),1);var a=new com.dentalclinic.auth.web.WebSessionService(null,old);var b=new com.dentalclinic.auth.web.WebSessionService(null,next);assertEquals(a.csrf(session),b.csrf(session));}
}
