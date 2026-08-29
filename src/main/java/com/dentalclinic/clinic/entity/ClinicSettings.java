package com.dentalclinic.clinic.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "clinic_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false, unique = true)
    private Clinic clinic;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "date_format", nullable = false, length = 30)
    private String dateFormat;

    @Column(name = "time_format", nullable = false, length = 10)
    private String timeFormat;

    @Column(name = "appointment_slot_minutes", nullable = false)
    private Integer appointmentSlotMinutes;

    @Column(name = "allow_walk_in", nullable = false)
    private Boolean allowWalkIn;

    @Column(name = "enable_whatsapp", nullable = false)
    private Boolean enableWhatsapp;

    @Column(name = "enable_sms", nullable = false)
    private Boolean enableSms;

    @Column(name = "enable_email", nullable = false)
    private Boolean enableEmail;

    @Column(name = "enable_ai_assistant", nullable = false)
    private Boolean enableAiAssistant;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings_json", columnDefinition = "jsonb")
    private Map<String, Object> settingsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (currency == null) {
            currency = "INR";
        }

        if (dateFormat == null) {
            dateFormat = "DD-MM-YYYY";
        }

        if (timeFormat == null) {
            timeFormat = "12_HOUR";
        }

        if (appointmentSlotMinutes == null) {
            appointmentSlotMinutes = 30;
        }

        if (allowWalkIn == null) {
            allowWalkIn = true;
        }

        if (enableWhatsapp == null) {
            enableWhatsapp = false;
        }

        if (enableSms == null) {
            enableSms = false;
        }

        if (enableEmail == null) {
            enableEmail = false;
        }

        if (enableAiAssistant == null) {
            enableAiAssistant = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}