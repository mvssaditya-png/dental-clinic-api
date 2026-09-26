package com.dentalclinic.security.authorization;

import com.dentalclinic.treatment.dto.UpdateTreatmentPlanStatusRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("treatmentPlanAuthorization")
public class TreatmentPlanAuthorization {
    public boolean canUpdateStatus(Authentication authentication, UpdateTreatmentPlanStatusRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || request == null || request.getStatus() == null) return false;
        String permission = request.getStatus() == com.dentalclinic.treatment.entity.TreatmentPlanStatus.APPROVED
                ? "TREATMENT_PLAN_APPROVE" : "TREATMENT_PLAN_EDIT";
        return authentication.getAuthorities().stream().anyMatch(a -> permission.equals(a.getAuthority()));
    }
}
