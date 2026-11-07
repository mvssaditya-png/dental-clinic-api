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

    private final com.dentalclinic.clinic.service.TenantClinicResolver tenantClinicResolver;

    private final OdontogramColourConfigRepository colourConfigRepository;
    private final OdontogramConditionRepository conditionRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly=true)
    public OdontogramColourConfigResponse getColourConfig(UUID id,UUID requestedClinicId){
        Clinic clinic=resolveClinic(SecurityUtils.getCurrentUser(),requestedClinicId);
        return mapToResponse(colourConfigRepository.findByConditionIdAndClinicId(id,clinic.getId()).orElseThrow());
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

    private Clinic resolveClinic(AppUser currentUser, java.util.UUID requestedClinicId) {
        return tenantClinicResolver.resolve(currentUser, requestedClinicId, "clinicId is required for platform users");
    }
}