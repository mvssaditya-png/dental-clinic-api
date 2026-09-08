package com.dentalclinic.casesheet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCaseSheetRequest {

    private String chiefComplaint;

    private String historyOfPresentIllness;

    private String clinicalExamination;

    private String diagnosisSummary;

    private String treatmentNotes;
}