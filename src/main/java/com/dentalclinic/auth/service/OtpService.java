package com.dentalclinic.auth.service;

import com.dentalclinic.auth.dto.SendOtpRequest;
import com.dentalclinic.auth.dto.SendOtpResponse;
import com.dentalclinic.auth.entity.OtpPurpose;
import com.dentalclinic.auth.entity.OtpVerification;
import com.dentalclinic.auth.repository.OtpVerificationRepository;
import com.dentalclinic.security.entity.Role;
import com.dentalclinic.security.entity.UserRole;
import com.dentalclinic.security.repository.RolePermissionRepository;
import com.dentalclinic.security.repository.UserRoleRepository;
import com.dentalclinic.user.entity.AppUser;
import com.dentalclinic.user.entity.UserStatus;
import com.dentalclinic.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import com.dentalclinic.auth.dto.VerifyOtpRequest;
import com.dentalclinic.auth.dto.VerifyOtpResponse;
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_REQUESTS = 5;
    private static final int RATE_LIMIT_MINUTES = 15;

    private final OtpVerificationRepository otpVerificationRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder otpPasswordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final JwtService jwtService;
    @Transactional
    public SendOtpResponse sendLoginOtp(SendOtpRequest request) {

        String phone = request.getPhone();

        // Only registered ACTIVE users can login.
        boolean activeUserExists =
                appUserRepository.existsByPhoneAndStatus(
                        phone,
                        UserStatus.ACTIVE
                );

        if (!activeUserExists) {
            throw new IllegalArgumentException(
                    "No active user found for this mobile number"
            );
        }

        LocalDateTime rateLimitStart =
                LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES);

        long recentRequests =
                otpVerificationRepository
                        .countByPhoneAndPurposeAndCreatedAtAfter(
                                phone,
                                OtpPurpose.LOGIN,
                                rateLimitStart
                        );

        if (recentRequests >= MAX_OTP_REQUESTS) {
            throw new IllegalStateException(
                    "Too many OTP requests. Please try again later."
            );
        }

        String otp = generateOtp();

        OtpVerification verification =
                OtpVerification.builder()
                        .phone(phone)
                        .otpHash(otpPasswordEncoder.encode(otp))
                        .purpose(OtpPurpose.LOGIN)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(OTP_EXPIRY_MINUTES)
                        )
                        .attemptCount(0)
                        .used(false)
                        .build();

        otpVerificationRepository.save(verification);

        /*
         * TEMPORARY FOR LOCAL DEVELOPMENT ONLY.
         *
         * Next we will replace this with the actual SMS provider.
         */
        System.out.println(
                "LOGIN OTP for " + phone + " = " + otp
        );

        return new SendOtpResponse(
                true,
                "OTP sent successfully",
                OTP_EXPIRY_MINUTES * 60
        );
    }

    private String generateOtp() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    @Transactional
    public VerifyOtpResponse verifyLoginOtp(
            VerifyOtpRequest request
    ) {

        OtpVerification verification =
                otpVerificationRepository
                        .findFirstByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                                request.getPhone(),
                                OtpPurpose.LOGIN
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "OTP not found. Please request a new OTP."
                                )
                        );

        if (verification.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "OTP has expired. Please request a new OTP."
            );
        }

        if (verification.getAttemptCount()
                >= MAX_VERIFY_ATTEMPTS) {

            throw new IllegalStateException(
                    "Maximum OTP verification attempts exceeded."
            );
        }

        if (!otpPasswordEncoder.matches(
                request.getOtp(),
                verification.getOtpHash()
        )) {

            verification.setAttemptCount(
                    verification.getAttemptCount() + 1
            );

            otpVerificationRepository.save(verification);

            throw new IllegalArgumentException(
                    "Invalid OTP"
            );
        }

        List<AppUser> users =
                appUserRepository.findAllByPhoneAndStatus(
                        request.getPhone(),
                        UserStatus.ACTIVE
                );

        if (users.isEmpty()) {
            throw new IllegalArgumentException(
                    "No active user found for this mobile number"
            );
        }

        if (users.size() > 1) {
            throw new IllegalStateException(
                    "This mobile number is associated with multiple accounts."
            );
        }

        AppUser user = users.get(0);

        verification.setUsed(true);
        verification.setVerifiedAt(LocalDateTime.now());

        user.setPhoneVerified(true);
        user.setLastLoginAt(LocalDateTime.now());

        otpVerificationRepository.save(verification);
        appUserRepository.save(user);

        List<UserRole> userRoles =
                userRoleRepository.findAllByUserId(user.getId());

        List<String> roles =
                userRoles.stream()
                        .map(UserRole::getRole)
                        .map(Role::getRoleCode)
                        .distinct()
                        .toList();

        List<String> permissions =
                userRoles.stream()
                        .flatMap(userRole ->
                                rolePermissionRepository
                                        .findAllByRoleId(
                                                userRole.getRole().getId()
                                        )
                                        .stream()
                        )
                        .map(rolePermission ->
                                rolePermission
                                        .getPermission()
                                        .getPermissionCode()
                        )
                        .distinct()
                        .sorted()
                        .toList();

        String token =
                jwtService.generateToken(
                        user,
                        roles,
                        permissions
                );

        return VerifyOtpResponse.builder()
                .success(true)
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInSeconds(
                        jwtService.getExpirationSeconds()
                )
                .userId(user.getId())

                .clinicId(
                        user.getClinic() != null
                                ? user.getClinic().getId()
                                : null
                )
                .clinicCode(
                        user.getClinic() != null
                                ? user.getClinic().getClinicCode()
                                : null
                )
                .clinicName(
                        user.getClinic() != null
                                ? user.getClinic().getClinicName()
                                : null
                )

                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .email(user.getEmail())

                .roles(roles)
                .permissions(permissions)

                .build();
    }
}