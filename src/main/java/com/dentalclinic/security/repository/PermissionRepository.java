package com.dentalclinic.security.repository;

import com.dentalclinic.security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByPermissionCode(String permissionCode);

    List<Permission> findAllByModuleAndActiveTrue(String module);

    List<Permission> findAllByActiveTrueOrderByModuleAscPermissionCodeAsc();

    boolean existsByPermissionCode(String permissionCode);
}