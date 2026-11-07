package com.dentalclinic.odontogram.service;
import com.dentalclinic.odontogram.dto.*;
import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
@Service @RequiredArgsConstructor
public class OdontogramConfigurationService {
 private final JdbcTemplate jdbc;
 private final TenantClinicResolver clinics;
 private UUID clinic(UUID requested){return clinics.resolve(SecurityUtils.getCurrentUser(),requested,"clinicId is required for platform users").getId();}
 private static final String SELECT="select c.*, f.colour_hex from odontogram_condition c left join odontogram_colour_config f on f.condition_id=c.id and f.clinic_id=c.clinic_id and f.is_active=true ";
 private final org.springframework.jdbc.core.RowMapper<ManagedOdontogramConditionResponse> mapper=(r,n)->new ManagedOdontogramConditionResponse(
  r.getObject("id",UUID.class),r.getObject("clinic_id",UUID.class),r.getString("condition_code"),r.getString("condition_name"),r.getString("description"),r.getString("patient_explanation"),r.getBoolean("is_active"),r.getString("colour_hex"),r.getLong("version"),r.getTimestamp("created_at").toLocalDateTime(),r.getTimestamp("updated_at").toLocalDateTime());
 @Transactional(readOnly=true) public List<ManagedOdontogramConditionResponse> list(UUID requested,boolean inactive){return jdbc.query(SELECT+"where c.clinic_id=? "+(inactive?"":"and c.is_active=true ")+"order by lower(c.condition_name),c.id",mapper,clinic(requested));}
 @Transactional(readOnly=true) public ManagedOdontogramConditionResponse get(UUID id,UUID requested){return read(id,clinic(requested));}
 private ManagedOdontogramConditionResponse read(UUID id,UUID clinic){return jdbc.query(SELECT+"where c.id=? and c.clinic_id=?",mapper,id,clinic).stream().findFirst().orElseThrow(()->new OdontogramMutationException(404,"Condition unavailable in this clinic"));}
 private static String normalize(String text){return text==null||text.isBlank()?null:text.trim();}
 private static long expected(String match){return ToothPrecondition.parse(match,null).version();}
 private void lock(UUID id,UUID clinic,String match){long version=expected(match);var rows=jdbc.queryForList("select version from odontogram_condition where id=? and clinic_id=? for update",id,clinic);if(rows.isEmpty())throw new OdontogramMutationException(404,"Condition unavailable in this clinic");if(((Number)rows.get(0).get("version")).longValue()!=version)throw new OdontogramMutationException(412,"Condition configuration changed. Reload and review before saving.");}
 @Transactional public ManagedOdontogramConditionResponse create(CreateOdontogramConditionRequest r){
  UUID clinic=clinic(r.getClinicId()),id=UUID.randomUUID(),actor=SecurityUtils.getCurrentUser().getId();
  String code=r.getConditionCode()==null||r.getConditionCode().isBlank()?"CUSTOM_"+id.toString().replace("-",""):r.getConditionCode().trim().toUpperCase(Locale.ROOT);
  try{jdbc.update("insert into odontogram_condition(id,clinic_id,condition_code,condition_name,description,patient_explanation,created_by,updated_by) values(?,?,?,?,?,?,?,?)",id,clinic,code,r.getConditionName().trim(),normalize(r.getDescription()),normalize(r.getPatientExplanation()),actor,actor);
   colour(id,clinic,r.getColourHex()==null?"#757575":r.getColourHex(),0,true,actor);
  }catch(DataIntegrityViolationException e){throw new OdontogramMutationException(409,"An active condition name or condition code already exists. Reload the catalogue.");}
  return read(id,clinic);
 }
 @Transactional public ManagedOdontogramConditionResponse update(UUID id,UUID requested,String match,UpdateOdontogramConditionRequest r){
  UUID clinic=clinic(requested);lock(id,clinic,match);var old=read(id,clinic);
  if(!old.conditionName().equals(r.conditionName().trim())&&!r.terminologyOnlyConfirmation())throw new OdontogramMutationException(409,"Confirm this is a terminology correction only. A different clinical meaning requires a new condition.");
  UUID actor=SecurityUtils.getCurrentUser().getId();
  try{jdbc.update("update odontogram_condition set condition_name=?,description=?,is_active=?,version=version+1,updated_at=localtimestamp,updated_by=? where id=? and clinic_id=?",r.conditionName().trim(),normalize(r.description()),r.active(),actor,id,clinic);
   // Preserve configured display order when editing through the unified form.
   Integer order=jdbc.query("select display_order from odontogram_colour_config where condition_id=? and clinic_id=?",(rs,n)->rs.getInt(1),id,clinic).stream().findFirst().orElse(0);
   colour(id,clinic,r.colourHex(),order,true,actor);
  }catch(DataIntegrityViolationException e){throw new OdontogramMutationException(409,"An active condition with this name already exists. Reload the catalogue.");}
  return read(id,clinic);
 }
 @Transactional public ManagedOdontogramConditionResponse colourOnly(SaveOdontogramColourConfigRequest r,String match){
  UUID clinic=clinic(r.getClinicId());lock(r.getConditionId(),clinic,match);UUID actor=SecurityUtils.getCurrentUser().getId();
  jdbc.update("update odontogram_condition set version=version+1,updated_at=localtimestamp,updated_by=? where id=? and clinic_id=?",actor,r.getConditionId(),clinic);
  colour(r.getConditionId(),clinic,r.getColourHex(),r.getDisplayOrder(),r.getActive()==null||r.getActive(),actor);return read(r.getConditionId(),clinic);
 }
 private void colour(UUID id,UUID clinic,String hex,int order,boolean active,UUID actor){jdbc.update("insert into odontogram_colour_config(clinic_id,condition_id,colour_hex,display_order,is_active,created_by,updated_by) values(?,?,?,?,?,?,?) on conflict(condition_id) do update set colour_hex=excluded.colour_hex,display_order=excluded.display_order,is_active=excluded.is_active,updated_at=localtimestamp,updated_by=excluded.updated_by",clinic,id,hex.toUpperCase(Locale.ROOT),order,active,actor,actor);}
}
