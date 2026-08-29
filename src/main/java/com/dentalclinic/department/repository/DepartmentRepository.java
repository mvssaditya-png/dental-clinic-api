package com.dentalclinic.department.repository;

import com.dentalclinic.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByClinicIdAndDepartmentCode(
            UUID clinicId,
            String departmentCode
    );

    List<Department> findAllByClinicIdAndActiveTrueOrderByDepartmentNameAsc(
            UUID clinicId
    );

    boolean existsByClinicIdAndDepartmentCode(
            UUID clinicId,
            String departmentCode
    );

    boolean existsByClinicIdAndDepartmentNameIgnoreCase(
            UUID clinicId,
            String departmentName
    );
}