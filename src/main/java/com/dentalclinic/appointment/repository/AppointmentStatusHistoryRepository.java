package com.dentalclinic.appointment.repository;

import com.dentalclinic.appointment.entity.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentStatusHistoryRepository
        extends JpaRepository<AppointmentStatusHistory, UUID> {

    List<AppointmentStatusHistory>
    findAllByAppointmentIdOrderByChangedAtAsc(UUID appointmentId);
}