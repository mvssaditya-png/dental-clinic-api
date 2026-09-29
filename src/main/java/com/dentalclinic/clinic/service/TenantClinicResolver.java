package com.dentalclinic.clinic.service;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.clinic.repository.ClinicRepository;
import com.dentalclinic.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component @RequiredArgsConstructor
public class TenantClinicResolver {
    private final ClinicRepository clinics;
    private final ClinicAccessPolicy access;
    public Clinic resolve(AppUser user, UUID requestedClinicId, String missingContextMessage) {
        Clinic clinic = user.getClinic();
        if (clinic == null) {
            if (requestedClinicId == null) throw new IllegalArgumentException(missingContextMessage);
            clinic = clinics.findById(requestedClinicId)
                    .orElseThrow(() -> new IllegalArgumentException("Clinic not found"));
        }
        access.requireActive(clinic);
        return clinic;
    }
}

