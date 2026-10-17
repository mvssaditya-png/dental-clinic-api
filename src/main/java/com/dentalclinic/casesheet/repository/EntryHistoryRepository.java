package com.dentalclinic.casesheet.repository;
import com.dentalclinic.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface EntryHistoryRepository extends JpaRepository<AuditLog,UUID> {
 @Query(value="""
 SELECT a.* FROM audit_log a WHERE a.clinic_id=:clinicId AND a.module='CASE_SHEET_ENTRIES'
 AND a.entity_type IN ('DIAGNOSIS','FINDING') AND a.action IN ('CREATE','UPDATE','REMOVE')
 AND a.new_value->>'caseSheetId'=:parentId
 AND ((a.entity_type='DIAGNOSIS' AND EXISTS (SELECT 1 FROM case_sheet_diagnosis d WHERE d.id=a.entity_id AND d.clinic_id=a.clinic_id AND d.case_sheet_id=CAST(:parentId AS uuid)))
 OR (a.entity_type='FINDING' AND EXISTS (SELECT 1 FROM case_sheet_clinical_finding f WHERE f.id=a.entity_id AND f.clinic_id=a.clinic_id AND f.case_sheet_id=CAST(:parentId AS uuid))))
 ORDER BY a.created_at DESC,a.id DESC
 """,countQuery="""
 SELECT count(*) FROM audit_log a WHERE a.clinic_id=:clinicId AND a.module='CASE_SHEET_ENTRIES'
 AND a.entity_type IN ('DIAGNOSIS','FINDING') AND a.action IN ('CREATE','UPDATE','REMOVE')
 AND a.new_value->>'caseSheetId'=:parentId
 AND ((a.entity_type='DIAGNOSIS' AND EXISTS (SELECT 1 FROM case_sheet_diagnosis d WHERE d.id=a.entity_id AND d.clinic_id=a.clinic_id AND d.case_sheet_id=CAST(:parentId AS uuid)))
 OR (a.entity_type='FINDING' AND EXISTS (SELECT 1 FROM case_sheet_clinical_finding f WHERE f.id=a.entity_id AND f.clinic_id=a.clinic_id AND f.case_sheet_id=CAST(:parentId AS uuid))))
 """,nativeQuery=true)
 Page<AuditLog> history(@Param("clinicId") UUID clinicId,@Param("parentId") String parentId,Pageable pageable);
}
