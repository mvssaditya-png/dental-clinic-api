package com.dentalclinic.odontogram.service;

import com.dentalclinic.odontogram.entity.Odontogram;
import com.dentalclinic.user.entity.AppUser;
import org.springframework.security.core.context.SecurityContextHolder;

/** Called only after clinic-scoped lookup. Never uses updatedBy or encounter doctor. */
public final class OdontogramArchiveAuthorization {
 private OdontogramArchiveAuthorization() {}
 public static boolean allowed(Odontogram chart) {
  var authentication=SecurityContextHolder.getContext().getAuthentication();
  if(authentication==null||!authentication.isAuthenticated()||!(authentication.getPrincipal() instanceof AppUser actor))return false;
  var authorities=authentication.getAuthorities().stream().map(a->a.getAuthority()).collect(java.util.stream.Collectors.toSet());
  if(!authorities.contains("ODONTOGRAM_ARCHIVE"))return false;
  if(authorities.contains("ROLE_ADMIN")||authorities.contains("ROLE_SUPER_ADMIN"))return true;
  return authorities.contains("ROLE_DOCTOR")&&actor.getId()!=null&&chart.getCreatedBy()!=null&&actor.getId().equals(chart.getCreatedBy().getId());
 }
}
