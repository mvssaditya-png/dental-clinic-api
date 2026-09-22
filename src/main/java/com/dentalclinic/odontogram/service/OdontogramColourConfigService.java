package com.dentalclinic.odontogram.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;

import com.dentalclinic.odontogram.dto.OdontogramColourConfigResponse;
import com.dentalclinic.odontogram.dto.SaveOdontogramColourConfigRequest;

import com.dentalclinic.odontogram.entity.OdontogramColourConfig;
import com.dentalclinic.odontogram.entity.OdontogramCondition;

import com.dentalclinic.odontogram.repository.OdontogramColourConfigRepository;
import com.dentalclinic.odontogram.repository.OdontogramConditionRepository;

import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OdontogramColourConfigService {

    private final OdontogramColourConfigRepository colourConfigRepository;
    private final OdontogramConditionRepository conditionRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    public OdontogramColourConfigResponse saveColourConfig(
            SaveOdontogramColourConfigRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        /*
         * Condition must belong to the same clinic.
         */
        OdontogramCondition condition =
                conditionRepository
                        .findByIdAndClinicId(
                                request.getConditionId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Odontogram condition not found"
                                )
                        );

        if (!Boolean.TRUE.equals(condition.getActive())) {
            throw new IllegalArgumentException(
                    "Odontogram condition is inactive"
            );
        }

        /*
         * One colour config per condition.
         *
         * If it already exists, update it.
         * Otherwise create it.
         */
        OdontogramColourConfig config =
                colourConfigRepository
                        .findByConditionIdAndClinicId(
                                condition.getId(),
                                clinic.getId()
                        )
                        .orElseGet(() ->
                                OdontogramColourConfig
                                        .builder()
                                        .clinic(clinic)
                                        .condition(condition)
                                        .createdBy(currentUser)
                                        .build()
                        );

        config.setColourHex(
                request.getColourHex().toUpperCase()
        );

        config.setDisplayOrder(
                request.getDisplayOrder()
        );

        config.setActive(
                request.getActive() != null
                        ? request.getActive()
                        : true
        );

        config.setUpdatedBy(currentUser);

        config =
                colourConfigRepository.save(config);

        return mapToResponse(config);
    }

    @Transactional(readOnly = true)
    public List<OdontogramColourConfigResponse> getColourConfigs(
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        return colourConfigRepository
                .findAllActiveWithCondition(
                        clinic.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OdontogramColourConfigResponse mapToResponse(
            OdontogramColourConfig config
    ) {

        OdontogramCondition condition =
                config.getCondition();

        return OdontogramColourConfigResponse
                .builder()
                .id(config.getId())
                .clinicId(
                        config.getClinic().getId()
                )
                .conditionId(
                        condition.getId()
                )
                .conditionCode(
                        condition.getConditionCode()
                )
                .conditionName(
                        condition.getConditionName()
                )
                .colourHex(
                        config.getColourHex()
                )
                .displayOrder(
                        config.getDisplayOrder()
                )
                .active(
                        config.getActive()
                )
                .build();
    }

    private Clinic resolveClinic(
            AppUser currentUser,
            UUID requestedClinicId
    ) {

        /*
         * Normal clinic user:
         * always use their own clinic.
         */
        if (currentUser.getClinic() != null) {
            return currentUser.getClinic();
        }

        /*
         * Platform user:
         * clinic context must be explicitly supplied.
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
}