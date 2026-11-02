package com.dentalclinic.consultation.dto;

import java.util.List;

public record ClinicalVisitPage(List<ClinicalVisitSummary> content, int number, int size,
        long totalElements, int totalPages) {}
