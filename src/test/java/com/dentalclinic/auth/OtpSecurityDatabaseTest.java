package com.dentalclinic.auth;

import com.dentalclinic.auth.dto.VerifyOtpRequest;
import com.dentalclinic.auth.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.jpa.show-sql=false")
@EnabledIfEnvironmentVariable(named="WEB_AUTH_TEST_DATABASE",matches="true")
class OtpSecurityDatabaseTest {
 @DynamicPropertySource static void db(DynamicPropertyRegistry r){String u=System.getenv("SPRING_DATASOURCE_URL");if(u==null||!u.matches("jdbc:postgresql://localhost:5432/dental_clinic_auth_verification_[a-z0-9_]+"))throw new IllegalStateException("Isolated local database required");r.add("spring.datasource.url",()->u);}
 @Autowired JdbcTemplate db; @Autowired OtpService service; @Autowired PasswordEncoder encoder; @Autowired PlatformTransactionManager transactions;
 UUID otp; String phone;
 @BeforeEach void setup(){UUID clinic=UUID.randomUUID();phone="T"+UUID.randomUUID().toString().substring(0,18);otp=UUID.randomUUID();db.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,'Auth test')",clinic,clinic.toString());db.update("insert into app_user(id,clinic_id,first_name,phone) values (?,?,'Auth test',?)",UUID.randomUUID(),clinic,phone);db.update("insert into otp_verification(id,phone,purpose,otp_hash,expires_at) values (?,?,'LOGIN',?,current_timestamp + interval '5 minutes')",otp,phone,encoder.encode("test-correct"));}
 VerifyOtpRequest request(String value){var r=new VerifyOtpRequest();r.setPhone(phone);r.setOtp(value);return r;}
 int attempts(){return db.queryForObject("select attempt_count from otp_verification where id=?",Integer.class,otp);}
 @Test void correctNativeContractAndSingleConsumption(){var result=service.verifyLoginOtp(request("test-correct"));assertTrue(result.isSuccess());assertEquals("Bearer",result.getTokenType());assertNotNull(result.getAccessToken());assertTrue(result.getExpiresInSeconds()>0);assertThrows(IllegalArgumentException.class,()->service.verifyLoginOtp(request("test-correct")));}
 @Test void failedCounterCommitsAndLimitApplies(){for(int n=1;n<=5;n++){assertThrows(FailedOtpException.class,()->service.verifyLoginOtp(request("bad")));assertEquals(n,attempts());}assertThrows(IllegalStateException.class,()->service.verifyLoginOtp(request("test-correct")));assertEquals(5,attempts());}
 @Test void expiredCannotConsume(){db.update("update otp_verification set expires_at=current_timestamp-interval '1 second' where id=?",otp);assertThrows(IllegalArgumentException.class,()->service.verifyLoginOtp(request("test-correct")));assertFalse(db.queryForObject("select is_used from otp_verification where id=?",Boolean.class,otp));}
 @Test void concurrentSuccessOnlyOnce()throws Exception{var results=parallel("test-correct",2);assertEquals(1,results.stream().filter(Boolean::booleanValue).count());}
 @Test void concurrentFailuresDoNotLoseIncrements()throws Exception{parallel("bad",8);assertEquals(5,attempts());}
 List<Boolean> parallel(String value,int count)throws Exception{var pool=Executors.newFixedThreadPool(count);try{var start=new CountDownLatch(1);List<Future<Boolean>> pending=new ArrayList<>();for(int i=0;i<count;i++)pending.add(pool.submit(()->{start.await();try{service.verifyLoginOtp(request(value));return true;}catch(IllegalArgumentException|IllegalStateException expected){return false;}}));start.countDown();List<Boolean> result=new ArrayList<>();for(var f:pending)result.add(f.get(30,TimeUnit.SECONDS));return result;}finally{pool.shutdownNow();}}
 @Test void laterTransactionFailureRollsBackSuccessfulConsumption(){var tx=new TransactionTemplate(transactions);assertThrows(IllegalStateException.class,()->tx.execute(status->{service.verifyLoginOtp(request("test-correct"));throw new IllegalStateException("simulated downstream persistence failure");}));assertFalse(db.queryForObject("select is_used from otp_verification where id=?",Boolean.class,otp));assertTrue(service.verifyLoginOtp(request("test-correct")).isSuccess());}
 @Test void loggingContainsNoPlaintextOutput()throws Exception{String source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/dentalclinic/auth/service/OtpService.java"));assertFalse(source.contains("System.out"));assertFalse(source.contains("LOGIN OTP for"));}
}
