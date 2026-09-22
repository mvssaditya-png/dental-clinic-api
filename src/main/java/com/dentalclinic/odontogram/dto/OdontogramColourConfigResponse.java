package com.dentalclinic.odontogram.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class OdontogramColourConfigResponse {

    private UUID id;

    private UUID clinicId;

    private UUID conditionId;

    private String conditionCode;

    private String conditionName;

    private String colourHex;

    private Integer displayOrder;

    private Boolean active;
}