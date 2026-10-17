package com.dentalclinic.casesheet.dto;
import java.util.UUID;
import java.time.LocalDateTime;
public record EntryHistoryResponse(UUID id, UUID caseSheetId, String entryType, UUID entryId,
 String operation, UUID actorId, LocalDateTime occurredAt, EntrySnapshot before,
 EntrySnapshot after, String removalReason) {}
