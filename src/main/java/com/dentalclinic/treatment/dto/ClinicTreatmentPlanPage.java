package com.dentalclinic.treatment.dto;
import java.util.List;
import org.springframework.data.domain.Page;
public record ClinicTreatmentPlanPage(List<ClinicTreatmentPlanSummary> content, int number, int size, long totalElements, int totalPages) {
 public static ClinicTreatmentPlanPage from(Page<ClinicTreatmentPlanSummary> page) {
  return new ClinicTreatmentPlanPage(page.getContent(),page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
 }
}
