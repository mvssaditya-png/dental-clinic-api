package com.dentalclinic.security.repository;

import com.dentalclinic.security.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Query("""
        SELECT ur
        FROM UserRole ur
        JOIN FETCH ur.role r
        WHERE ur.user.id = :userId
    """)
    List<UserRole> findAllByUserId(
            @Param("userId") UUID userId
    );

    Optional<UserRole> findByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );

    boolean existsByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );

    void deleteByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );
}