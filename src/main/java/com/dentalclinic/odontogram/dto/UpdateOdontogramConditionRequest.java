package com.dentalclinic.odontogram.dto;
import jakarta.validation.constraints.*;
public record UpdateOdontogramConditionRequest(
 @NotBlank @Size(max=150) String conditionName,
 @Size(max=4000) String description,
 @NotBlank @Pattern(regexp="^#[0-9a-fA-F]{6}$") String colourHex,
 @NotNull Boolean active,
 boolean terminologyOnlyConfirmation
) {}
