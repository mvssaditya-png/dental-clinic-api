package com.dentalclinic.appointment.repository;

import com.dentalclinic.appointment.entity.Appointment;
import com.dentalclinic.appointment.entity.AppointmentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository
        extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByIdAndClinicId(
            UUID id,
            UUID clinicId
    );

    boolean existsByClinicIdAndAppointmentNumber(
            UUID clinicId,
            String appointmentNumber
    );

    List<Appointment> findAllByClinicIdAndAppointmentDateOrderByStartTimeAsc(
            UUID clinicId,
            LocalDate appointmentDate
    );

    List<Appointment> findAllByDoctorIdAndAppointmentDateOrderByStartTimeAsc(
            UUID doctorId,
            LocalDate appointmentDate
    );

    List<Appointment> findAllByPatientIdOrderByAppointmentDateDescStartTimeDesc(
            UUID patientId
    );

    List<Appointment> findAllByClinicIdAndAppointmentDateAndStatus(
            UUID clinicId,
            LocalDate appointmentDate,
            AppointmentStatus status
    );

    @Query("""
    SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
    FROM Appointment a
    WHERE a.clinic.id = :clinicId
      AND a.doctor.id = :doctorId
      AND a.appointmentDate = :appointmentDate
      AND a.status NOT IN (
            com.dentalclinic.appointment.entity.AppointmentStatus.CANCELLED,
            com.dentalclinic.appointment.entity.AppointmentStatus.RESCHEDULED,
            com.dentalclinic.appointment.entity.AppointmentStatus.NO_SHOW
      )
      AND a.startTime < :endTime
      AND a.endTime > :startTime
""")
    boolean existsDoctorOverlap(
            @Param("clinicId") UUID clinicId,
            @Param("doctorId") UUID doctorId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
    SELECT DISTINCT a
    FROM Appointment a
    JOIN FETCH a.clinic c
    JOIN FETCH a.patient p
    JOIN FETCH a.doctor d
    JOIN FETCH d.user du
    LEFT JOIN FETCH a.department dep
    LEFT JOIN FETCH a.chair ch
    WHERE c.id = :clinicId
      AND a.appointmentDate = :appointmentDate
    ORDER BY a.startTime ASC
""")
    List<Appointment> findDailyAppointmentsWithDetails(
            @Param("clinicId") UUID clinicId,
            @Param("appointmentDate") LocalDate appointmentDate
    );

    @Query("""
    SELECT DISTINCT a
    FROM Appointment a
    JOIN FETCH a.clinic c
    JOIN FETCH a.patient p
    JOIN FETCH a.doctor d
    JOIN FETCH d.user du
    LEFT JOIN FETCH a.department dep
    LEFT JOIN FETCH a.chair ch
    WHERE a.id = :appointmentId
      AND c.id = :clinicId
""")
    Optional<Appointment> findByIdAndClinicIdWithDetails(
            @Param("appointmentId") UUID appointmentId,
            @Param("clinicId") UUID clinicId
    );
}