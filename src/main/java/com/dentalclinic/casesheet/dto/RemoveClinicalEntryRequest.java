package com.dentalclinic.casesheet.dto;

import com.dentalclinic.casesheet.validation.ValidRemovalReason;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoveClinicalEntryRequest {
    @ValidRemovalReason
    private String reason;
}
