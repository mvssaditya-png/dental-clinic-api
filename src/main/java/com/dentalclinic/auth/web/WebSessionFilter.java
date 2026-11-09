package com.dentalclinic.auth.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.dentalclinic.user.repository.AppUserRepository;
import com.dentalclinic.user.entity.UserStatus;
import com.dentalclinic.security.repository.*;
import com.dentalclinic.clinic.service.ClinicAccessPolicy;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class WebSessionFilter extends OncePerRequestFilter {
 public static final String SESSION="dental.web.session";
 private final WebSessionSettings settings;private final WebSessionService sessions;
 private final AppUserRepository users;private final UserRoleRepository roles;private final RolePermissionRepository permissions;private final ClinicAccessPolicy clinics;
 public static String cookie(HttpServletRequest req,String name){String value=null;for(Cookie c:req.getCookies()==null?new Cookie[0]:req.getCookies())if(c.getName().equals(name)){if(value!=null)throw new IllegalArgumentException("Duplicate cookie");value=c.getValue();}return value;}
 public static void error(HttpServletResponse res,int status,String code)throws IOException{res.setStatus(status);res.setContentType("application/json");res.setHeader("Cache-Control","no-store");res.getWriter().write("{\"status\":"+status+",\"error\":\""+code+"\",\"message\":\""+code+"\"}");}
 public boolean originAllowed(HttpServletRequest req){String origin=req.getHeader("Origin");if(origin==null){try{URI uri=URI.create(req.getHeader("Referer"));origin=uri.getScheme()+"://"+uri.getRawAuthority();}catch(Exception e){return false;}}return settings.origins.contains(origin)&&!"cross-site".equals(req.getHeader("Sec-Fetch-Site"));}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  boolean web=req.getRequestURI().startsWith("/api/auth/web/");
  if(!settings.enabled){if(web){error(res,404,"WEB_SESSION_DISABLED");return;}chain.doFilter(req,res);return;}
  String raw;try{raw=cookie(req,settings.cookie());}catch(IllegalArgumentException e){error(res,400,"INVALID_COOKIE");return;}
  if(req.getHeader("Authorization")!=null&&(raw!=null||web)){error(res,400,"AMBIGUOUS_CREDENTIALS");return;}
  boolean unsafe=!Set.of("GET","HEAD","OPTIONS").contains(req.getMethod());
  if((web||raw!=null)&&unsafe&&!originAllowed(req)){error(res,403,"ORIGIN_REJECTED");return;}
  if(web&&req.getHeader("Origin")!=null&&!settings.origins.contains(req.getHeader("Origin"))){error(res,403,"ORIGIN_REJECTED");return;}
  WebSessionService.Session session=null;
  try{if(raw!=null)session=sessions.validate(raw);}catch(org.springframework.dao.DataAccessException e){error(res,503,"SESSION_UNAVAILABLE");return;}
  if(session!=null){
   var user=users.findByIdWithClinic(session.userId()).filter(u->u.getStatus()==UserStatus.ACTIVE).orElse(null);
   if(user==null){error(res,401,"SESSION_INVALID");return;}
   try{clinics.requireUserClinicActive(user);}catch(org.springframework.security.access.AccessDeniedException e){error(res,403,"CLINIC_UNAVAILABLE");return;}
   var authorities=new ArrayList<SimpleGrantedAuthority>();
   for(var role:roles.findAllByUserId(user.getId())){authorities.add(new SimpleGrantedAuthority("ROLE_"+role.getRole().getRoleCode()));for(var permission:permissions.findAllByRoleId(role.getRole().getId()))authorities.add(new SimpleGrantedAuthority(permission.getPermission().getPermissionCode()));}
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,authorities));req.setAttribute(SESSION,session);
   if(!web&&(unsafe||req.getHeader("X-Expected-User-Id")!=null)&&!session.userId().toString().equals(req.getHeader("X-Expected-User-Id"))){error(res,409,"SESSION_IDENTITY_CHANGED");return;}
   if(unsafe){
    if(!sessions.equal(sessions.csrf(session),req.getHeader("X-CSRF-Token"))){error(res,403,"CSRF_INVALID");return;}
   }
  }else if(raw!=null&&!web){error(res,401,"SESSION_INVALID");return;}
  if(web&&unsafe&&session==null&&!req.getRequestURI().endsWith("/logout")){
   String nonce;try{nonce=cookie(req,settings.preauth());}catch(Exception e){error(res,400,"INVALID_COOKIE");return;}
   String token=req.getHeader("X-CSRF-Token");boolean valid=false;
   try{String[] parts=token.split("\\.");long expiry=Long.parseLong(parts[0]);valid=parts.length==2&&nonce!=null&&nonce.matches("[A-Za-z0-9_-]{43}")&&expiry>Instant.now().getEpochSecond()&&expiry<=Instant.now().getEpochSecond()+600&&sessions.equal(parts[1],sessions.sign("preauth:"+nonce+":"+parts[0]));}catch(Exception ignored){}
   if(!valid){error(res,403,"CSRF_INVALID");return;}
  }
  if(web||raw!=null)res.setHeader("Cache-Control","no-store");
  chain.doFilter(req,res);
 }
}
