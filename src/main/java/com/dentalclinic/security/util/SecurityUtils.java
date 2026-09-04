package com.dentalclinic.security.util;

import com.dentalclinic.user.entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUser getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUser user)) {

            throw new IllegalStateException(
                    "Authenticated user not found"
            );
        }

        return user;
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static UUID getCurrentClinicId() {

        AppUser user = getCurrentUser();

        if (user.getClinic() == null) {
            return null;
        }

        return user.getClinic().getId();
    }
}