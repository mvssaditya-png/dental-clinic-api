package com.dentalclinic.casesheet.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FinalizeCaseSheetRequest {

    private UUID clinicId;
}