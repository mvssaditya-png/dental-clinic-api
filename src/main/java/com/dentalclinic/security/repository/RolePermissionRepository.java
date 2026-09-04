package com.dentalclinic.security.repository;

import com.dentalclinic.security.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, UUID> {

    @Query("""
        SELECT rp
        FROM RolePermission rp
        JOIN FETCH rp.permission p
        WHERE rp.role.id = :roleId
    """)
    List<RolePermission> findAllByRoleId(
            @Param("roleId") UUID roleId
    );

    Optional<RolePermission> findByRoleIdAndPermissionId(
            UUID roleId,
            UUID permissionId
    );

    boolean existsByRoleIdAndPermissionId(
            UUID roleId,
            UUID permissionId
    );

    void deleteByRoleIdAndPermissionId(
            UUID roleId,
            UUID permissionId
    );
}