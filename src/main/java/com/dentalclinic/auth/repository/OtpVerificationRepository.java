package com.dentalclinic.auth.repository;

import com.dentalclinic.auth.entity.OtpPurpose;
import com.dentalclinic.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification>
    findFirstByPhoneAndPurposeOrderByCreatedAtDesc(
            String phone,
            OtpPurpose purpose
    );

    long countByPhoneAndPurposeAndCreatedAtAfter(
            String phone,
            OtpPurpose purpose,
            LocalDateTime createdAt
    );
}