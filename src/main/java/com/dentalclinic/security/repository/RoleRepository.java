package com.dentalclinic.security.repository;

import com.dentalclinic.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByClinicIsNullAndRoleCode(String roleCode);

    Optional<Role> findByClinicIdAndRoleCode(UUID clinicId, String roleCode);

    List<Role> findAllByClinicIdAndActiveTrue(UUID clinicId);

    List<Role> findAllByClinicIsNullAndActiveTrue();

    boolean existsByClinicIdAndRoleCode(UUID clinicId, String roleCode);

    Optional<Role> findByRoleCodeAndClinicIdIsNull(
            String roleCode
    );
}