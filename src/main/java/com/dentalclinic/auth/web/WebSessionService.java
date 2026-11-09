package com.dentalclinic.auth.web;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service @RequiredArgsConstructor
public class WebSessionService {
 private final JdbcTemplate db; private final WebSessionSettings settings;
 private static final SecureRandom RANDOM=new SecureRandom();
 public record Session(UUID id,UUID userId,Instant expiresAt,int keyVersion){}
 public record Issued(String secret,Session session){}
 public String random(){byte[] b=new byte[32];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 public byte[] hash(String raw){try{if(raw==null||!raw.matches("[A-Za-z0-9_-]{43}"))return new byte[0];return MessageDigest.getInstance("SHA-256").digest(Base64.getUrlDecoder().decode(raw));}catch(Exception e){return new byte[0];}}
 @Transactional public Issued create(UUID user,String previous){
  revoke(previous,"REAUTHENTICATED");String raw=random();UUID id=UUID.randomUUID();
  Instant expiry=db.queryForObject("insert into web_auth_session(id,token_hash,user_id,expires_at,csrf_key_version) values (?,?,?,current_timestamp+interval '24 hours',?) returning expires_at",(r,n)->r.getTimestamp(1).toInstant(),id,hash(raw),user,settings.keyVersion);
  event(id,user,"CREATED","LOGIN");return new Issued(raw,new Session(id,user,expiry,settings.keyVersion));
 }
 @Transactional public Session validate(String raw){
  var rows=db.query("update web_auth_session set last_seen_at=greatest(last_seen_at,current_timestamp) where token_hash=? and revoked_at is null and expires_at>current_timestamp and last_seen_at>current_timestamp-interval '8 hours' returning id,user_id,expires_at,csrf_key_version",(r,n)->new Session(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getTimestamp(3).toInstant(),r.getInt(4)),hash(raw));
  return rows.isEmpty()?null:rows.get(0);
 }
 @Transactional public void revoke(String raw,String reason){
  var rows=db.query("update web_auth_session set revoked_at=current_timestamp,revocation_reason=? where token_hash=? and revoked_at is null returning id,user_id,expires_at,csrf_key_version",(r,n)->new Session(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getTimestamp(3).toInstant(),r.getInt(4)),reason,hash(raw));
  for(var s:rows)event(s.id(),s.userId(),"REVOKED",reason);
 }
 private void event(UUID id,UUID user,String type,String reason){db.update("insert into web_auth_session_event(id,session_id,user_id,event_type,outcome_code) values (?,?,?,?,?)",UUID.randomUUID(),id,user,type,reason);}
 public String sign(String text){return sign(text,settings.keyVersion);}
 private String sign(String text,int version){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(settings.keys.get(version),"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("CSRF configuration unavailable",e);}}
 public String csrf(Session s){return sign("web-session:"+s.keyVersion()+":"+s.id(),s.keyVersion());}
 public boolean equal(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),b.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @Transactional public void cleanup(){
  // Bounded terminal batches; row locks prevent duplicate expiry events across nodes.
  var rows=db.query("select id,user_id,expires_at,csrf_key_version from web_auth_session where revoked_at is null and (expires_at<=current_timestamp or last_seen_at<=current_timestamp-interval '8 hours') order by expires_at limit 500 for update skip locked",(r,n)->new Session(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getTimestamp(3).toInstant(),r.getInt(4)));
  for(var s:rows){event(s.id(),s.userId(),"EXPIRED","TIMEOUT");db.update("update web_auth_session set revoked_at=current_timestamp,revocation_reason='SECURITY' where id=?",s.id());}
  db.update("delete from web_auth_session where id in (select id from web_auth_session where revoked_at<current_timestamp-interval '30 days' limit 500)");
  db.update("delete from web_auth_session_event where id in (select id from web_auth_session_event where occurred_at<current_timestamp-interval '30 days' limit 500)");
 }
}
