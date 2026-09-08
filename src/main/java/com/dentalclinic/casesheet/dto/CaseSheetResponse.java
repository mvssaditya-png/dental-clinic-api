package com.dentalclinic.casesheet.dto;

import com.dentalclinic.casesheet.entity.CaseSheetStatus;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CaseSheetResponse {

    private UUID id;

    private UUID clinicId;

    private UUID consultationId;

    private UUID appointmentId;
    private String appointmentNumber;

    private UUID patientId;
    private String patientNumber;
    private String patientName;

    private UUID doctorId;
    private String doctorName;

    private UUID departmentId;

    private String caseSheetNumber;
    private Integer visitNumber;

    private String chiefComplaint;
    private String historyOfPresentIllness;
    private String clinicalExamination;
    private String diagnosisSummary;
    private String treatmentNotes;

    private CaseSheetStatus status;

    private LocalDateTime finalizedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}