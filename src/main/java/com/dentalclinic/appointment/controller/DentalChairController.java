package com.dentalclinic.appointment.controller;

import com.dentalclinic.appointment.dto.DentalChairResponse;
import com.dentalclinic.appointment.service.DentalChairService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dental-chairs")
@RequiredArgsConstructor
public class DentalChairController {

    private final DentalChairService dentalChairService;

    @GetMapping
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public List<DentalChairResponse> getDentalChairs(
            @RequestParam(required = false) UUID clinicId
    ) {

        return dentalChairService.getDentalChairs(
                clinicId
        );
    }

    @GetMapping("/{chairId}")
    @PreAuthorize("hasAuthority('APPOINTMENT_VIEW')")
    public DentalChairResponse getDentalChair(
            @PathVariable UUID chairId,
            @RequestParam(required = false) UUID clinicId
    ) {

        return dentalChairService.getDentalChair(
                chairId,
                clinicId
        );
    }
}