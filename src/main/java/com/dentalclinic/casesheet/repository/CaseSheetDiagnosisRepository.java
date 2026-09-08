package com.dentalclinic.casesheet.repository;

import com.dentalclinic.casesheet.entity.CaseSheetDiagnosis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseSheetDiagnosisRepository
        extends JpaRepository<CaseSheetDiagnosis, UUID> {

    List<CaseSheetDiagnosis>
    findAllByCaseSheetIdOrderByCreatedAtAsc(
            UUID caseSheetId
    );
}