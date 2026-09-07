package com.dentalclinic.doctor.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.doctor.dto.CreateDoctorRequest;
import com.dentalclinic.doctor.dto.DoctorResponse;
import com.dentalclinic.doctor.entity.DoctorProfile;
import com.dentalclinic.doctor.repository.DoctorProfileRepository;
import com.dentalclinic.security.entity.Role;
import com.dentalclinic.security.entity.UserRole;
import com.dentalclinic.security.repository.RoleRepository;
import com.dentalclinic.security.repository.UserRoleRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;
import com.dentalclinic.user.entity.UserStatus;
import com.dentalclinic.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    public DoctorResponse createDoctor(
            CreateDoctorRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                request.getClinicId()
        );

        String phone = request.getPhone().trim();

        if (appUserRepository.existsByClinicIdAndPhone(
                clinic.getId(),
                phone
        )) {
            throw new IllegalArgumentException(
                    "A user with this phone number already exists in this clinic"
            );
        }

        String registrationNumber =
                normalizeNullable(
                        request.getRegistrationNumber()
                );

        if (registrationNumber != null &&
                doctorProfileRepository
                        .existsByClinicIdAndRegistrationNumber(
                                clinic.getId(),
                                registrationNumber
                        )) {

            throw new IllegalArgumentException(
                    "A doctor with this registration number already exists"
            );
        }

        /*
         * 1. Create login/account
         */
        AppUser user = AppUser.builder()
                .clinic(clinic)
                .firstName(request.getFirstName().trim())
                .lastName(
                        normalizeNullable(
                                request.getLastName()
                        )
                )
                .phone(phone)
                .email(
                        normalizeNullable(
                                request.getEmail()
                        )
                )
                .status(UserStatus.ACTIVE)
                .phoneVerified(false)
                .emailVerified(false)
                .build();

        user = appUserRepository.save(user);

        /*
         * 2. Assign DOCTOR role
         */
        Role doctorRole =
                roleRepository
                        .findByRoleCodeAndClinicIdIsNull(
                                "DOCTOR"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "DOCTOR role not configured"
                                )
                        );

        UserRole userRole =
                UserRole.builder()
                        .user(user)
                        .role(doctorRole)
                        .assignedBy(currentUser)
                        .build();

        userRoleRepository.save(userRole);

        /*
         * 3. Create professional doctor profile
         */
        DoctorProfile doctor =
                DoctorProfile.builder()
                        .clinic(clinic)
                        .user(user)
                        .registrationNumber(
                                registrationNumber
                        )
                        .qualification(
                                normalizeNullable(
                                        request.getQualification()
                                )
                        )
                        .specialization(
                                normalizeNullable(
                                        request.getSpecialization()
                                )
                        )
                        .designation(
                                normalizeNullable(
                                        request.getDesignation()
                                )
                        )
                        .experienceYears(
                                request.getExperienceYears()
                        )
                        .consultationFee(
                                request.getConsultationFee()
                        )
                        .bio(
                                normalizeNullable(
                                        request.getBio()
                                )
                        )
                        .active(true)
                        .createdBy(currentUser)
                        .updatedBy(currentUser)
                        .build();

        doctor = doctorProfileRepository.save(doctor);

        return mapToResponse(doctor);
    }

    private Clinic resolveClinic(
            AppUser currentUser,
            java.util.UUID requestedClinicId
    ) {

        /*
         * Normal clinic user:
         * Always force their own clinic.
         */
        if (currentUser.getClinic() != null) {
            return currentUser.getClinic();
        }

        /*
         * Platform SUPER_ADMIN:
         * clinic must be supplied.
         */
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

    private DoctorResponse mapToResponse(
            DoctorProfile doctor
    ) {

        AppUser user = doctor.getUser();

        return DoctorResponse.builder()
                .id(doctor.getId())
                .userId(user.getId())
                .clinicId(
                        doctor.getClinic().getId()
                )
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .registrationNumber(
                        doctor.getRegistrationNumber()
                )
                .qualification(
                        doctor.getQualification()
                )
                .specialization(
                        doctor.getSpecialization()
                )
                .designation(
                        doctor.getDesignation()
                )
                .experienceYears(
                        doctor.getExperienceYears()
                )
                .consultationFee(
                        doctor.getConsultationFee()
                )
                .bio(doctor.getBio())
                .active(doctor.getActive())
                .createdAt(doctor.getCreatedAt())
                .build();
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

    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctors(
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        return doctorProfileRepository
                .findActiveDoctorsByClinicId(
                        clinic.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(
            UUID doctorId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        DoctorProfile doctor =
                doctorProfileRepository
                        .findByIdAndClinicIdWithDetails(
                                doctorId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Doctor not found"
                                )
                        );

        return mapToResponse(doctor);
    }
}