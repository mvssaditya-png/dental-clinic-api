package com.dentalclinic.security.authorization;

import com.dentalclinic.appointment.dto.UpdateAppointmentStatusRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("appointmentAuthorization")
public class AppointmentAuthorization {
    public boolean canUpdateStatus(Authentication authentication, UpdateAppointmentStatusRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || request == null || request.getStatus() == null) return false;
        return switch (request.getStatus()) {
            case CHECKED_IN -> has(authentication, "APPOINTMENT_CHECK_IN");
            case CANCELLED -> has(authentication, "APPOINTMENT_CANCEL");
            case IN_CONSULTATION, COMPLETED -> has(authentication, "APPOINTMENT_EDIT") || has(authentication, "CONSULTATION_MANAGE");
            default -> has(authentication, "APPOINTMENT_EDIT");
        };
    }
    private boolean has(Authentication authentication, String permission) {
        return authentication.getAuthorities().stream().anyMatch(a -> permission.equals(a.getAuthority()));
    }
}
