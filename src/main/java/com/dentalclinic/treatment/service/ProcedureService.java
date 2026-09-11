package com.dentalclinic.treatment.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.dto.CreateProcedurePriceRequest;
import com.dentalclinic.treatment.dto.CreateProcedureRequest;
import com.dentalclinic.treatment.dto.ProcedurePriceResponse;
import com.dentalclinic.treatment.dto.ProcedureResponse;
import com.dentalclinic.treatment.entity.ProcedureMaster;
import com.dentalclinic.treatment.entity.ProcedurePrice;
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
public class ProcedureService {

    private final ProcedureMasterRepository procedureMasterRepository;

    private final ProcedurePriceRepository procedurePriceRepository;

    private final ClinicRepository clinicRepository;

    /*
     * Add your EXISTING DepartmentRepository here.
     *
     * private final DepartmentRepository departmentRepository;
     *
     * Do not create another Department entity.
     */

    @Transactional
    public ProcedureResponse createProcedure(
            CreateProcedureRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        String code =
                request.getProcedureCode()
                        .trim()
                        .toUpperCase();

        String name =
                request.getProcedureName()
                        .trim();

        if (procedureMasterRepository
                .existsByClinicIdAndProcedureCode(
                        clinic.getId(),
                        code
                )) {

            throw new IllegalArgumentException(
                    "Procedure code already exists"
            );
        }

        ProcedureMaster procedure =
                ProcedureMaster.builder()
                        .clinic(clinic)
                        .procedureCode(code)
                        .procedureName(name)
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

                        /*
                         * Keep department null in this first API pass.
                         *
                         * When we complete Department APIs,
                         * we'll validate departmentId belongs
                         * to this clinic and set it here.
                         */
                        .department(null)

                        .defaultDurationMinutes(
                                request.getDefaultDurationMinutes()
                        )
                        .defaultEstimatedVisits(
                                request.getDefaultEstimatedVisits()
                        )
                        .defaultNotes(
                                normalize(
                                        request.getDefaultNotes()
                                )
                        )
                        .followUpInstructions(
                                normalize(
                                        request.getFollowUpInstructions()
                                )
                        )
                        .active(true)
                        .createdBy(currentUser)
                        .updatedBy(currentUser)
                        .build();

        procedure =
                procedureMasterRepository.save(
                        procedure
                );

        return mapProcedureResponse(
                procedure,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<ProcedureResponse> getProcedures(
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        LocalDate today =
                LocalDate.now();

        return procedureMasterRepository
                .findActiveByClinicId(
                        clinic.getId()
                )
                .stream()
                .map(procedure -> {

                    ProcedurePrice price =
                            procedurePriceRepository
                                    .findCurrentPrice(
                                            clinic.getId(),
                                            procedure.getId(),
                                            today
                                    )
                                    .orElse(null);

                    return mapProcedureResponse(
                            procedure,
                            price
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProcedureResponse getProcedure(
            UUID procedureId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        ProcedureMaster procedure =
                procedureMasterRepository
                        .findByIdAndClinicIdWithDetails(
                                procedureId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Procedure not found"
                                )
                        );

        ProcedurePrice price =
                procedurePriceRepository
                        .findCurrentPrice(
                                clinic.getId(),
                                procedure.getId(),
                                LocalDate.now()
                        )
                        .orElse(null);

        return mapProcedureResponse(
                procedure,
                price
        );
    }

    @Transactional
    public ProcedurePriceResponse addPrice(
            UUID procedureId,
            CreateProcedurePriceRequest request
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        request.getClinicId()
                );

        ProcedureMaster procedure =
                procedureMasterRepository
                        .findByIdAndClinicId(
                                procedureId,
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

        LocalDate effectiveFrom =
                request.getEffectiveFrom() != null
                        ? request.getEffectiveFrom()
                        : LocalDate.now();

        LocalDate effectiveTo =
                request.getEffectiveTo();

        if (effectiveTo != null &&
                effectiveTo.isBefore(
                        effectiveFrom
                )) {

            throw new IllegalArgumentException(
                    "effectiveTo cannot be before effectiveFrom"
            );
        }

        LocalDate overlapEnd =
                effectiveTo != null
                        ? effectiveTo
                        : LocalDate.of(
                        9999,
                        12,
                        31
                );

        boolean overlap =
                procedurePriceRepository
                        .existsOverlappingPrice(
                                clinic.getId(),
                                procedure.getId(),
                                effectiveFrom,
                                overlapEnd
                        );

        if (overlap) {
            throw new IllegalArgumentException(
                    "Procedure price overlaps with an existing active price period"
            );
        }

        ProcedurePrice price =
                ProcedurePrice.builder()
                        .clinic(clinic)
                        .procedure(procedure)
                        .price(request.getPrice())
                        .effectiveFrom(
                                effectiveFrom
                        )
                        .effectiveTo(
                                effectiveTo
                        )
                        .active(true)
                        .createdBy(currentUser)
                        .build();

        price =
                procedurePriceRepository.save(
                        price
                );

        return mapPriceResponse(
                price
        );
    }

    @Transactional(readOnly = true)
    public List<ProcedurePriceResponse>
    getPriceHistory(
            UUID procedureId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        procedureMasterRepository
                .findByIdAndClinicId(
                        procedureId,
                        clinic.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Procedure not found"
                        )
                );

        return procedurePriceRepository
                .findPriceHistory(
                        clinic.getId(),
                        procedureId
                )
                .stream()
                .map(this::mapPriceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProcedurePriceResponse getCurrentPrice(
            UUID procedureId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic =
                resolveClinic(
                        currentUser,
                        requestedClinicId
                );

        procedureMasterRepository
                .findByIdAndClinicId(
                        procedureId,
                        clinic.getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Procedure not found"
                        )
                );

        ProcedurePrice price =
                procedurePriceRepository
                        .findCurrentPrice(
                                clinic.getId(),
                                procedureId,
                                LocalDate.now()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No current price configured for this procedure"
                                )
                        );

        return mapPriceResponse(
                price
        );
    }

    private ProcedureResponse mapProcedureResponse(
            ProcedureMaster procedure,
            ProcedurePrice price
    ) {

        return ProcedureResponse.builder()

                .id(
                        procedure.getId()
                )

                .clinicId(
                        procedure.getClinic()
                                .getId()
                )

                .procedureCode(
                        procedure.getProcedureCode()
                )

                .procedureName(
                        procedure.getProcedureName()
                )

                .description(
                        procedure.getDescription()
                )

                .patientExplanation(
                        procedure.getPatientExplanation()
                )

                .departmentId(
                        procedure.getDepartment() != null
                                ? procedure
                                  .getDepartment()
                                  .getId()
                                : null
                )

                .defaultDurationMinutes(
                        procedure.getDefaultDurationMinutes()
                )

                .defaultEstimatedVisits(
                        procedure.getDefaultEstimatedVisits()
                )

                .defaultNotes(
                        procedure.getDefaultNotes()
                )

                .followUpInstructions(
                        procedure.getFollowUpInstructions()
                )

                .active(
                        procedure.getActive()
                )

                .currentPrice(
                        price != null
                                ? price.getPrice()
                                : null
                )

                .priceEffectiveFrom(
                        price != null
                                ? price.getEffectiveFrom()
                                : null
                )

                .createdAt(
                        procedure.getCreatedAt()
                )

                .updatedAt(
                        procedure.getUpdatedAt()
                )

                .build();
    }

    private ProcedurePriceResponse mapPriceResponse(
            ProcedurePrice price
    ) {

        return ProcedurePriceResponse.builder()

                .id(
                        price.getId()
                )

                .procedureId(
                        price.getProcedure()
                                .getId()
                )

                .price(
                        price.getPrice()
                )

                .effectiveFrom(
                        price.getEffectiveFrom()
                )

                .effectiveTo(
                        price.getEffectiveTo()
                )

                .active(
                        price.getActive()
                )

                .createdAt(
                        price.getCreatedAt()
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