package com.dentalclinic.clinic;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.clinic.service.*;
import com.dentalclinic.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ClinicAccessPolicyTest {
    ClinicAccessPolicy policy = new ClinicAccessPolicy();
    @Test void eligibilityFailsClosedButAllowsPlatformIdentity() {
        assertTrue(policy.isLoginEligible(AppUser.builder().build()));
        for (Boolean active : Arrays.asList(true, false, null)) {
            Clinic c = Clinic.builder().active(active).build();
            assertEquals(Boolean.TRUE.equals(active), policy.isLoginEligible(AppUser.builder().clinic(c).build()));
            if (!Boolean.TRUE.equals(active)) assertThrows(AccessDeniedException.class, () -> policy.requireActive(c));
        }
    }
    @Test void resolverPreservesOwnClinicAndRequiresExplicitActivePlatformTarget() {
        var repo = mock(ClinicRepository.class); var resolver = new TenantClinicResolver(repo, policy);
        var own = Clinic.builder().id(UUID.randomUUID()).active(true).build();
        assertSame(own, resolver.resolve(AppUser.builder().clinic(own).build(), UUID.randomUUID(), "required"));
        verifyNoInteractions(repo);
        var platform = AppUser.builder().build();
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(platform, null, "required"));
        when(repo.findById(own.getId())).thenReturn(Optional.of(own));
        assertSame(own, resolver.resolve(platform, own.getId(), "required"));
        own.setActive(false);
        assertThrows(AccessDeniedException.class, () -> resolver.resolve(platform, own.getId(), "required"));
        assertThrows(AccessDeniedException.class, () -> resolver.resolve(AppUser.builder().clinic(own).build(), UUID.randomUUID(), "required"));
    }
}

