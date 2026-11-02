package com.dentalclinic.consultation.service;

import com.dentalclinic.clinic.service.TenantClinicResolver;
import com.dentalclinic.consultation.dto.ClinicalVisitPage;
import com.dentalclinic.consultation.repository.ConsultationRepository;
import com.dentalclinic.patient.repository.PatientRepository;
import com.dentalclinic.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicalVisitService {
    private final TenantClinicResolver clinics;
    private final PatientRepository patients;
    private final ConsultationRepository consultations;

    public ClinicalVisitPage read(UUID patientId, UUID requestedClinic, int page, int size, UUID excluded) {
        if (page < 0 || size < 1 || (long) page * Math.min(size, 100) > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid page or size");
        var clinic = clinics.resolve(SecurityUtils.getCurrentUser(), requestedClinic, "clinicId is required");
        patients.findByIdAndClinicId(patientId, clinic.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        var result = consultations.readClinicalVisits(clinic.getId(), patientId, excluded,
                PageRequest.of(page, Math.min(size, 100)));
        return new ClinicalVisitPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
