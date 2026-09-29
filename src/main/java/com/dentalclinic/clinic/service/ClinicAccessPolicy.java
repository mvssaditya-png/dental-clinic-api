package com.dentalclinic.clinic.service;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class ClinicAccessPolicy {
    public boolean isLoginEligible(AppUser user) {
        return user.getClinic() == null || Boolean.TRUE.equals(user.getClinic().getActive());
    }
    public void requireActive(Clinic clinic) {
        if (clinic == null || !Boolean.TRUE.equals(clinic.getActive()))
            throw new AccessDeniedException("Clinic is inactive");
    }
    public void requireUserClinicActive(AppUser user) {
        if (user.getClinic() != null) requireActive(user.getClinic());
    }
}

