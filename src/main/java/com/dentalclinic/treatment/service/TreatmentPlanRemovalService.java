package com.dentalclinic.treatment.service;

import com.dentalclinic.casesheet.validation.RemovalReasonValidator;
import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.dto.RemoveTreatmentPlanRequest;
import com.dentalclinic.treatment.entity.*;
import com.dentalclinic.treatment.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TreatmentPlanRemovalService {
    private final TenantClinicResolver clinics;
    private final TreatmentPlanRepository plans;
    private final TreatmentPlanItemRepository items;
    private final TreatmentPlanAuditRepository audit;
    private final JdbcTemplate jdbc;

    public static void requireActive(TreatmentPlan plan) {
        if (plan.getRemovedAt() != null)
            throw new TreatmentItemException(409, "Treatment plan has been removed");
    }

    @Transactional
    public void remove(UUID id, UUID requestedClinic, String match, RemoveTreatmentPlanRequest request) {
        long expected = TreatmentPlanItemService.version(match);
        if (request == null || !RemovalReasonValidator.valid(request.getReason()))
            throw new TreatmentItemException(400, "Invalid removal reason");
        var actor = SecurityUtils.getCurrentUser();
        var clinic = clinics.resolve(actor, requestedClinic, "clinicId is required");
        var plan = plans.lockByIdAndClinicId(id, clinic.getId())
                .orElseThrow(() -> new TreatmentItemException(404, "Treatment plan not found"));
        if (plan.getVersion() != expected)
            throw new TreatmentItemException(412, "Treatment plan changed; reload before trying again");
        requireActive(plan);
        if (plan.getStatus() != TreatmentPlanStatus.DRAFT)
            throw new TreatmentItemException(409, "Only DRAFT treatment plans can be removed");
        // Include ALL child rows, even previously soft-removed items. Do not cascade removal.
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
            with children as (select id from treatment_plan_item where treatment_plan_id=?)
            select exists(
                select 1 from invoice where treatment_plan_id=?
                union all select 1 from invoice_item where treatment_plan_item_id in (select id from children)
                union all select 1 from patient_material_usage where treatment_plan_item_id in (select id from children)
                union all select 1 from patient_consent where treatment_plan_id=? or treatment_plan_item_id in (select id from children)
                union all select 1 from patient_follow_up where treatment_plan_id=? or treatment_plan_item_id in (select id from children)
            )
            """, Boolean.class, id, id, id, id)))
            throw new TreatmentItemException(409, "Treatment plan has protected downstream references");

        var retained = items.findAllByTreatmentPlanIdAndClinicIdOrderById(id, clinic.getId()).stream()
                .map(i -> new TreatmentPlanAudit.RetainedItem(i.getId(), i.getVersion())).toList();
        var snapshot = snapshot(plan);
        var now = LocalDateTime.now();
        plan.setRemovedAt(now);
        plan.setRemovedBy(actor);
        plan.setUpdatedBy(actor);
        plan.advanceVersion();
        plans.saveAndFlush(plan);

        var event = new TreatmentPlanAudit();
        event.setClinicId(clinic.getId());
        event.setTreatmentPlanId(id);
        event.setActorId(actor.getId());
        event.setCreatedAt(now);
        event.setOperation("REMOVE");
        event.setRemovalReason(request.getReason());
        snapshot.put("removedAt", now.toString());
        snapshot.put("removedBy", actor.getId());
        snapshot.put("resultingVersion", plan.getVersion());
        event.setPlanSnapshot(snapshot);
        event.setRetainedItems(retained);
        audit.save(event); // Commit/flush failure propagates before the controller can return 204.
    }

    private static Map<String, Object> snapshot(TreatmentPlan p) {
        var s = new LinkedHashMap<String, Object>();
        s.put("id", p.getId()); s.put("clinicId", p.getClinic().getId());
        s.put("patientId", p.getPatient().getId()); s.put("doctorId", p.getDoctor().getId());
        s.put("caseSheetId", p.getCaseSheet() == null ? null : p.getCaseSheet().getId());
        s.put("consultationId", p.getConsultation() == null ? null : p.getConsultation().getId());
        s.put("appointmentId", p.getAppointment() == null ? null : p.getAppointment().getId());
        s.put("treatmentPlanNumber", p.getTreatmentPlanNumber()); s.put("title", p.getTitle());
        s.put("status", p.getStatus().name()); s.put("version", p.getVersion());
        s.put("subtotal", p.getSubtotal()); s.put("discountType", p.getDiscountType());
        s.put("discountValue", p.getDiscountValue()); s.put("discountAmount", p.getDiscountAmount());
        s.put("finalAmount", p.getFinalAmount()); s.put("estimatedTotalVisits", p.getEstimatedTotalVisits());
        s.put("notes", p.getNotes()); s.put("patientNotes", p.getPatientNotes());
        s.put("presentedAt", Objects.toString(p.getPresentedAt(), null));
        s.put("approvedAt", Objects.toString(p.getApprovedAt(), null));
        s.put("declinedAt", Objects.toString(p.getDeclinedAt(), null));
        s.put("completedAt", Objects.toString(p.getCompletedAt(), null));
        s.put("cancelledAt", Objects.toString(p.getCancelledAt(), null));
        s.put("approvalNotes", p.getApprovalNotes()); s.put("declineReason", p.getDeclineReason());
        s.put("cancellationReason", p.getCancellationReason());
        s.put("createdAt", Objects.toString(p.getCreatedAt(), null));
        s.put("updatedAt", Objects.toString(p.getUpdatedAt(), null));
        s.put("createdBy", p.getCreatedBy() == null ? null : p.getCreatedBy().getId());
        s.put("updatedBy", p.getUpdatedBy() == null ? null : p.getUpdatedBy().getId());
        return s;
    }
}
