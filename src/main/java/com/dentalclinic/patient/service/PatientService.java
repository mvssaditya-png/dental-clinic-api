package com.dentalclinic.patient.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.patient.dto.*;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.patient.entity.PatientActivity;
import com.dentalclinic.patient.entity.PatientMedicalHistory;
import com.dentalclinic.patient.repository.PatientActivityRepository;
import com.dentalclinic.patient.repository.PatientMedicalHistoryRepository;
import com.dentalclinic.patient.repository.PatientRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMedicalHistoryRepository medicalHistoryRepository;
    private final PatientActivityRepository patientActivityRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(currentUser, request.getClinicId());

        if (request.getAadhaarNumber() != null
                && !request.getAadhaarNumber().isBlank()
                && patientRepository.existsByClinicIdAndAadhaarNumber(
                clinic.getId(),
                request.getAadhaarNumber()
        )) {

            throw new IllegalArgumentException(
                    "A patient with this Aadhaar number already exists"
            );
        }

        String patientNumber = generatePatientNumber(clinic.getId());

        Patient patient = Patient.builder()
                .clinic(clinic)
                .patientNumber(patientNumber)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(request.getGender())
                .dob(request.getDob())
                .bloodGroup(request.getBloodGroup())
                .maritalStatus(request.getMaritalStatus())
                .occupation(request.getOccupation())
                .nationality(request.getNationality())
                .phone(request.getPhone())
                .email(request.getEmail())
                .aadhaarNumber(request.getAadhaarNumber())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .reasonForVisit(request.getReasonForVisit())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .active(true)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        patient = patientRepository.save(patient);

        if (request.getMedicalHistory() != null) {
            saveMedicalHistory(
                    patient,
                    clinic,
                    currentUser,
                    request.getMedicalHistory()
            );
        }

        savePatientActivity(
                patient,
                clinic,
                currentUser
        );

        return mapToResponse(patient);
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

        return clinicRepository.findById(requestedClinicId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Clinic not found"
                        )
                );
    }

    private void saveMedicalHistory(
            Patient patient,
            Clinic clinic,
            AppUser currentUser,
            MedicalHistoryRequest request
    ) {

        PatientMedicalHistory history =
                PatientMedicalHistory.builder()
                        .clinic(clinic)
                        .patient(patient)

                        .diabetes(Boolean.TRUE.equals(request.getDiabetes()))
                        .hypertension(Boolean.TRUE.equals(request.getHypertension()))
                        .thyroid(Boolean.TRUE.equals(request.getThyroid()))
                        .heartDisease(Boolean.TRUE.equals(request.getHeartDisease()))
                        .kidneyDisease(Boolean.TRUE.equals(request.getKidneyDisease()))
                        .pregnancy(Boolean.TRUE.equals(request.getPregnancy()))
                        .asthma(Boolean.TRUE.equals(request.getAsthma()))
                        .allergies(Boolean.TRUE.equals(request.getAllergies()))
                        .tobacco(Boolean.TRUE.equals(request.getTobacco()))
                        .alcohol(Boolean.TRUE.equals(request.getAlcohol()))
                        .smoking(Boolean.TRUE.equals(request.getSmoking()))

                        .allergyNotes(request.getAllergyNotes())
                        .otherConditions(request.getOtherConditions())

                        .createdBy(currentUser)
                        .updatedBy(currentUser)

                        .build();

        medicalHistoryRepository.save(history);
    }

    private void savePatientActivity(
            Patient patient,
            Clinic clinic,
            AppUser currentUser
    ) {

        PatientActivity activity =
                PatientActivity.builder()
                        .clinic(clinic)
                        .patient(patient)
                        .activityType("PATIENT_CREATED")
                        .referenceType("PATIENT")
                        .referenceId(patient.getId())
                        .title("Patient created")
                        .description(
                                "Patient profile created for "
                                        + patient.getFirstName()
                                        + " "
                                        + (patient.getLastName() != null
                                        ? patient.getLastName()
                                        : "")
                        )
                        .performedBy(currentUser)
                        .build();

        patientActivityRepository.save(activity);
    }

    private String generatePatientNumber(UUID clinicId) {

        String prefix = "PAT";

        long sequence =
                patientRepository.findAllByClinicIdAndActiveTrue(clinicId)
                        .size() + 1L;

        String patientNumber;

        do {
            patientNumber =
                    prefix + String.format("%05d", sequence);

            sequence++;

        } while (
                patientRepository.existsByClinicIdAndPatientNumber(
                        clinicId,
                        patientNumber
                )
        );

        return patientNumber;
    }

    private PatientResponse mapToResponse(Patient patient) {

        return PatientResponse.builder()
                .id(patient.getId())
                .patientNumber(patient.getPatientNumber())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .gender(patient.getGender())
                .dob(patient.getDob())
                .bloodGroup(patient.getBloodGroup())
                .maritalStatus(patient.getMaritalStatus())
                .occupation(patient.getOccupation())
                .nationality(patient.getNationality())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .aadhaarNumber(patient.getAadhaarNumber())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .reasonForVisit(patient.getReasonForVisit())
                .addressLine1(patient.getAddressLine1())
                .addressLine2(patient.getAddressLine2())
                .city(patient.getCity())
                .state(patient.getState())
                .country(patient.getCountry())
                .pincode(patient.getPincode())
                .active(patient.getActive())
                .clinicId(patient.getClinic().getId())
                .createdAt(patient.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> getPatients(
            UUID requestedClinicId,
            String search,
            int page,
            int size
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable =
                PageRequest.of(safePage, safeSize);

        String normalizedSearch =
                search == null
                        ? null
                        : search.trim();

        return patientRepository
                .searchPatients(
                        clinic.getId(),
                        normalizedSearch,
                        pageable
                )
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PatientDetailResponse getPatientById(
            UUID patientId,
            UUID requestedClinicId
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Patient patient = patientRepository
                .findByIdAndClinicId(
                        patientId,
                        clinic.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Patient not found"
                        )
                );

        PatientMedicalHistory medicalHistory =
                medicalHistoryRepository
                        .findByPatientId(patient.getId())
                        .orElse(null);

        return mapToDetailResponse(
                patient,
                medicalHistory
        );
    }

    private PatientDetailResponse mapToDetailResponse(
            Patient patient,
            PatientMedicalHistory history
    ) {

        return PatientDetailResponse.builder()
                .id(patient.getId())
                .patientNumber(patient.getPatientNumber())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .gender(patient.getGender())
                .dob(patient.getDob())
                .bloodGroup(patient.getBloodGroup())
                .maritalStatus(patient.getMaritalStatus())
                .occupation(patient.getOccupation())
                .nationality(patient.getNationality())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .aadhaarNumber(patient.getAadhaarNumber())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .reasonForVisit(patient.getReasonForVisit())
                .addressLine1(patient.getAddressLine1())
                .addressLine2(patient.getAddressLine2())
                .city(patient.getCity())
                .state(patient.getState())
                .country(patient.getCountry())
                .pincode(patient.getPincode())
                .active(patient.getActive())
                .clinicId(patient.getClinic().getId())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .medicalHistory(
                        history != null
                                ? mapMedicalHistory(history)
                                : null
                )
                .build();
    }

    private MedicalHistoryResponse mapMedicalHistory(
            PatientMedicalHistory history
    ) {

        return MedicalHistoryResponse.builder()
                .diabetes(history.getDiabetes())
                .hypertension(history.getHypertension())
                .thyroid(history.getThyroid())
                .heartDisease(history.getHeartDisease())
                .kidneyDisease(history.getKidneyDisease())
                .pregnancy(history.getPregnancy())
                .asthma(history.getAsthma())
                .allergies(history.getAllergies())
                .tobacco(history.getTobacco())
                .alcohol(history.getAlcohol())
                .smoking(history.getSmoking())
                .allergyNotes(history.getAllergyNotes())
                .otherConditions(history.getOtherConditions())
                .build();
    }

    @Transactional
    public PatientDetailResponse updatePatient(
            UUID patientId,
            UpdatePatientRequest request
    ) {

        AppUser currentUser = SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                request.getClinicId()
        );

        Patient patient = patientRepository
                .findByIdAndClinicId(
                        patientId,
                        clinic.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("Patient not found")
                );

        String aadhaarNumber =
                normalizeNullable(request.getAadhaarNumber());

        if (aadhaarNumber != null &&
                patientRepository
                        .existsByClinicIdAndAadhaarNumberAndIdNot(
                                clinic.getId(),
                                aadhaarNumber,
                                patientId
                        )) {

            throw new IllegalArgumentException(
                    "A patient with this Aadhaar number already exists"
            );
        }

        patient.setFirstName(request.getFirstName().trim());
        patient.setLastName(normalizeNullable(request.getLastName()));
        patient.setGender(request.getGender());
        patient.setDob(request.getDob());
        patient.setBloodGroup(normalizeNullable(request.getBloodGroup()));
        patient.setMaritalStatus(normalizeNullable(request.getMaritalStatus()));
        patient.setOccupation(normalizeNullable(request.getOccupation()));
        patient.setNationality(normalizeNullable(request.getNationality()));

        patient.setPhone(request.getPhone().trim());
        patient.setEmail(normalizeNullable(request.getEmail()));

        patient.setAadhaarNumber(aadhaarNumber);

        patient.setEmergencyContactName(
                normalizeNullable(request.getEmergencyContactName())
        );

        patient.setEmergencyContactPhone(
                normalizeNullable(request.getEmergencyContactPhone())
        );

        patient.setReasonForVisit(
                normalizeNullable(request.getReasonForVisit())
        );

        patient.setAddressLine1(
                normalizeNullable(request.getAddressLine1())
        );

        patient.setAddressLine2(
                normalizeNullable(request.getAddressLine2())
        );

        patient.setCity(normalizeNullable(request.getCity()));
        patient.setState(normalizeNullable(request.getState()));

        patient.setCountry(
                normalizeNullable(request.getCountry())
        );

        patient.setPincode(
                normalizeNullable(request.getPincode())
        );

        patient.setUpdatedBy(currentUser);

        patientRepository.save(patient);

        PatientMedicalHistory history =
                updateMedicalHistory(
                        patient,
                        clinic,
                        currentUser,
                        request.getMedicalHistory()
                );

        PatientActivity activity =
                PatientActivity.builder()
                        .clinic(clinic)
                        .patient(patient)
                        .activityType("PATIENT_UPDATED")
                        .referenceType("PATIENT")
                        .referenceId(patient.getId())
                        .title("Patient details updated")
                        .description(
                                "Patient profile or medical history was updated"
                        )
                        .performedBy(currentUser)
                        .build();

        patientActivityRepository.save(activity);

        return mapToDetailResponse(
                patient,
                history
        );
    }

    private PatientMedicalHistory updateMedicalHistory(
            Patient patient,
            Clinic clinic,
            AppUser currentUser,
            MedicalHistoryRequest request
    ) {

        PatientMedicalHistory history =
                medicalHistoryRepository
                        .findByPatientId(patient.getId())
                        .orElse(null);

        if (request == null) {
            return history;
        }

        if (history == null) {

            history = PatientMedicalHistory.builder()
                    .clinic(clinic)
                    .patient(patient)
                    .createdBy(currentUser)
                    .build();
        }

        history.setDiabetes(
                Boolean.TRUE.equals(request.getDiabetes())
        );

        history.setHypertension(
                Boolean.TRUE.equals(request.getHypertension())
        );

        history.setThyroid(
                Boolean.TRUE.equals(request.getThyroid())
        );

        history.setHeartDisease(
                Boolean.TRUE.equals(request.getHeartDisease())
        );

        history.setKidneyDisease(
                Boolean.TRUE.equals(request.getKidneyDisease())
        );

        history.setPregnancy(
                Boolean.TRUE.equals(request.getPregnancy())
        );

        history.setAsthma(
                Boolean.TRUE.equals(request.getAsthma())
        );

        history.setAllergies(
                Boolean.TRUE.equals(request.getAllergies())
        );

        history.setTobacco(
                Boolean.TRUE.equals(request.getTobacco())
        );

        history.setAlcohol(
                Boolean.TRUE.equals(request.getAlcohol())
        );

        history.setSmoking(
                Boolean.TRUE.equals(request.getSmoking())
        );

        history.setAllergyNotes(
                normalizeNullable(request.getAllergyNotes())
        );

        history.setOtherConditions(
                normalizeNullable(request.getOtherConditions())
        );

        history.setUpdatedBy(currentUser);

        return medicalHistoryRepository.save(history);
    }

    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}