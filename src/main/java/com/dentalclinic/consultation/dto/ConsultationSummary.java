package com.dentalclinic.consultation.dto;
import com.dentalclinic.consultation.entity.ConsultationStatus;
import com.dentalclinic.appointment.entity.AppointmentStatus;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;
public record ConsultationSummary(UUID consultationId, UUID clinicId, UUID appointmentId, String appointmentNumber,
 UUID patientId, String patientName, String patientNumber, String doctorName, ConsultationStatus consultationStatus,
 AppointmentStatus appointmentStatus, LocalDateTime startedAt, LocalDateTime endedAt, LocalDate appointmentDate) {}
