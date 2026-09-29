package com.dentalclinic.appointment.service;

import com.dentalclinic.appointment.dto.DentalChairResponse;
import com.dentalclinic.appointment.entity.DentalChair;
import com.dentalclinic.appointment.repository.DentalChairRepository;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DentalChairService {

    private final com.dentalclinic.clinic.service.TenantClinicResolver tenantClinicResolver;

    private final DentalChairRepository dentalChairRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public List<DentalChairResponse> getDentalChairs(
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        return dentalChairRepository
                .findAllByClinicIdAndActiveTrueOrderByChairNameAsc(
                        clinic.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DentalChairResponse getDentalChair(
            UUID chairId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        DentalChair chair =
                dentalChairRepository
                        .findByIdAndClinicId(
                                chairId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Dental chair not found"
                                )
                        );

        return mapToResponse(chair);
    }

    private DentalChairResponse mapToResponse(
            DentalChair chair
    ) {

        return DentalChairResponse.builder()
                .id(chair.getId())
                .clinicId(chair.getClinic().getId())
                .chairCode(
                        chair.getChairCode()
                )
                .chairName(
                        chair.getChairName()
                )
                .location(
                        chair.getLocation()
                )
                .description(
                        chair.getDescription()
                )
                .active(
                        chair.getActive()
                )
                .build();
    }

    private Clinic resolveClinic(AppUser currentUser, java.util.UUID requestedClinicId) {
        return tenantClinicResolver.resolve(currentUser, requestedClinicId, "clinicId is required");
    }
}