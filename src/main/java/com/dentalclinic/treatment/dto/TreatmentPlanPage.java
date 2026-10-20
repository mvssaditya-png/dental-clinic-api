package com.dentalclinic.treatment.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record TreatmentPlanPage(List<TreatmentPlanSummary> content, int number, int size,
        long totalElements, int totalPages) {
    public static TreatmentPlanPage from(Page<TreatmentPlanSummary> page) {
        return new TreatmentPlanPage(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
