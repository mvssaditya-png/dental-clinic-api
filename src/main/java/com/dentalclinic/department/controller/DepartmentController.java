package com.dentalclinic.department.controller;

import com.dentalclinic.department.dto.DepartmentResponse;
import com.dentalclinic.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public List<DepartmentResponse> getDepartments(
            @RequestParam(required = false) UUID clinicId
    ) {

        return departmentService.getDepartments(
                clinicId
        );
    }

    @GetMapping("/{departmentId}")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public DepartmentResponse getDepartment(
            @PathVariable UUID departmentId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return departmentService.getDepartment(
                departmentId,
                clinicId
        );
    }
}