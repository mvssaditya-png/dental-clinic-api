package com.dentalclinic.department.service;

import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.department.dto.DepartmentResponse;
import com.dentalclinic.department.entity.Department;
import com.dentalclinic.department.repository.DepartmentRepository;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments(
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        return departmentRepository
                .findAllByClinicIdAndActiveTrueOrderByDepartmentNameAsc(
                        clinic.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(
            UUID departmentId,
            UUID requestedClinicId
    ) {

        AppUser currentUser =
                SecurityUtils.getCurrentUser();

        Clinic clinic = resolveClinic(
                currentUser,
                requestedClinicId
        );

        Department department =
                departmentRepository
                        .findByIdAndClinicId(
                                departmentId,
                                clinic.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Department not found"
                                )
                        );

        return mapToResponse(department);
    }

    private DepartmentResponse mapToResponse(
            Department department
    ) {

        return DepartmentResponse.builder()
                .id(department.getId())
                .clinicId(department.getClinic().getId())
                .departmentCode(
                        department.getDepartmentCode()
                )
                .departmentName(
                        department.getDepartmentName()
                )
                .description(
                        department.getDescription()
                )
                .active(
                        department.getActive()
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
                    "clinicId is required"
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