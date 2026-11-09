package com.dentalclinic.auth.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;

@Component
public class WebSessionSettings {
 public final boolean enabled,secure;
 public final Set<String> origins;
 public final byte[] csrfKey;
 public final int keyVersion;
 public final Map<Integer,byte[]> keys;
 public WebSessionSettings(boolean enabled,boolean secure,String origins,String key,Environment env){this(enabled,secure,origins,key,1,"",0,env);}
 @org.springframework.beans.factory.annotation.Autowired
 public WebSessionSettings(@Value("${app.web-session.enabled:false}") boolean enabled,
 @Value("${app.web-session.secure:true}") boolean secure,
 @Value("${app.web-session.origins:}") String origins,
 @Value("${app.web-session.csrf-key:}") String key, @Value("${app.web-session.csrf-key-version:1}") int version, @Value("${app.web-session.previous-csrf-key:}") String previousKey, @Value("${app.web-session.previous-csrf-key-version:0}") int previousVersion, Environment env){
  this.enabled=enabled;this.secure=secure;
  this.origins=Set.copyOf(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());
  this.csrfKey=key.isEmpty()?new byte[0]:Base64.getDecoder().decode(key);
  this.keyVersion=version;var keyMap=new HashMap<Integer,byte[]>();keyMap.put(version,this.csrfKey);if(!previousKey.isEmpty()){byte[] previous=Base64.getDecoder().decode(previousKey);if(previous.length<32||previousVersion<=0||previousVersion==version)throw new IllegalStateException("Invalid previous CSRF key configuration");keyMap.put(previousVersion,previous);}this.keys=Map.copyOf(keyMap);
  if(enabled){
   if(version<1||version>32767)throw new IllegalStateException("Invalid CSRF key version");
   if(this.csrfKey.length<32||this.origins.isEmpty())throw new IllegalStateException("Web sessions require configured origins and a separate 256-bit CSRF key");
   boolean local=Arrays.asList(env.getActiveProfiles()).contains("local-web");
   for(String origin:this.origins){URI u=URI.create(origin);boolean loopback=u.getHost()!=null&&Set.of("localhost","127.0.0.1","[::1]").contains(u.getHost());
    if(!secure&&!loopback)throw new IllegalStateException("HTTP development cookies are loopback-only");
    if(u.getHost()==null||u.getRawUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||!u.getPath().isEmpty()||(!"https".equals(u.getScheme())&&!(local&&!secure&&loopback&&"http".equals(u.getScheme()))))throw new IllegalStateException("Invalid Web session origin");
   }
   if(!secure&&(!local||Arrays.stream(env.getActiveProfiles()).anyMatch(p->!p.equals("local-web"))))throw new IllegalStateException("Insecure cookies require the exclusive local-web profile");
  }
 }
 public String cookie(){return secure?"__Host-dental-web-session":"dental-web-session-dev";}
 public String preauth(){return secure?"__Host-dental-web-preauth":"dental-web-preauth-dev";}
}
