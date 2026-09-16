package com.dentalclinic.billing.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class IssueInvoiceRequest {

    private UUID clinicId;
}