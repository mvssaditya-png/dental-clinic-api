package com.dentalclinic.auth.web;
import com.dentalclinic.auth.dto.*;
import com.dentalclinic.auth.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;

@RestController @RequestMapping("/api/auth/web") @RequiredArgsConstructor
public class WebSessionController {
 private final WebSessionSettings settings;private final WebSessionService sessions;private final OtpService otp;
 private void cookie(HttpServletResponse res,String name,String value,boolean clear,boolean preauth){var b=ResponseCookie.from(name,value).httpOnly(true).secure(settings.secure).sameSite("Lax").path("/");if(clear)b.maxAge(0);else if(preauth)b.maxAge(600);res.addHeader("Set-Cookie",b.build().toString());}
 @GetMapping("/csrf") public Map<String,String> csrf(HttpServletRequest req,HttpServletResponse res){
  var session=(WebSessionService.Session)req.getAttribute(WebSessionFilter.SESSION);String token;
  if(session!=null)token=sessions.csrf(session);else{String nonce=WebSessionFilter.cookie(req,settings.preauth());if(nonce==null||!nonce.matches("[A-Za-z0-9_-]{43}"))nonce=sessions.random();cookie(res,settings.preauth(),nonce,false,true);String expiry=Long.toString(Instant.now().getEpochSecond()+600);token=expiry+"."+sessions.sign("preauth:"+nonce+":"+expiry);}
  return Map.of("csrfToken",token,"headerName","X-CSRF-Token");
 }
 @PostMapping("/send-otp") public SendOtpResponse send(@Valid @RequestBody SendOtpRequest request){return otp.sendLoginOtp(request);}
 @PostMapping("/verify-otp") @Transactional(noRollbackFor=FailedOtpException.class)
 public Map<String,Object> verify(@Valid @RequestBody VerifyOtpRequest request,HttpServletRequest req,HttpServletResponse res){
  var identity=otp.verifyLoginOtp(request);var issued=sessions.create(identity.getUserId(),WebSessionFilter.cookie(req,settings.cookie()));
  cookie(res,settings.cookie(),issued.secret(),false,false);cookie(res,settings.preauth(),"",true,true);
  return Map.of("success",true,"expiresAt",issued.session().expiresAt().toString());
 }
 @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest req,HttpServletResponse res){sessions.revoke(WebSessionFilter.cookie(req,settings.cookie()),"LOGOUT");cookie(res,settings.cookie(),"",true,false);cookie(res,settings.preauth(),"",true,true);return ResponseEntity.noContent().build();}
}
