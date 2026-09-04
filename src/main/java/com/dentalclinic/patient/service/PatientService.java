package com.dentalclinic.patient.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.patient.dto.CreatePatientRequest;
import com.dentalclinic.patient.dto.MedicalHistoryRequest;
import com.dentalclinic.patient.dto.PatientResponse;
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
}