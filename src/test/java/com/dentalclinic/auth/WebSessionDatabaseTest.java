package com.dentalclinic.auth;
import com.dentalclinic.auth.web.*;
import com.dentalclinic.auth.service.*;
import com.dentalclinic.auth.dto.VerifyOtpRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import com.dentalclinic.user.repository.AppUserRepository;
import com.dentalclinic.security.repository.*;
import com.dentalclinic.clinic.service.ClinicAccessPolicy;
import jakarta.servlet.http.Cookie;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.jpa.show-sql=false","spring.profiles.active=local-web","app.web-session.enabled=true","app.web-session.secure=false","app.web-session.origins=http://localhost:8081","app.web-session.csrf-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="})
@EnabledIfEnvironmentVariable(named="WEB_AUTH_TEST_DATABASE",matches="true")
class WebSessionDatabaseTest {
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){String url=System.getenv("SPRING_DATASOURCE_URL");if(url==null||!url.matches("jdbc:postgresql://localhost:5432/dental_clinic_auth_verification_[a-z0-9_]+"))throw new IllegalStateException("Isolated local DB required");r.add("spring.datasource.url",()->url);}
 @Autowired WebSessionService service;@Autowired WebSessionSettings settings;@Autowired WebSessionController controller;@Autowired JdbcTemplate db;@Autowired PasswordEncoder encoder;
 @Autowired AppUserRepository users;@Autowired UserRoleRepository roles;@Autowired RolePermissionRepository permissions;@Autowired ClinicAccessPolicy clinics;
 UUID user,clinic;String phone;WebSessionFilter filter;
 @BeforeEach void setup(){user=UUID.randomUUID();clinic=UUID.randomUUID();phone="W"+user.toString().substring(0,18);db.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,'Web test')",clinic,clinic.toString());db.update("insert into app_user(id,clinic_id,first_name,phone) values (?,?,'Web test',?)",user,clinic,phone);filter=new WebSessionFilter(settings,service,users,roles,permissions,clinics);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void hashedStorageAndRevocation(){var issued=service.create(user,null);assertEquals(32,db.queryForObject("select octet_length(token_hash) from web_auth_session where id=?",Integer.class,issued.session().id()));assertEquals(user,service.validate(issued.secret()).userId());service.revoke(issued.secret(),"LOGOUT");assertNull(service.validate(issued.secret()));assertEquals(2,db.queryForObject("select count(*) from web_auth_session_event where session_id=?",Integer.class,issued.session().id()));}
 @Test void idleAndAbsoluteExpiration(){var a=service.create(user,null);db.update("update web_auth_session set created_at=current_timestamp-interval '10 hours',last_seen_at=current_timestamp-interval '9 hours' where id=?",a.session().id());assertNull(service.validate(a.secret()));var b=service.create(user,null);db.update("update web_auth_session set created_at=current_timestamp-interval '25 hours',expires_at=current_timestamp-interval '1 hour' where id=?",b.session().id());assertNull(service.validate(b.secret()));}
 @Test void reauthenticationRevokesPrior(){var a=service.create(user,null);var b=service.create(user,a.secret());assertNull(service.validate(a.secret()));assertNotEquals(a.secret(),b.secret());assertNotNull(service.validate(b.secret()));}
 MockHttpServletRequest request(String method,String path,String secret){var r=new MockHttpServletRequest(method,path);if(secret!=null)r.setCookies(new Cookie(settings.cookie(),secret));r.addHeader("Origin","http://localhost:8081");return r;}
 int run(MockHttpServletRequest r)throws Exception{var response=new MockHttpServletResponse();filter.doFilter(r,response,(req,res)->res.getWriter().write("ok"));return response.getStatus();}
 @Test void csrfAndExpectedAccountRequired()throws Exception{var a=service.create(user,null);var r=request("PUT","/api/example",a.secret());assertEquals(409,run(r));r.addHeader("X-Expected-User-Id",user.toString());assertEquals(403,run(r));r.addHeader("X-CSRF-Token",service.csrf(a.session()));assertEquals(200,run(r));}
 @Test void hostileOriginRejected()throws Exception{var a=service.create(user,null);var r=request("POST","/api/example",a.secret());r.removeHeader("Origin");r.addHeader("Origin","http://evil.invalid");r.addHeader("X-CSRF-Token",service.csrf(a.session()));assertEquals(403,run(r));}
 @Test void ambiguousCredentialsRejectedAndBearerOnlyUntouched()throws Exception{var a=service.create(user,null);var r=request("GET","/api/auth/me",a.secret());r.addHeader("Authorization","Bearer invalid");assertEquals(400,run(r));var nativeRequest=request("GET","/api/auth/me",null);nativeRequest.addHeader("Authorization","Bearer test-only");assertEquals(200,run(nativeRequest));}
 @Test void invalidCookieDeniedAndInactiveClinicDenied()throws Exception{assertEquals(401,run(request("GET","/api/auth/me","invalid")));var a=service.create(user,null);db.update("update clinic set is_active=false where id=?",clinic);assertEquals(403,run(request("GET","/api/auth/me",a.secret())));}
 @Test void preauthCsrfAndCookieAttributes()throws Exception{var req=request("GET","/api/auth/web/csrf",null);var res=new MockHttpServletResponse();var result=controller.csrf(req,res);String header=res.getHeader("Set-Cookie");assertTrue(header.contains("HttpOnly"));assertTrue(header.contains("SameSite=Lax"));assertFalse(header.contains("Domain="));String nonce=header.split(";",2)[0].split("=",2)[1];var post=request("POST","/api/auth/web/send-otp",null);post.setCookies(new Cookie(settings.preauth(),nonce));assertEquals(403,run(post));post.addHeader("X-CSRF-Token",result.get("csrfToken"));assertEquals(200,run(post));}
 @Test void sessionCsrfStableAcrossTabs(){var a=service.create(user,null);assertEquals(service.csrf(a.session()),service.csrf(service.validate(a.secret())));}
 @Test void webLoginCreatesCookieWithoutBearerResponse(){db.update("insert into otp_verification(id,phone,purpose,otp_hash,expires_at) values (?,?,'LOGIN',?,current_timestamp+interval '5 minutes')",UUID.randomUUID(),phone,encoder.encode("test-otp"));var r=new VerifyOtpRequest();r.setPhone(phone);r.setOtp("test-otp");var response=new MockHttpServletResponse();var body=controller.verify(r,request("POST","/api/auth/web/verify-otp",null),response);assertEquals(Set.of("success","expiresAt"),body.keySet());assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(s->s.startsWith(settings.cookie()+"=")));}
 @Test void cleanupRetainsRecentAudit(){var a=service.create(user,null);db.update("update web_auth_session set created_at=current_timestamp-interval '10 hours',last_seen_at=current_timestamp-interval '9 hours' where id=?",a.session().id());service.cleanup();assertEquals(1,db.queryForObject("select count(*) from web_auth_session_event where session_id=? and event_type='EXPIRED'",Integer.class,a.session().id()));service.cleanup();assertEquals(1,db.queryForObject("select count(*) from web_auth_session_event where session_id=? and event_type='EXPIRED'",Integer.class,a.session().id()));}
 @Test void staleAccountReadRejected()throws Exception{var a=service.create(user,null);var r=request("GET","/api/patients",a.secret());r.addHeader("X-Expected-User-Id",UUID.randomUUID().toString());assertEquals(409,run(r));}
 @Test void staleAccountAllMethodsRejectedBeforeCsrf()throws Exception{var a=service.create(user,null);for(String method:List.of("GET","POST","PUT","PATCH","DELETE")){var r=request(method,"/api/patients",a.secret());r.addHeader("X-Expected-User-Id",UUID.randomUUID().toString());r.addHeader("X-CSRF-Token","old-session-token");assertEquals(409,run(r),method);}}
}
