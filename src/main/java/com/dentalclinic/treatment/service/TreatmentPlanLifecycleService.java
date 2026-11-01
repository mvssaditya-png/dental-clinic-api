package com.dentalclinic.treatment.service;

import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.security.util.SecurityUtils;
import com.dentalclinic.treatment.dto.TreatmentPlanLifecyclePage;
import com.dentalclinic.treatment.repository.TreatmentPlanRepository;
import com.dentalclinic.treatment.repository.TreatmentPlanStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TreatmentPlanLifecycleService {
    private final TenantClinicResolver clinics;
    private final TreatmentPlanRepository plans;
    private final TreatmentPlanStatusHistoryRepository history;

    public TreatmentPlanLifecyclePage read(UUID id, UUID requestedClinic, int page, int size) {
        if (page < 0 || size < 1 || (long) page * Math.min(size, 100) > Integer.MAX_VALUE)
            throw new TreatmentItemException(400, "Invalid page or size");
        var clinic = clinics.resolve(SecurityUtils.getCurrentUser(), requestedClinic, "clinicId is required");
        plans.findByIdAndClinicId(id, clinic.getId())
                .orElseThrow(() -> new TreatmentItemException(404, "Treatment plan not found"));
        var result = history.readLifecycle(clinic.getId(), id, PageRequest.of(page, Math.min(size, 100)));
        var events = result.map(e -> {
            var actor = e.getChangedBy();
            String name = actor == null ? null : ((actor.getFirstName() == null ? "" : actor.getFirstName()) + " " +
                    (actor.getLastName() == null ? "" : actor.getLastName())).strip();
            return new TreatmentPlanLifecyclePage.Event(e.getId(), clinic.getId(), id, e.getFromStatus(),
                    e.getToStatus(), actor == null ? null : actor.getId(), name == null || name.isBlank() ? null : name,
                    e.getChangedAt(), e.getReason());
        });
        return new TreatmentPlanLifecyclePage(events.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
