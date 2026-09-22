package com.dentalclinic.department.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DepartmentResponse {

    private UUID id;

    private UUID clinicId;

    private String departmentCode;

    private String departmentName;

    private String description;

    private Boolean active;
}