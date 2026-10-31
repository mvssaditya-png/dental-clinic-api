package com.dentalclinic.treatment.dto;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class UpdateTreatmentPlanItemRequest {
 private String toothNumber;
 @NotNull @Positive private Integer quantity;
 @Positive private Integer estimatedVisits;
 private String description; private String clinicalNotes; private String patientExplanation; private String followUpInstructions;
}
