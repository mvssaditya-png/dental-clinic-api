package com.dentalclinic.treatment.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.odontogram.entity.OdontogramCondition;
import com.dentalclinic.odontogram.repository.OdontogramConditionRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.dto.ConditionProcedureMappingResponse;
import com.dentalclinic.treatment.dto.CreateConditionProcedureMappingRequest;
import com.dentalclinic.treatment.entity.ConditionProcedureMapping;
import com.dentalclinic.treatment.entity.ProcedureMaster;
import com.dentalclinic.treatment.entity.ProcedurePrice;
import com.dentalclinic.treatment.repository.ConditionProcedureMappingRepository;
import com.dentalclinic.treatment.repository.ProcedureMasterRepository;
import com.dentalclinic.treatment.repository.ProcedurePriceRepository;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConditionProcedureMappingService {

    private final ConditionProcedureMappingRepository
            conditionProcedureMappingRepository;

    private final OdontogramConditionRepository
            odontogramConditionRepository;

    private final ProcedureMasterRepository
            procedureMasterRepository;

    private final ProcedurePriceRepository
            procedurePriceRepository;

    private final ClinicRepository
            clinicRepository;

    @Transactional
    public ConditionProcedureMappingResponse createMapping(
            UUID conditionId,
            CreateConditionProcedureMappingRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        OdontogramCondition condition =
                odontogramConditionRepository
                        .findByIdAndClinicId(
                                conditionId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Odontogram condition not found"
                                )
                        );

        if (!Boolean.TRUE.equals(
                condition.getActive()
        )) {
            throw new IllegalArgumentException(
                    "Odontogram condition is inactive"
            );
        }

        ProcedureMaster procedure =
                procedureMasterRepository
                        .findByIdAndClinicId(
                                request.getProcedureId(),
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Procedure not found"
                                )
                        );

        if (!Boolean.TRUE.equals(
                procedure.getActive()
        )) {
            throw new IllegalArgumentException(
                    "Procedure is inactive"
            );
        }

        if (conditionProcedureMappingRepository
                .existsByConditionIdAndProcedureId(
                        condition.getId(),
                        procedure.getId()
                )) {

            throw new IllegalArgumentException(
                    "This procedure is already mapped to the condition"
            );
        }

        ConditionProcedureMapping mapping =
                ConditionProcedureMapping.builder()
                        .clinic(clinic)
                        .condition(condition)
                        .procedure(procedure)
                        .primary(
                                request.getPrimary() != null
                                        ? request.getPrimary()
                                        : false
                        )
                        .displayOrder(
                                request.getDisplayOrder() != null
                                        ? request.getDisplayOrder()
                                        : 0
                        )
                        .defaultNotes(
                                normalize(
                                        request.getDefaultNotes()
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

        mapping =
                conditionProcedureMappingRepository
                        .save(mapping);

        ProcedurePrice currentPrice =
                procedurePriceRepository
                        .findCurrentPrice(
                                clinic.getId(),
                                procedure.getId(),
                                LocalDate.now()
                        )
                        .orElse(null);

        return mapResponse(
                mapping,
                currentPrice
        );
    }

    @Transactional(readOnly = true)
    public List<ConditionProcedureMappingResponse>
    getSuggestedProcedures(
            UUID conditionId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        OdontogramCondition condition =
                odontogramConditionRepository
                        .findByIdAndClinicId(
                                conditionId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Odontogram condition not found"
                                )
                        );

        return conditionProcedureMappingRepository
                .findActiveSuggestions(
                        clinic.getId(),
                        condition.getId()
                )
                .stream()
                .map(mapping -> {

                    ProcedurePrice currentPrice =
                            procedurePriceRepository
                                    .findCurrentPrice(
                                            clinic.getId(),
                                            mapping
                                                    .getProcedure()
                                                    .getId(),
                                            LocalDate.now()
                                    )
                                    .orElse(null);

                    return mapResponse(
                            mapping,
                            currentPrice
                    );
                })
                .toList();
    }

    private ConditionProcedureMappingResponse mapResponse(
            ConditionProcedureMapping mapping,
            ProcedurePrice currentPrice
    ) {

        return ConditionProcedureMappingResponse.builder()

                .id(
                        mapping.getId()
                )

                .conditionId(
                        mapping.getCondition()
                                .getId()
                )

                .conditionCode(
                        mapping.getCondition()
                                .getConditionCode()
                )

                .conditionName(
                        mapping.getCondition()
                                .getConditionName()
                )

                .procedureId(
                        mapping.getProcedure()
                                .getId()
                )

                .procedureCode(
                        mapping.getProcedure()
                                .getProcedureCode()
                )

                .procedureName(
                        mapping.getProcedure()
                                .getProcedureName()
                )

                .primary(
                        mapping.getPrimary()
                )

                .displayOrder(
                        mapping.getDisplayOrder()
                )

                .defaultNotes(
                        mapping.getDefaultNotes()
                )

                .patientExplanation(
                        mapping.getPatientExplanation()
                )

                .currentPrice(
                        currentPrice != null
                                ? currentPrice.getPrice()
                                : null
                )

                .active(
                        mapping.getActive()
                )

                .createdAt(
                        mapping.getCreatedAt()
                )

                .updatedAt(
                        mapping.getUpdatedAt()
                )

                .build();
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
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
                    "clinicId is required"
            );
        }

        return clinicRepository
                .findById(
                        requestedClinicId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Clinic not found"
                        )
                );
    }
}