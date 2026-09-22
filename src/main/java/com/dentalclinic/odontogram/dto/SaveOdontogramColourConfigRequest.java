package com.dentalclinic.odontogram.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SaveOdontogramColourConfigRequest {

    // Required for platform SUPER_ADMIN.
    // Normal clinic users automatically use their clinic.
    private UUID clinicId;

    @NotNull
    private UUID conditionId;

    @NotBlank
    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "colourHex must be a valid hex colour such as #FF0000"
    )
    private String colourHex;

    @NotNull
    @Min(0)
    private Integer displayOrder;

    private Boolean active;
}