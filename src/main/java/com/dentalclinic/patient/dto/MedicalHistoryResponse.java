package com.dentalclinic.patient.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MedicalHistoryResponse {

    private Boolean diabetes;
    private Boolean hypertension;
    private Boolean thyroid;
    private Boolean heartDisease;
    private Boolean kidneyDisease;
    private Boolean pregnancy;
    private Boolean asthma;
    private Boolean allergies;
    private Boolean tobacco;
    private Boolean alcohol;
    private Boolean smoking;

    private String allergyNotes;
    private String otherConditions;
}