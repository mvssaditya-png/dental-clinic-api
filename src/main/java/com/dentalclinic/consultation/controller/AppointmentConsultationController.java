package com.dentalclinic.consultation.controller;

import com.dentalclinic.consultation.dto.ConsultationResponse;
import com.dentalclinic.consultation.service.ConsultationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentConsultationController {

    private final ConsultationService consultationService;

    @GetMapping("/{appointmentId}/consultation")
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_VIEW')"
    )
    public ResponseEntity<ConsultationResponse>
    getConsultationByAppointment(
            @PathVariable UUID appointmentId,
            @RequestParam(required = false)
            UUID clinicId
    ) {

        return ResponseEntity.ok(
                consultationService
                        .getConsultationByAppointment(
                                appointmentId,
                                clinicId
                        )
        );
    }
}