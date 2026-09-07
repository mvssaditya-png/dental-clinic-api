package com.dentalclinic.appointment.controller;

import com.dentalclinic.appointment.dto.AppointmentResponse;
import com.dentalclinic.appointment.dto.CreateAppointmentRequest;
import com.dentalclinic.appointment.dto.UpdateAppointmentStatusRequest;
import com.dentalclinic.appointment.service.AppointmentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize(
            "hasAuthority('APPOINTMENT_CREATE')"
    )
    public ResponseEntity<AppointmentResponse>
    createAppointment(
            @Valid
            @RequestBody CreateAppointmentRequest request
    ) {

        AppointmentResponse response =
                appointmentService
                        .createAppointment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) UUID clinicId,
            @RequestParam LocalDate date
    ) {

        return ResponseEntity.ok(
                appointmentService.getAppointments(
                        clinicId,
                        date
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID clinicId
    ) {

        return ResponseEntity.ok(
                appointmentService.getAppointmentById(
                        id,
                        clinicId
                )
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('APPOINTMENT_EDIT')")
    public ResponseEntity<AppointmentResponse>
    updateAppointmentStatus(
            @PathVariable UUID id,
            @Valid
            @RequestBody UpdateAppointmentStatusRequest request
    ) {

        return ResponseEntity.ok(
                appointmentService.updateAppointmentStatus(
                        id,
                        request
                )
        );
    }
}