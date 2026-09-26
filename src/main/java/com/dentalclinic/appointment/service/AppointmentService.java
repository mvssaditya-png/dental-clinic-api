package com.dentalclinic.appointment.service;

import com.dentalclinic.appointment.dto.AppointmentResponse;
import com.dentalclinic.appointment.dto.CreateAppointmentRequest;
import com.dentalclinic.appointment.dto.UpdateAppointmentStatusRequest;
import com.dentalclinic.appointment.entity.*;
import com.dentalclinic.appointment.repository.AppointmentRepository;
import com.dentalclinic.appointment.repository.AppointmentStatusHistoryRepository;
import com.dentalclinic.appointment.repository.DentalChairRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.consultation.entity.Consultation;
import com.dentalclinic.consultation.entity.ConsultationStatus;
import com.dentalclinic.consultation.repository.ConsultationRepository;
import com.dentalclinic.department.entity.Department;
import com.dentalclinic.department.repository.DepartmentRepository;
import com.dentalclinic.doctor.entity.DoctorProfile;
import com.dentalclinic.doctor.repository.DoctorProfileRepository;
import com.dentalclinic.patient.entity.Patient;
import com.dentalclinic.patient.repository.PatientRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusHistoryRepository statusHistoryRepository;

    private final PatientRepository patientRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ClinicRepository clinicRepository;
    private final ConsultationRepository consultationRepository;

    /*
     * Department + Dental Chair repositories.
     */
    private final DepartmentRepository departmentRepository;
    private final DentalChairRepository dentalChairRepository;

    @Transactional
    public AppointmentResponse createAppointment(
            CreateAppointmentRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                request.getClinicId()
        );

        /*
         * Validate time.
         */
        if (!request.getEndTime()
                .isAfter(request.getStartTime())) {

            throw new IllegalArgumentException(
                    "Appointment end time must be after start time"
            );
        }

        /*
         * Validate patient belongs to clinic.
         */
        Patient patient =
                patientRepository
                        .findByIdAndClinicId(
                                request.getPatientId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Patient not found"
                                )
                        );

        if (!Boolean.TRUE.equals(patient.getActive())) {
            throw new IllegalArgumentException(
                    "Patient is inactive"
            );
        }

        /*
         * Validate doctor belongs to same clinic.
         */
        DoctorProfile doctor =
                doctorProfileRepository
                        .findByIdAndClinicId(
                                request.getDoctorId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Doctor not found"
                                )
                        );

        if (!Boolean.TRUE.equals(doctor.getActive())) {
            throw new IllegalArgumentException(
                    "Doctor is inactive"
            );
        }

        /*
         * Validate optional department.
         *
         * The clinic is part of the lookup, so a department
         * belonging to another clinic cannot be attached.
         */
        Department department = null;

        if (request.getDepartmentId() != null) {

            department =
                    departmentRepository
                            .findByIdAndClinicId(
                                    request.getDepartmentId(),
                                    clinic.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Department not found"
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    department.getActive()
            )) {

                throw new IllegalArgumentException(
                        "Department is inactive"
                );
            }
        }

        /*
         * Validate optional dental chair.
         *
         * The clinic is part of the lookup, so a chair
         * belonging to another clinic cannot be attached.
         */
        DentalChair chair = null;

        if (request.getChairId() != null) {

            chair =
                    dentalChairRepository
                            .findByIdAndClinicId(
                                    request.getChairId(),
                                    clinic.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Dental chair not found"
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    chair.getActive()
            )) {

                throw new IllegalArgumentException(
                        "Dental chair is inactive"
                );
            }
        }

        /*
         * Prevent doctor double-booking.
         */
        boolean overlap =
                appointmentRepository.existsDoctorOverlap(
                        clinic.getId(),
                        doctor.getId(),
                        request.getAppointmentDate(),
                        request.getStartTime(),
                        request.getEndTime()
                );

        if (overlap) {
            throw new IllegalArgumentException(
                    "Doctor already has an appointment during this time"
            );
        }

        /*
         * Prevent dental chair double-booking.
         *
         * Only checked when a chair was selected.
         */
        if (chair != null) {

            boolean chairOverlap =
                    appointmentRepository.existsChairOverlap(
                            clinic.getId(),
                            chair.getId(),
                            request.getAppointmentDate(),
                            request.getStartTime(),
                            request.getEndTime()
                    );

            if (chairOverlap) {
                throw new IllegalArgumentException(
                        "Dental chair already has an appointment during this time"
                );
            }
        }

        int calculatedDuration =
                (int) Duration.between(
                        request.getStartTime(),
                        request.getEndTime()
                ).toMinutes();

        Integer duration =
                request.getEstimatedDurationMinutes() != null
                        ? request.getEstimatedDurationMinutes()
                        : calculatedDuration;

        String appointmentNumber =
                generateAppointmentNumber(
                        clinic.getId()
                );

        Appointment appointment =
                Appointment.builder()
                        .clinic(clinic)
                        .appointmentNumber(
                                appointmentNumber
                        )
                        .patient(patient)
                        .doctor(doctor)

                        /*
                         * Department and chair are optional.
                         */
                        .department(department)
                        .chair(chair)

                        .appointmentType(
                                request.getAppointmentType() != null
                                        ? request.getAppointmentType()
                                        : AppointmentType.NEW
                        )

                        .appointmentDate(
                                request.getAppointmentDate()
                        )

                        .startTime(
                                request.getStartTime()
                        )

                        .endTime(
                                request.getEndTime()
                        )

                        .estimatedDurationMinutes(
                                duration
                        )

                        .priority(
                                request.getPriority() != null
                                        ? request.getPriority()
                                        : AppointmentPriority.ROUTINE
                        )

                        .reason(
                                normalizeNullable(
                                        request.getReason()
                                )
                        )

                        .remarks(
                                normalizeNullable(
                                        request.getRemarks()
                                )
                        )

                        .status(
                                AppointmentStatus.SCHEDULED
                        )

                        .createdBy(currentUser)
                        .updatedBy(currentUser)

                        .build();

        appointment =
                appointmentRepository.save(
                        appointment
                );

        /*
         * Initial lifecycle history.
         *
         * NULL -> SCHEDULED
         */
        AppointmentStatusHistory history =
                AppointmentStatusHistory.builder()
                        .clinic(clinic)
                        .appointment(appointment)
                        .fromStatus(null)
                        .toStatus(
                                AppointmentStatus.SCHEDULED
                        )
                        .reason("Appointment created")
                        .changedBy(currentUser)
                        .build();

        statusHistoryRepository.save(history);

        return mapToResponse(appointment);
    }

    private String generateAppointmentNumber(
            UUID clinicId
    ) {

        long sequence =
                appointmentRepository
                        .findAll()
                        .stream()
                        .filter(a ->
                                a.getClinic()
                                        .getId()
                                        .equals(clinicId)
                        )
                        .count() + 1;

        String appointmentNumber;

        do {

            appointmentNumber =
                    "APT" +
                            String.format(
                                    "%06d",
                                    sequence
                            );

            sequence++;

        } while (
                appointmentRepository
                        .existsByClinicIdAndAppointmentNumber(
                                clinicId,
                                appointmentNumber
                        )
        );

        return appointmentNumber;
    }

    private AppointmentResponse mapToResponse(
            Appointment appointment
    ) {

        Patient patient =
                appointment.getPatient();

        DoctorProfile doctor =
                appointment.getDoctor();

        AppUser doctorUser =
                doctor.getUser();

        String patientName =
                buildName(
                        patient.getFirstName(),
                        patient.getLastName()
                );

        String doctorName =
                buildName(
                        doctorUser.getFirstName(),
                        doctorUser.getLastName()
                );

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .appointmentNumber(
                        appointment.getAppointmentNumber()
                )
                .clinicId(
                        appointment.getClinic().getId()
                )
                .patientId(patient.getId())
                .patientNumber(
                        patient.getPatientNumber()
                )
                .patientName(patientName)
                .doctorId(doctor.getId())
                .doctorName(doctorName)
                .doctorSpecialization(
                        doctor.getSpecialization()
                )
                .departmentId(
                        appointment.getDepartment() != null
                                ? appointment
                                  .getDepartment()
                                  .getId()
                                : null
                )
                .chairId(
                        appointment.getChair() != null
                                ? appointment
                                  .getChair()
                                  .getId()
                                : null
                )
                .appointmentType(
                        appointment.getAppointmentType()
                )
                .appointmentDate(
                        appointment.getAppointmentDate()
                )
                .startTime(
                        appointment.getStartTime()
                )
                .endTime(
                        appointment.getEndTime()
                )
                .estimatedDurationMinutes(
                        appointment
                                .getEstimatedDurationMinutes()
                )
                .priority(
                        appointment.getPriority()
                )
                .reason(
                        appointment.getReason()
                )
                .remarks(
                        appointment.getRemarks()
                )
                .status(
                        appointment.getStatus()
                )
                .createdAt(
                        appointment.getCreatedAt()
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

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private String buildName(
            String firstName,
            String lastName
    ) {

        if (lastName == null ||
                lastName.isBlank()) {

            return firstName;
        }

        return firstName + " " + lastName;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(
            UUID requestedClinicId,
            LocalDate date
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        if (date == null) {
            throw new IllegalArgumentException(
                    "Appointment date is required"
            );
        }

        return appointmentRepository
                .findDailyAppointmentsWithDetails(
                        clinic.getId(),
                        date
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(
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

        Appointment appointment =
                appointmentRepository
                        .findByIdAndClinicIdWithDetails(
                                appointmentId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        return mapToResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateAppointmentStatus(
            UUID appointmentId,
            UpdateAppointmentStatusRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        Appointment appointment =
                appointmentRepository
                        .findByIdAndClinicIdWithDetails(
                                appointmentId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Appointment not found"
                                )
                        );

        AppointmentStatus currentStatus =
                appointment.getStatus();

        AppointmentStatus newStatus =
                request.getStatus();

        if (currentStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Appointment is already in status " + newStatus
            );
        }

        validateStatusTransition(
                currentStatus,
                newStatus
        );

        /*
         * Appointment cannot be completed until its
         * consultation is completed.
         */
        if (newStatus == AppointmentStatus.COMPLETED) {

            Consultation consultation =
                    consultationRepository
                            .findByAppointmentIdAndClinicId(
                                    appointment.getId(),
                                    clinic.getId()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Appointment cannot be completed because consultation does not exist"
                                    )
                            );

            if (consultation.getStatus()
                    != ConsultationStatus.COMPLETED) {

                throw new IllegalArgumentException(
                        "Appointment cannot be completed until the consultation is completed"
                );
            }
        }

        /*
         * Cancellation reason is required when cancelling.
         */
        if (newStatus == AppointmentStatus.CANCELLED &&
                (request.getReason() == null ||
                        request.getReason().isBlank())) {

            throw new IllegalArgumentException(
                    "Cancellation reason is required"
            );
        }

        /*
         * Update lifecycle timestamps.
         */
        switch (newStatus) {

            case CHECKED_IN ->
                    appointment.setCheckedInAt(
                            java.time.LocalDateTime.now()
                    );

            case IN_CONSULTATION ->
                    appointment.setConsultationStartedAt(
                            java.time.LocalDateTime.now()
                    );

            case COMPLETED ->
                    appointment.setCompletedAt(
                            java.time.LocalDateTime.now()
                    );

            case CANCELLED -> {

                appointment.setCancelledAt(
                        java.time.LocalDateTime.now()
                );

                appointment.setCancellationReason(
                        request.getReason().trim()
                );
            }

            default -> {
                // No special timestamp required.
            }
        }

        appointment.setStatus(newStatus);
        appointment.setUpdatedBy(currentUser);

        appointment =
                appointmentRepository.save(
                        appointment
                );

        /*
         * Record every status transition.
         */
        AppointmentStatusHistory history =
                AppointmentStatusHistory.builder()
                        .clinic(clinic)
                        .appointment(appointment)
                        .fromStatus(currentStatus)
                        .toStatus(newStatus)
                        .reason(
                                normalizeNullable(
                                        request.getReason()
                                )
                        )
                        .changedBy(currentUser)
                        .build();

        statusHistoryRepository.save(history);

        return mapToResponse(appointment);
    }

    private void validateStatusTransition(
            AppointmentStatus currentStatus,
            AppointmentStatus newStatus
    ) {

        boolean valid =
                switch (currentStatus) {

                    case SCHEDULED ->
                            newStatus == AppointmentStatus.CHECKED_IN
                                    || newStatus == AppointmentStatus.CANCELLED
                                    || newStatus == AppointmentStatus.NO_SHOW
                                    || newStatus == AppointmentStatus.RESCHEDULED;

                    case CHECKED_IN ->
                            newStatus == AppointmentStatus.WAITING
                                    || newStatus == AppointmentStatus.IN_CONSULTATION
                                    || newStatus == AppointmentStatus.CANCELLED;

                    case WAITING ->
                            newStatus == AppointmentStatus.IN_CONSULTATION
                                    || newStatus == AppointmentStatus.CANCELLED;

                    case IN_CONSULTATION ->
                            newStatus == AppointmentStatus.COMPLETED;

                    /*
                     * Terminal states.
                     */
                    case COMPLETED,
                         CANCELLED,
                         NO_SHOW,
                         RESCHEDULED -> false;
                };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid appointment status transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }

    @Transactional(readOnly = true)
    public List<com.dentalclinic.appointment.dto.BookingDoctorResponse> getBookingDoctors(UUID requestedClinicId) {
        Clinic clinic = resolveClinic(SecurityUtils.getCurrentUser(), requestedClinicId);
        return doctorProfileRepository.findBookingDoctorsByClinicId(clinic.getId());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(LocalDate date) {
        AppUser currentUser = SecurityUtils.getCurrentUser();
        if (date == null) throw new IllegalArgumentException("Appointment date is required");
        if (currentUser.getClinic() == null) {
            throw new IllegalArgumentException("A clinic-linked doctor account is required");
        }
        UUID clinicId = currentUser.getClinic().getId();
        doctorProfileRepository.findByUserIdAndClinicId(currentUser.getId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found for this account and clinic"));
        return appointmentRepository.findOwnDailyAppointmentsWithDetails(clinicId, currentUser.getId(), date)
                .stream().map(this::mapToResponse).toList();
    }

}