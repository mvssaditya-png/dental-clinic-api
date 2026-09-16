package com.dentalclinic.treatment.dto;

import com.dentalclinic.treatment.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UpdateTreatmentPlanRequest {

    private UUID clinicId;

    private String title;

    private DiscountType discountType;

    @DecimalMin(value = "0.00")
    private BigDecimal discountValue;

    @Positive
    private Integer estimatedTotalVisits;

    private String notes;

    private String patientNotes;
}