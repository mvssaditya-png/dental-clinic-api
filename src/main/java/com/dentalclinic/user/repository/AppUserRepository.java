package com.dentalclinic.user.repository;

import com.dentalclinic.user.entity.AppUser;
import com.dentalclinic.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByClinicIdAndEmailIgnoreCase(UUID clinicId, String email);

    Optional<AppUser> findByClinicIdAndPhone(UUID clinicId, String phone);

    List<AppUser> findAllByClinicIdAndStatus(UUID clinicId, UserStatus status);

    boolean existsByClinicIdAndEmailIgnoreCase(UUID clinicId, String email);

    boolean existsByClinicIdAndPhone(UUID clinicId, String phone);

    Optional<AppUser> findFirstByPhoneAndStatus(
            String phone,
            UserStatus status
    );
    boolean existsByPhoneAndStatus(
            String phone,
            UserStatus status
    );
    List<AppUser> findAllByPhoneAndStatus(
            String phone,
            UserStatus status
    );

    @Query("""
    SELECT u
    FROM AppUser u
    LEFT JOIN FETCH u.clinic
    WHERE u.id = :userId
""")
    Optional<AppUser> findByIdWithClinic(
            @Param("userId") UUID userId
    );
}