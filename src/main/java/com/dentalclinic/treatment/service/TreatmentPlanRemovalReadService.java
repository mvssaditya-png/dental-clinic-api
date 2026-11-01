package com.dentalclinic.treatment.service;

import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.entity.TreatmentPlan;
import com.dentalclinic.treatment.entity.TreatmentPlanAudit;
import com.dentalclinic.treatment.repository.TreatmentPlanAuditRepository;
import com.dentalclinic.treatment.repository.TreatmentPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class TreatmentPlanRemovalReadService {
    private final TenantClinicResolver clinics;
    private final TreatmentPlanRepository plans;
    private final TreatmentPlanAuditRepository audit;

    private TreatmentPlan parent(UUID id, UUID requestedClinic) {
        var clinic = clinics.resolve(SecurityUtils.getCurrentUser(), requestedClinic, "clinicId is required");
        return plans.findByIdAndClinicId(id, clinic.getId())
                .orElseThrow(() -> new TreatmentItemException(404, "Treatment plan not found"));
    }

    public record RemovalState(UUID id, long version, boolean removed) {}
    public RemovalState state(UUID id, UUID clinicId) {
        var plan = parent(id, clinicId);
        return new RemovalState(plan.getId(), plan.getVersion(), plan.getRemovedAt() != null);
    }

    public record Event(UUID id, String operation, UUID actorId, LocalDateTime timestamp,
                        String removalReason, Map<String, Object> planSnapshot,
                        List<TreatmentPlanAudit.RetainedItem> retainedItems) {}
    public record HistoryPage(List<Event> content, int number, int size, long totalElements, int totalPages) {}

    public HistoryPage history(UUID id, UUID clinicId, int page, int size) {
        if (page < 0 || size < 1 || (long) page * Math.min(size, 100) > Integer.MAX_VALUE)
            throw new TreatmentItemException(400, "Invalid page or size");
        var plan = parent(id, clinicId);
        var result = audit.findByClinicIdAndTreatmentPlanId(plan.getClinic().getId(), id,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new HistoryPage(result.map(e -> new Event(e.getId(), e.getOperation(), e.getActorId(),
                e.getCreatedAt(), e.getRemovalReason(), e.getPlanSnapshot(), e.getRetainedItems())).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
