package com.dentalclinic.casesheet.repository;

import com.dentalclinic.casesheet.entity.CaseSheetDiagnosis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseSheetDiagnosisRepository
        extends JpaRepository<CaseSheetDiagnosis, UUID> {

    List<CaseSheetDiagnosis>
    findAllByCaseSheetIdAndClinicIdAndRemovedAtIsNullOrderByCreatedAtAsc(
            UUID caseSheetId, UUID clinicId
    );

    boolean existsByCaseSheetIdAndClinicIdAndRemovedAtIsNull(UUID caseSheetId, UUID clinicId);
    java.util.Optional<CaseSheetDiagnosis> findByIdAndCaseSheetIdAndClinicId(UUID id, UUID caseSheetId, UUID clinicId);
}
