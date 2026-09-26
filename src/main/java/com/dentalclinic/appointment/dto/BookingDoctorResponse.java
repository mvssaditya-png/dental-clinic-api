package com.dentalclinic.appointment.dto;

import java.util.UUID;

/** Minimal appointment-booking projection; does not expose account/contact or pricing details. */
public record BookingDoctorResponse(UUID doctorId, String firstName, String lastName, String specialization) {}
