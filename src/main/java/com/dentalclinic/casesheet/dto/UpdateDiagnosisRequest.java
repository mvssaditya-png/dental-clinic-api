package com.dentalclinic.casesheet.dto;
import com.dentalclinic.casesheet.entity.DiagnosisType;
import jakarta.validation.constraints.NotNull;
public class UpdateDiagnosisRequest extends CreateCaseSheetDiagnosisRequest {
 @Override @NotNull public DiagnosisType getDiagnosisType(){return super.getDiagnosisType();}
}
