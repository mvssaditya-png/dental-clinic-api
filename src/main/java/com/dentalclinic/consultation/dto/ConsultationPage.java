package com.dentalclinic.consultation.dto;
import java.util.List;
public record ConsultationPage(List<ConsultationSummary> content, int number, int size, long totalElements, int totalPages) {}
