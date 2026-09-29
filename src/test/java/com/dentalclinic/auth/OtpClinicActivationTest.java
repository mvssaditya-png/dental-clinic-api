package com.dentalclinic.auth;
import com.dentalclinic.auth.service.*;
import com.dentalclinic.auth.dto.*;
import com.dentalclinic.auth.entity.*;
import com.dentalclinic.auth.repository.*;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.service.ClinicAccessPolicy;
import com.dentalclinic.user.entity.*;
import com.dentalclinic.user.repository.*;
import com.dentalclinic.security.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class OtpClinicActivationTest {
    OtpVerificationRepository otps = mock(OtpVerificationRepository.class);
    AppUserRepository users = mock(AppUserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    JwtService jwt = mock(JwtService.class);
    OtpService service = new OtpService(otps, users, encoder, mock(UserRoleRepository.class), mock(RolePermissionRepository.class), jwt, new ClinicAccessPolicy());
    Clinic clinic = Clinic.builder().active(false).build();
    AppUser user = AppUser.builder().clinic(clinic).status(UserStatus.ACTIVE).build();
    SendOtpRequest send() { var r = new SendOtpRequest(); r.setPhone("9876500001"); return r; }
    VerifyOtpRequest verifyRequest() { var r = new VerifyOtpRequest(); r.setPhone("9876500001"); r.setOtp("123456"); return r; }
    OtpVerification validOtp() {
        var otp = OtpVerification.builder().expiresAt(LocalDateTime.now().plusMinutes(5)).attemptCount(0).used(false).otpHash("hash").build();
        when(otps.findFirstByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(anyString(), eq(OtpPurpose.LOGIN))).thenReturn(Optional.of(otp));
        when(encoder.matches("123456", "hash")).thenReturn(true); return otp;
    }
    @BeforeEach void setup() { when(users.findAllByPhoneAndStatus(anyString(), eq(UserStatus.ACTIVE))).thenReturn(List.of(user)); }
    @Test void inactiveClinicCannotSendOtp() {
        assertThrows(IllegalArgumentException.class, () -> service.sendLoginOtp(send())); verifyNoInteractions(otps, encoder, jwt);
    }
    @Test void inactiveClinicCannotConsumeOtpOrUpdateLogin() {
        var otp = validOtp(); assertThrows(IllegalArgumentException.class, () -> service.verifyLoginOtp(verifyRequest()));
        assertFalse(otp.getUsed()); assertNull(user.getLastLoginAt()); verify(users, never()).save(any()); verify(otps, never()).save(any()); verifyNoInteractions(jwt);
    }
    @Test void deactivationBetweenSendAndVerifyIsRechecked() {
        clinic.setActive(true); when(encoder.encode(anyString())).thenReturn("hash");
        service.sendLoginOtp(send()); verify(otps).save(any()); clearInvocations(otps);
        clinic.setActive(false); validOtp(); assertThrows(IllegalArgumentException.class, () -> service.verifyLoginOtp(verifyRequest()));
        verify(otps, never()).save(any()); verifyNoInteractions(jwt);
    }
    @Test void platformCanRequestOtp() { user.setClinic(null); when(encoder.encode(anyString())).thenReturn("hash"); assertTrue(service.sendLoginOtp(send()).isSuccess()); }
    @Test void duplicateActiveAccountsAreNotResolvedByFilteringInactiveClinics() {
        validOtp(); when(users.findAllByPhoneAndStatus(anyString(), eq(UserStatus.ACTIVE))).thenReturn(List.of(user, AppUser.builder().build()));
        assertThrows(IllegalStateException.class, () -> service.verifyLoginOtp(verifyRequest())); verifyNoInteractions(jwt);
    }
}

