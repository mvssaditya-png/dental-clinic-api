package com.dentalclinic.security;

import com.dentalclinic.security.authorization.*;
import com.dentalclinic.appointment.dto.UpdateAppointmentStatusRequest;
import com.dentalclinic.appointment.entity.AppointmentStatus;
import com.dentalclinic.treatment.dto.UpdateTreatmentPlanStatusRequest;
import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class AuthorizationPolicyTest {
    private Authentication auth(String permission) {
        return new UsernamePasswordAuthenticationToken("user", null, List.of(new SimpleGrantedAuthority(permission)));
    }
    @TestFactory Stream<DynamicTest> appointmentActionsRequireTheirOwnPermission() {
        var policy = new AppointmentAuthorization();
        return Arrays.stream(AppointmentStatus.values()).flatMap(status -> Stream.of(
                "APPOINTMENT_EDIT", "APPOINTMENT_CHECK_IN", "APPOINTMENT_CANCEL", "CONSULTATION_MANAGE", "APPOINTMENT_VIEW", "ROLE_SUPER_ADMIN")
                .map(permission -> DynamicTest.dynamicTest(status + " / " + permission, () -> {
                    var request = new UpdateAppointmentStatusRequest(); request.setStatus(status);
                    boolean allowed = switch (status) {
                        case CHECKED_IN -> permission.equals("APPOINTMENT_CHECK_IN");
                        case CANCELLED -> permission.equals("APPOINTMENT_CANCEL");
                        case IN_CONSULTATION, COMPLETED -> Set.of("APPOINTMENT_EDIT", "CONSULTATION_MANAGE").contains(permission);
                        default -> permission.equals("APPOINTMENT_EDIT");
                    };
                    assertEquals(allowed, policy.canUpdateStatus(auth(permission), request));
                })));
    }
    @TestFactory Stream<DynamicTest> treatmentApprovalCannotBeBypassedWithEdit() {
        var policy = new TreatmentPlanAuthorization();
        return Arrays.stream(TreatmentPlanStatus.values()).flatMap(status -> Stream.of(
                "TREATMENT_PLAN_EDIT", "TREATMENT_PLAN_APPROVE", "TREATMENT_PLAN_VIEW", "APPOINTMENT_EDIT")
                .map(permission -> DynamicTest.dynamicTest(status + " / " + permission, () -> {
                    var request = new UpdateTreatmentPlanStatusRequest(); request.setStatus(status);
                    assertEquals(permission.equals(status == TreatmentPlanStatus.APPROVED ? "TREATMENT_PLAN_APPROVE" : "TREATMENT_PLAN_EDIT"),
                            policy.canUpdateStatus(auth(permission), request));
                })));
    }
    @Test void missingAuthenticationAndInvalidRequestsFailClosed() {
        var appointment = new AppointmentAuthorization();
        var treatment = new TreatmentPlanAuthorization();
        assertFalse(appointment.canUpdateStatus(null, new UpdateAppointmentStatusRequest()));
        assertFalse(appointment.canUpdateStatus(auth("APPOINTMENT_EDIT"), null));
        assertFalse(appointment.canUpdateStatus(auth("APPOINTMENT_EDIT"), new UpdateAppointmentStatusRequest()));
        assertFalse(treatment.canUpdateStatus(null, new UpdateTreatmentPlanStatusRequest()));
        assertFalse(treatment.canUpdateStatus(auth("TREATMENT_PLAN_EDIT"), null));
        assertFalse(treatment.canUpdateStatus(auth("TREATMENT_PLAN_EDIT"), new UpdateTreatmentPlanStatusRequest()));
    }
}
