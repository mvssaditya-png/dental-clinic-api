package com.dentalclinic.casesheet.service;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.casesheet.dto.*;
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.casesheet.entity.CaseSheetDiagnosis;
import com.dentalclinic.casesheet.entity.CaseSheetStatus;
import com.dentalclinic.casesheet.entity.DiagnosisType;
import com.dentalclinic.casesheet.repository.CaseSheetClinicalFindingRepository;
import com.dentalclinic.casesheet.repository.CaseSheetDiagnosisRepository;
import com.dentalclinic.casesheet.repository.CaseSheetRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.consultation.entity.Consultation;
import com.dentalclinic.consultation.entity.ConsultationStatus;
import com.dentalclinic.consultation.repository.ConsultationRepository;
import com.dentalclinic.doctor.entity.DoctorProfile;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import com.dentalclinic.casesheet.dto.CreateCaseSheetClinicalFindingRequest;
import com.dentalclinic.casesheet.dto.CaseSheetClinicalFindingResponse;
import com.dentalclinic.casesheet.entity.CaseSheetClinicalFinding;
import com.dentalclinic.casesheet.repository.CaseSheetClinicalFindingRepository;
@Service
@RequiredArgsConstructor
public class CaseSheetService {

    private final CaseSheetRepository caseSheetRepository;
    private final ConsultationRepository consultationRepository;
    private final ClinicRepository clinicRepository;
    private final CaseSheetDiagnosisRepository caseSheetDiagnosisRepository;
    private final CaseSheetClinicalFindingRepository
            caseSheetClinicalFindingRepository;
    @Transactional
    public CaseSheetResponse createCaseSheet(
            CreateCaseSheetRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        /*
         * Load consultation with appointment,
         * patient, doctor, doctor user and clinic.
         */
        Consultation consultation =
                consultationRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getConsultationId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consultation not found"
                                )
                        );

        /*
         * We should not create a clinical record
         * for an already completed/cancelled encounter.
         */
        if (consultation.getStatus()
                == ConsultationStatus.COMPLETED
                ||
                consultation.getStatus()
                        == ConsultationStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Case sheet cannot be created for a completed or cancelled consultation"
            );
        }

        /*
         * V5 guarantees one case sheet
         * per consultation.
         */
        if (caseSheetRepository
                .existsByConsultationId(
                        consultation.getId()
                )) {

            throw new IllegalArgumentException(
                    "Case sheet already exists for this consultation"
            );
        }

        Appointment appointment =
                consultation.getAppointment();

        Patient patient =
                consultation.getPatient();

        DoctorProfile doctor =
                consultation.getDoctor();

        String caseSheetNumber =
                generateCaseSheetNumber(
                        clinic.getId()
                );

        /*
         * Visit #1 means first recorded case sheet
         * for the patient, #2 second visit, etc.
         */
        int visitNumber =
                (int) caseSheetRepository
                        .countByPatientId(
                                patient.getId()
                        ) + 1;

        CaseSheet caseSheet =
                CaseSheet.builder()
                        .clinic(clinic)
                        .consultation(consultation)
                        .appointment(appointment)
                        .patient(patient)
                        .doctor(doctor)

                        /*
                         * Appointment already contains
                         * the department selected for
                         * the encounter, if any.
                         */
                        .department(
                                appointment.getDepartment()
                        )

                        .caseSheetNumber(
                                caseSheetNumber
                        )
                        .visitNumber(
                                visitNumber
                        )
                        .status(
                                CaseSheetStatus.DRAFT
                        )
                        .createdBy(currentUser)
                        .updatedBy(currentUser)
                        .build();

        caseSheet =
                caseSheetRepository.save(
                        caseSheet
                );

        return mapToResponse(caseSheet);
    }

    private String generateCaseSheetNumber(
            UUID clinicId
    ) {

        long nextNumber =
                caseSheetRepository
                        .countByClinicId(clinicId)
                        + 1;

        String caseSheetNumber;

        do {

            caseSheetNumber =
                    String.format(
                            "CS%06d",
                            nextNumber
                    );

            nextNumber++;

        } while (
                caseSheetRepository
                        .existsByClinicIdAndCaseSheetNumber(
                                clinicId,
                                caseSheetNumber
                        )
        );

        return caseSheetNumber;
    }

    private CaseSheetResponse mapToResponse(
            CaseSheet caseSheet
    ) {

        Patient patient =
                caseSheet.getPatient();

        DoctorProfile doctor =
                caseSheet.getDoctor();

        AppUser doctorUser =
                doctor.getUser();

        return CaseSheetResponse.builder()

                .id(
                        caseSheet.getId()
                )

                .clinicId(
                        caseSheet
                                .getClinic()
                                .getId()
                )

                .consultationId(
                        caseSheet
                                .getConsultation()
                                .getId()
                )

                .appointmentId(
                        caseSheet
                                .getAppointment()
                                .getId()
                )

                .appointmentNumber(
                        caseSheet
                                .getAppointment()
                                .getAppointmentNumber()
                )

                .patientId(
                        patient.getId()
                )

                .patientNumber(
                        patient.getPatientNumber()
                )

                .patientName(
                        buildName(
                                patient.getFirstName(),
                                patient.getLastName()
                        )
                )

                .doctorId(
                        doctor.getId()
                )

                .doctorName(
                        buildName(
                                doctorUser.getFirstName(),
                                doctorUser.getLastName()
                        )
                )

                .departmentId(
                        caseSheet.getDepartment() != null
                                ? caseSheet
                                  .getDepartment()
                                  .getId()
                                : null
                )

                .caseSheetNumber(
                        caseSheet.getCaseSheetNumber()
                )

                .visitNumber(
                        caseSheet.getVisitNumber()
                )

                .chiefComplaint(
                        caseSheet.getChiefComplaint()
                )

                .historyOfPresentIllness(
                        caseSheet.getHistoryOfPresentIllness()
                )

                .clinicalExamination(
                        caseSheet.getClinicalExamination()
                )

                .diagnosisSummary(
                        caseSheet.getDiagnosisSummary()
                )

                .treatmentNotes(
                        caseSheet.getTreatmentNotes()
                )

                .status(
                        caseSheet.getStatus()
                )

                .finalizedAt(
                        caseSheet.getFinalizedAt()
                )

                .createdAt(
                        caseSheet.getCreatedAt()
                )

                .updatedAt(
                        caseSheet.getUpdatedAt()
                )

                .build();
    }

    private Clinic resolveClinic(
            AppUser currentUser,
            UUID requestedClinicId
    ) {

        if (currentUser.getClinic() != null) {
            return currentUser.getClinic();
        }

        if (requestedClinicId == null) {
            throw new IllegalArgumentException(
                    "clinicId is required for platform users"
            );
        }

        return clinicRepository
                .findById(requestedClinicId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Clinic not found"
                        )
                );
    }

    private String buildName(
            String firstName,
            String lastName
    ) {

        if (lastName == null
                || lastName.isBlank()) {

            return firstName;
        }

        return firstName
                + " "
                + lastName;
    }

    @Transactional
    public CaseSheetResponse updateCaseSheet(
            UUID caseSheetId,
            UUID requestedClinicId,
            UpdateCaseSheetRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                caseSheetId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found"
                                )
                        );

        if (caseSheet.getStatus()
                != CaseSheetStatus.DRAFT) {

            throw new IllegalArgumentException(
                    "Only DRAFT case sheets can be edited"
            );
        }

        caseSheet.setChiefComplaint(
                normalize(request.getChiefComplaint())
        );

        caseSheet.setHistoryOfPresentIllness(
                normalize(
                        request.getHistoryOfPresentIllness()
                )
        );

        caseSheet.setClinicalExamination(
                normalize(
                        request.getClinicalExamination()
                )
        );

        caseSheet.setDiagnosisSummary(
                normalize(
                        request.getDiagnosisSummary()
                )
        );

        caseSheet.setTreatmentNotes(
                normalize(
                        request.getTreatmentNotes()
                )
        );

        caseSheet.setUpdatedBy(currentUser);

        caseSheet =
                caseSheetRepository.save(caseSheet);

        return mapToResponse(caseSheet);
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    @Transactional(readOnly = true)
    public CaseSheetResponse getCaseSheet(
            UUID caseSheetId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                caseSheetId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found"
                                )
                        );

        return mapToResponse(caseSheet);
    }

    @Transactional(readOnly = true)
    public CaseSheetResponse getCaseSheetByConsultation(
            UUID consultationId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByConsultationIdAndClinicIdWithDetails(
                                consultationId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found for this consultation"
                                )
                        );

        return mapToResponse(caseSheet);
    }

    @Transactional
    public CaseSheetDiagnosisResponse addDiagnosis(
            UUID caseSheetId,
            UUID requestedClinicId,
            CreateCaseSheetDiagnosisRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                caseSheetId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found"
                                )
                        );

        if (caseSheet.getStatus()
                != CaseSheetStatus.DRAFT) {

            throw new IllegalArgumentException(
                    "Diagnoses can only be added to DRAFT case sheets"
            );
        }

        CaseSheetDiagnosis diagnosis =
                CaseSheetDiagnosis.builder()
                        .clinic(clinic)
                        .caseSheet(caseSheet)
                        .toothNumber(
                                normalize(
                                        request.getToothNumber()
                                )
                        )
                        .diagnosisCode(
                                normalize(
                                        request.getDiagnosisCode()
                                )
                        )
                        .diagnosisName(
                                request.getDiagnosisName().trim()
                        )
                        .diagnosisType(
                                request.getDiagnosisType() != null
                                        ? request.getDiagnosisType()
                                        : DiagnosisType.PRIMARY
                        )
                        .notes(
                                normalize(
                                        request.getNotes()
                                )
                        )
                        .createdBy(currentUser)
                        .build();

        diagnosis =
                caseSheetDiagnosisRepository
                        .save(diagnosis);

        return mapDiagnosisResponse(diagnosis);
    }

    @Transactional(readOnly = true)
    public List<CaseSheetDiagnosisResponse> getDiagnoses(
            UUID caseSheetId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                caseSheetId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found"
                                )
                        );

        return caseSheetDiagnosisRepository
                .findAllByCaseSheetIdOrderByCreatedAtAsc(
                        caseSheet.getId()
                )
                .stream()
                .map(this::mapDiagnosisResponse)
                .toList();
    }

    private CaseSheetDiagnosisResponse mapDiagnosisResponse(
            CaseSheetDiagnosis diagnosis
    ) {

        return CaseSheetDiagnosisResponse.builder()
                .id(diagnosis.getId())
                .caseSheetId(
                        diagnosis
                                .getCaseSheet()
                                .getId()
                )
                .toothNumber(
                        diagnosis.getToothNumber()
                )
                .diagnosisCode(
                        diagnosis.getDiagnosisCode()
                )
                .diagnosisName(
                        diagnosis.getDiagnosisName()
                )
                .diagnosisType(
                        diagnosis.getDiagnosisType()
                )
                .notes(
                        diagnosis.getNotes()
                )
                .createdAt(
                        diagnosis.getCreatedAt()
                )
                .build();
    }

    @Transactional
    public CaseSheetClinicalFindingResponse addClinicalFinding(
            UUID caseSheetId,
            UUID requestedClinicId,
            CreateCaseSheetClinicalFindingRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                caseSheetId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found"
                                )
                        );

        if (caseSheet.getStatus()
                != CaseSheetStatus.DRAFT) {

            throw new IllegalArgumentException(
                    "Clinical findings can only be added to DRAFT case sheets"
            );
        }

        CaseSheetClinicalFinding finding =
                CaseSheetClinicalFinding.builder()
                        .clinic(clinic)
                        .caseSheet(caseSheet)
                        .toothNumber(
                                normalize(
                                        request.getToothNumber()
                                )
                        )
                        .findingType(
                                request
                                        .getFindingType()
                                        .trim()
                        )
                        .findingValue(
                                normalize(
                                        request.getFindingValue()
                                )
                        )
                        .notes(
                                normalize(
                                        request.getNotes()
                                )
                        )
                        .createdBy(currentUser)
                        .build();

        finding =
                caseSheetClinicalFindingRepository
                        .save(finding);

        return mapClinicalFindingResponse(
                finding
        );
    }

    @Transactional(readOnly = true)
    public List<CaseSheetClinicalFindingResponse>
    getClinicalFindings(
            UUID caseSheetId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                caseSheetId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Case sheet not found"
                                )
                        );

        return caseSheetClinicalFindingRepository
                .findAllByCaseSheetIdOrderByCreatedAtAsc(
                        caseSheet.getId()
                )
                .stream()
                .map(
                        this::mapClinicalFindingResponse
                )
                .toList();
    }

    private CaseSheetClinicalFindingResponse
    mapClinicalFindingResponse(
            CaseSheetClinicalFinding finding
    ) {

        return CaseSheetClinicalFindingResponse
                .builder()
                .id(
                        finding.getId()
                )
                .caseSheetId(
                        finding
                                .getCaseSheet()
                                .getId()
                )
                .toothNumber(
                        finding.getToothNumber()
                )
                .findingType(
                        finding.getFindingType()
                )
                .findingValue(
                        finding.getFindingValue()
                )
                .notes(
                        finding.getNotes()
                )
                .createdAt(
                        finding.getCreatedAt()
                )
                .build();
    }
}