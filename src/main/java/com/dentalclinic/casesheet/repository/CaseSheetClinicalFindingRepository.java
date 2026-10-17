package com.dentalclinic.casesheet.repository;

import com.dentalclinic.casesheet.entity.CaseSheetClinicalFinding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseSheetClinicalFindingRepository
        extends JpaRepository<CaseSheetClinicalFinding, UUID> {

    List<CaseSheetClinicalFinding>
    findAllByCaseSheetIdAndClinicIdAndRemovedAtIsNullOrderByCreatedAtAsc(
            UUID caseSheetId, UUID clinicId
    );
    java.util.Optional<CaseSheetClinicalFinding> findByIdAndCaseSheetIdAndClinicId(UUID id, UUID caseSheetId, UUID clinicId);
}
