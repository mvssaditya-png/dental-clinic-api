package com.dentalclinic.treatment.dto;
import com.dentalclinic.casesheet.validation.ValidRemovalReason;
import lombok.Getter; import lombok.Setter;
@Getter @Setter public class RemoveTreatmentPlanItemRequest { @ValidRemovalReason private String reason; }
