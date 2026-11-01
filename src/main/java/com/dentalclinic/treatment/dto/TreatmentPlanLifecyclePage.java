package com.dentalclinic.treatment.dto;

import com.dentalclinic.treatment.entity.TreatmentPlanStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TreatmentPlanLifecyclePage(List<Event> content, int number, int size, long totalElements, int totalPages) {
    public record Event(UUID id, UUID clinicId, UUID treatmentPlanId,
                        TreatmentPlanStatus fromStatus, TreatmentPlanStatus toStatus,
                        UUID actorId, String actorName, LocalDateTime changedAt, String reason) {}
}
