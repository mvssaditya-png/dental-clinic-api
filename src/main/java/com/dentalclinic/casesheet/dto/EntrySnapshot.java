package com.dentalclinic.casesheet.dto;
import java.util.UUID;
import java.time.LocalDateTime;
public record EntrySnapshot(UUID id, UUID caseSheetId, String toothNumber,
 String diagnosisName, String diagnosisType, String diagnosisCode,
 String findingType, String findingValue, String notes, long version,
 LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime removedAt, UUID removedBy) {}
