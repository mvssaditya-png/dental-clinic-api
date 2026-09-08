package com.dentalclinic.odontogram.service;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.casesheet.entity.CaseSheet;
import com.dentalclinic.casesheet.entity.CaseSheetStatus;
import com.dentalclinic.casesheet.repository.CaseSheetRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.consultation.entity.Consultation;
import com.dentalclinic.odontogram.dto.*;
import com.dentalclinic.odontogram.entity.*;
import com.dentalclinic.odontogram.repository.OdontogramConditionRepository;
import com.dentalclinic.odontogram.repository.OdontogramRepository;
import com.dentalclinic.odontogram.repository.OdontogramToothHistoryRepository;
import com.dentalclinic.odontogram.repository.OdontogramToothRepository;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.patient.repository.PatientRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OdontogramService {

    private final OdontogramRepository odontogramRepository;
    private final PatientRepository patientRepository;
    private final ClinicRepository clinicRepository;
    private final OdontogramConditionRepository
            odontogramConditionRepository;
    private final OdontogramToothRepository odontogramToothRepository;

    private final OdontogramToothHistoryRepository
            odontogramToothHistoryRepository;

    private final CaseSheetRepository caseSheetRepository;
    @Transactional
    public OdontogramResponse createOdontogram(
            UUID patientId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        Patient patient =
                patientRepository
                        .findByIdAndClinicId(
                                patientId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Patient not found"
                                )
                        );

        if (odontogramRepository
                .existsByPatientId(patient.getId())) {

            throw new IllegalArgumentException(
                    "Odontogram already exists for this patient"
            );
        }

        Odontogram odontogram =
                Odontogram.builder()
                        .clinic(clinic)
                        .patient(patient)
                        .status(
                                OdontogramStatus.ACTIVE
                        )
                        .createdBy(currentUser)
                        .updatedBy(currentUser)
                        .build();

        odontogram =
                odontogramRepository.save(
                        odontogram
                );

        return mapToResponse(odontogram);
    }

    @Transactional(readOnly = true)
    public OdontogramResponse getOdontogramByPatient(
            UUID patientId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        // Validate that the requested patient belongs
        // to the resolved clinic.
        patientRepository
                .findByIdAndClinicId(
                        patientId,
                        clinic.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        )
                );

        Odontogram odontogram =
                odontogramRepository
                        .findByPatientIdAndClinicId(
                                patientId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Odontogram not found for this patient"
                                )
                        );

        return mapToResponse(odontogram);
    }

    private OdontogramResponse mapToResponse(
            Odontogram odontogram
    ) {

        Patient patient =
                odontogram.getPatient();

        return OdontogramResponse.builder()
                .id(odontogram.getId())
                .clinicId(
                        odontogram
                                .getClinic()
                                .getId()
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
                .status(
                        odontogram.getStatus()
                )
                .createdAt(
                        odontogram.getCreatedAt()
                )
                .updatedAt(
                        odontogram.getUpdatedAt()
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
    public OdontogramConditionResponse createCondition(
            CreateOdontogramConditionRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        String conditionCode =
                request
                        .getConditionCode()
                        .trim()
                        .toUpperCase();

        if (odontogramConditionRepository
                .existsByClinicIdAndConditionCode(
                        clinic.getId(),
                        conditionCode
                )) {

            throw new IllegalArgumentException(
                    "Odontogram condition already exists"
            );
        }

        OdontogramCondition condition =
                OdontogramCondition.builder()
                        .clinic(clinic)
                        .conditionCode(conditionCode)
                        .conditionName(
                                request
                                        .getConditionName()
                                        .trim()
                        )
                        .description(
                                normalize(
                                        request.getDescription()
                                )
                        )
                        .patientExplanation(
                                normalize(
                                        request.getPatientExplanation()
                                )
                        )
                        .active(true)
                        .createdBy(currentUser)
                        .updatedBy(currentUser)
                        .build();

        condition =
                odontogramConditionRepository
                        .save(condition);

        return mapConditionResponse(condition);
    }

    @Transactional(readOnly = true)
    public List<OdontogramConditionResponse> getConditions(
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        return odontogramConditionRepository
                .findAllByClinicIdAndActiveTrueOrderByConditionNameAsc(
                        clinic.getId()
                )
                .stream()
                .map(this::mapConditionResponse)
                .toList();
    }

    private OdontogramConditionResponse mapConditionResponse(
            OdontogramCondition condition
    ) {

        return OdontogramConditionResponse.builder()
                .id(condition.getId())
                .clinicId(
                        condition
                                .getClinic()
                                .getId()
                )
                .conditionCode(
                        condition.getConditionCode()
                )
                .conditionName(
                        condition.getConditionName()
                )
                .description(
                        condition.getDescription()
                )
                .patientExplanation(
                        condition.getPatientExplanation()
                )
                .active(
                        condition.getActive()
                )
                .createdAt(
                        condition.getCreatedAt()
                )
                .updatedAt(
                        condition.getUpdatedAt()
                )
                .build();
    }

    private String normalize(String value) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    @Transactional
    public OdontogramToothResponse updateTooth(
            UUID odontogramId,
            String toothNumber,
            UpdateOdontogramToothRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        String normalizedToothNumber =
                normalize(toothNumber);

        if (normalizedToothNumber == null) {
            throw new IllegalArgumentException(
                    "toothNumber is required"
            );
        }

        Odontogram odontogram =
                odontogramRepository
                        .findByIdAndClinicId(
                                odontogramId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Odontogram not found"
                                )
                        );

        if (odontogram.getStatus()
                != OdontogramStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Archived odontogram cannot be updated"
            );
        }

        CaseSheet caseSheet =
                caseSheetRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getCaseSheetId(),
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
                    "Odontogram can only be changed from a DRAFT case sheet"
            );
        }

        if (!caseSheet
                .getPatient()
                .getId()
                .equals(
                        odontogram
                                .getPatient()
                                .getId()
                )) {

            throw new IllegalArgumentException(
                    "Case sheet and odontogram belong to different patients"
            );
        }

        OdontogramCondition newCondition = null;

        if (request.getConditionId() != null) {

            newCondition =
                    odontogramConditionRepository
                            .findByIdAndClinicId(
                                    request.getConditionId(),
                                    clinic.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Odontogram condition not found"
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    newCondition.getActive()
            )) {

                throw new IllegalArgumentException(
                        "Odontogram condition is inactive"
                );
            }
        }

        String newNotes =
                normalize(
                        request.getNotes()
                );

        Appointment appointment =
                caseSheet.getAppointment();

        Consultation consultation =
                caseSheet.getConsultation();

        LocalDateTime now =
                LocalDateTime.now();

        OdontogramTooth tooth =
                odontogramToothRepository
                        .findByOdontogramIdAndToothNumber(
                                odontogramId,
                                normalizedToothNumber
                        )
                        .orElse(null);

        /*
         * FIRST TIME this tooth is recorded.
         */
        if (tooth == null) {

            if (newCondition == null
                    && newNotes == null) {

                throw new IllegalArgumentException(
                        "Condition or notes are required for a new tooth record"
                );
            }

            tooth =
                    OdontogramTooth.builder()
                            .clinic(clinic)
                            .odontogram(odontogram)
                            .toothNumber(
                                    normalizedToothNumber
                            )
                            .condition(
                                    newCondition
                            )
                            .notes(
                                    newNotes
                            )
                            .lastCaseSheet(
                                    caseSheet
                            )
                            .lastAppointment(
                                    appointment
                            )
                            .lastChangedAt(
                                    now
                            )
                            .lastChangedBy(
                                    currentUser
                            )
                            .build();

            tooth =
                    odontogramToothRepository
                            .save(tooth);

            createToothHistory(
                    clinic,
                    odontogram,
                    tooth,
                    null,
                    newCondition,
                    null,
                    newNotes,
                    caseSheet,
                    appointment,
                    consultation,
                    OdontogramChangeType.CREATED,
                    normalize(
                            request.getChangeReason()
                    ),
                    currentUser,
                    now
            );

            return mapToothResponse(tooth);
        }

        /*
         * Existing tooth.
         */
        OdontogramCondition previousCondition =
                tooth.getCondition();

        String previousNotes =
                tooth.getNotes();

        UUID previousConditionId =
                previousCondition != null
                        ? previousCondition.getId()
                        : null;

        UUID newConditionId =
                newCondition != null
                        ? newCondition.getId()
                        : null;

        if (Objects.equals(
                previousConditionId,
                newConditionId
        ) && Objects.equals(
                previousNotes,
                newNotes
        )) {

            throw new IllegalArgumentException(
                    "No odontogram changes detected"
            );
        }

        OdontogramChangeType changeType;

        if (newCondition == null
                && newNotes == null) {

            changeType =
                    OdontogramChangeType.CLEARED;

        } else {

            changeType =
                    OdontogramChangeType.UPDATED;
        }

        tooth.setCondition(
                newCondition
        );

        tooth.setNotes(
                newNotes
        );

        tooth.setLastCaseSheet(
                caseSheet
        );

        tooth.setLastAppointment(
                appointment
        );

        tooth.setLastChangedAt(
                now
        );

        tooth.setLastChangedBy(
                currentUser
        );

        tooth =
                odontogramToothRepository
                        .save(tooth);

        createToothHistory(
                clinic,
                odontogram,
                tooth,
                previousCondition,
                newCondition,
                previousNotes,
                newNotes,
                caseSheet,
                appointment,
                consultation,
                changeType,
                normalize(
                        request.getChangeReason()
                ),
                currentUser,
                now
        );

        return mapToothResponse(tooth);
    }

    private void createToothHistory(
            Clinic clinic,
            Odontogram odontogram,
            OdontogramTooth tooth,
            OdontogramCondition previousCondition,
            OdontogramCondition newCondition,
            String previousNotes,
            String newNotes,
            CaseSheet caseSheet,
            Appointment appointment,
            Consultation consultation,
            OdontogramChangeType changeType,
            String changeReason,
            AppUser changedBy,
            LocalDateTime changedAt
    ) {

        OdontogramToothHistory history =
                OdontogramToothHistory.builder()
                        .clinic(clinic)
                        .odontogram(odontogram)
                        .odontogramTooth(tooth)
                        .toothNumber(
                                tooth.getToothNumber()
                        )
                        .previousCondition(
                                previousCondition
                        )
                        .newCondition(
                                newCondition
                        )
                        .previousNotes(
                                previousNotes
                        )
                        .newNotes(
                                newNotes
                        )
                        .caseSheet(
                                caseSheet
                        )
                        .appointment(
                                appointment
                        )
                        .consultation(
                                consultation
                        )
                        .changeType(
                                changeType
                        )
                        .changeReason(
                                changeReason
                        )
                        .changedBy(
                                changedBy
                        )
                        .changedAt(
                                changedAt
                        )
                        .build();

        odontogramToothHistoryRepository
                .save(history);
    }

    private OdontogramToothResponse mapToothResponse(
            OdontogramTooth tooth
    ) {

        OdontogramCondition condition =
                tooth.getCondition();

        return OdontogramToothResponse.builder()
                .id(
                        tooth.getId()
                )
                .odontogramId(
                        tooth
                                .getOdontogram()
                                .getId()
                )
                .toothNumber(
                        tooth.getToothNumber()
                )
                .conditionId(
                        condition != null
                                ? condition.getId()
                                : null
                )
                .conditionCode(
                        condition != null
                                ? condition.getConditionCode()
                                : null
                )
                .conditionName(
                        condition != null
                                ? condition.getConditionName()
                                : null
                )
                .notes(
                        tooth.getNotes()
                )
                .lastCaseSheetId(
                        tooth.getLastCaseSheet() != null
                                ? tooth.getLastCaseSheet().getId()
                                : null
                )
                .lastAppointmentId(
                        tooth.getLastAppointment() != null
                                ? tooth.getLastAppointment().getId()
                                : null
                )
                .lastChangedAt(
                        tooth.getLastChangedAt()
                )
                .createdAt(
                        tooth.getCreatedAt()
                )
                .updatedAt(
                        tooth.getUpdatedAt()
                )
                .build();
    }

    @Transactional(readOnly = true)
    public List<OdontogramToothResponse> getTeeth(
            UUID odontogramId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        Odontogram odontogram =
                odontogramRepository
                        .findByIdAndClinicId(
                                odontogramId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Odontogram not found"
                                )
                        );

        return odontogramToothRepository
                .findAllByOdontogramIdOrderByToothNumberAsc(
                        odontogram.getId()
                )
                .stream()
                .map(this::mapToothResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OdontogramToothHistoryResponse> getToothHistory(
            UUID odontogramId,
            String toothNumber,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        Odontogram odontogram =
                odontogramRepository
                        .findByIdAndClinicId(
                                odontogramId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Odontogram not found"
                                )
                        );

        String normalizedToothNumber =
                normalize(toothNumber);

        if (normalizedToothNumber == null) {
            throw new IllegalArgumentException(
                    "toothNumber is required"
            );
        }

        return odontogramToothHistoryRepository
                .findAllByOdontogramIdAndToothNumberOrderByChangedAtDesc(
                        odontogram.getId(),
                        normalizedToothNumber
                )
                .stream()
                .map(this::mapToothHistoryResponse)
                .toList();
    }

    private OdontogramToothHistoryResponse mapToothHistoryResponse(
            OdontogramToothHistory history
    ) {

        OdontogramCondition previousCondition =
                history.getPreviousCondition();

        OdontogramCondition newCondition =
                history.getNewCondition();

        return OdontogramToothHistoryResponse.builder()

                .id(history.getId())

                .toothNumber(
                        history.getToothNumber()
                )

                .previousConditionId(
                        previousCondition != null
                                ? previousCondition.getId()
                                : null
                )

                .previousConditionCode(
                        previousCondition != null
                                ? previousCondition.getConditionCode()
                                : null
                )

                .previousConditionName(
                        previousCondition != null
                                ? previousCondition.getConditionName()
                                : null
                )

                .newConditionId(
                        newCondition != null
                                ? newCondition.getId()
                                : null
                )

                .newConditionCode(
                        newCondition != null
                                ? newCondition.getConditionCode()
                                : null
                )

                .newConditionName(
                        newCondition != null
                                ? newCondition.getConditionName()
                                : null
                )

                .previousNotes(
                        history.getPreviousNotes()
                )

                .newNotes(
                        history.getNewNotes()
                )

                .caseSheetId(
                        history.getCaseSheet() != null
                                ? history.getCaseSheet().getId()
                                : null
                )

                .appointmentId(
                        history.getAppointment() != null
                                ? history.getAppointment().getId()
                                : null
                )

                .consultationId(
                        history.getConsultation() != null
                                ? history.getConsultation().getId()
                                : null
                )

                .changeType(
                        history.getChangeType()
                )

                .changeReason(
                        history.getChangeReason()
                )

                .changedBy(
                        history.getChangedBy().getId()
                )

                .changedAt(
                        history.getChangedAt()
                )

                .build();
    }
}