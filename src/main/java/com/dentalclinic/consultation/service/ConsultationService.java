package com.dentalclinic.consultation.service;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.appointment.entity.AppointmentStatus;
import com.dentalclinic.appointment.repository.AppointmentRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.consultation.dto.CreateConsultationRequest;
import com.dentalclinic.consultation.dto.ConsultationResponse;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    public ConsultationResponse createConsultation(
            CreateConsultationRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        /*
         * Load appointment with patient,
         * doctor and doctor user.
         */
        Appointment appointment =
                appointmentRepository
                        .findByIdAndClinicIdWithDetails(
                                request.getAppointmentId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        /*
         * Consultation can start only after the
         * appointment has entered consultation.
         */
        if (appointment.getStatus()
                != AppointmentStatus.IN_CONSULTATION) {

            throw new IllegalArgumentException(
                    "Appointment must be IN_CONSULTATION before starting consultation"
            );
        }

        /*
         * V5 allows only one consultation
         * per appointment.
         */
        if (consultationRepository
                .existsByAppointmentId(
                        appointment.getId()
                )) {

            throw new IllegalArgumentException(
                    "Consultation already exists for this appointment"
            );
        }

        Patient patient =
                appointment.getPatient();

        DoctorProfile doctor =
                appointment.getDoctor();

        /*
         * Use the appointment consultation timestamp
         * so appointment and consultation remain aligned.
         */
        LocalDateTime startedAt =
                appointment.getConsultationStartedAt() != null
                        ? appointment.getConsultationStartedAt()
                        : LocalDateTime.now();

        Consultation consultation =
                Consultation.builder()
                        .clinic(clinic)
                        .appointment(appointment)
                        .patient(patient)
                        .doctor(doctor)
                        .status(
                                ConsultationStatus.STARTED
                        )
                        .startedAt(startedAt)
                        .createdBy(currentUser)
                        .updatedBy(currentUser)
                        .build();

        consultation =
                consultationRepository.save(
                        consultation
                );

        return mapToResponse(
                consultation
        );
    }

    private ConsultationResponse mapToResponse(
            Consultation consultation
    ) {

        Appointment appointment =
                consultation.getAppointment();

        Patient patient =
                consultation.getPatient();

        DoctorProfile doctor =
                consultation.getDoctor();

        AppUser doctorUser =
                doctor.getUser();

        return ConsultationResponse.builder()
                .id(
                        consultation.getId()
                )
                .clinicId(
                        consultation
                                .getClinic()
                                .getId()
                )
                .appointmentId(
                        appointment.getId()
                )
                .appointmentNumber(
                        appointment
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
                .status(
                        consultation.getStatus()
                )
                .startedAt(
                        consultation.getStartedAt()
                )
                .endedAt(
                        consultation.getEndedAt()
                )
                .createdAt(
                        consultation.getCreatedAt()
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

        if (lastName == null ||
                lastName.isBlank()) {

            return firstName;
        }

        return firstName +
                " " +
                lastName;
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultation(
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

        Consultation consultation =
                consultationRepository
                        .findByIdAndClinicIdWithDetails(
                                consultationId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consultation not found"
                                )
                        );

        return mapToResponse(consultation);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationByAppointment(
            UUID appointmentId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        Consultation consultation =
                consultationRepository
                        .findByAppointmentIdAndClinicIdWithDetails(
                                appointmentId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Consultation not found for this appointment"
                                )
                        );

        return mapToResponse(consultation);
    }
}