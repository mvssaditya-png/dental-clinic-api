package com.dentalclinic.odontogram.dto;

import com.dentalclinic.odontogram.entity.OdontogramChangeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class OdontogramToothHistoryResponse {

    private UUID id;

    private String toothNumber;

    private UUID previousConditionId;
    private String previousConditionCode;
    private String previousConditionName;

    private UUID newConditionId;
    private String newConditionCode;
    private String newConditionName;

    private String previousNotes;
    private String newNotes;

    private UUID caseSheetId;
    private UUID appointmentId;
    private UUID consultationId;

    private OdontogramChangeType changeType;

    private String changeReason;

    private UUID changedBy;

    private LocalDateTime changedAt;
}