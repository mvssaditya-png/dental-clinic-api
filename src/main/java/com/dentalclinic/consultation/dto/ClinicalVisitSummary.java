package com.dentalclinic.consultation.dto;

import com.dentalclinic.consultation.entity.ConsultationStatus;
import com.dentalclinic.casesheet.entity.CaseSheetStatus;
import java.time.*;
import java.util.UUID;

public record ClinicalVisitSummary(UUID consultationId, ConsultationStatus consultationStatus,
        LocalDateTime startedAt, LocalDateTime endedAt, UUID appointmentId, String appointmentNumber,
        LocalDate appointmentDate, LocalTime appointmentStartTime, UUID doctorId, String doctorName,
        UUID caseSheetId, String caseSheetNumber, CaseSheetStatus caseSheetStatus,
        Integer visitNumber, String chiefComplaint) {}
