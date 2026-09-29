package com.dentalclinic.clinic.service;
import com.dentalclinic.clinic.dto.*;
import com.dentalclinic.clinic.entity.*;
import com.dentalclinic.clinic.repository.*;
import com.dentalclinic.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.sql.SQLException;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class ClinicService {
    private final ClinicRepository clinics;
    private final ClinicSettingsRepository settings;
    private final ClinicAccessPolicy access;

    @Transactional(readOnly = true)
    public Page<ClinicSummaryResponse> list(String search, Boolean active, int page, int size) {
        return clinics.searchClinics(search == null ? "" : search.trim(), active,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))))
                .map(c -> ClinicSummaryResponse.builder().id(c.getId()).clinicCode(c.getClinicCode())
                    .clinicName(c.getClinicName()).city(c.getCity()).state(c.getState()).active(c.getActive()).build());
    }
    @Transactional(readOnly = true)
    public ClinicResponse get(UUID id) { return response(find(id)); }

    @Transactional
    public ClinicResponse create(CreateClinicRequest request) {
        String code = request.getClinicCode().trim();
        if (code.isEmpty()) throw new IllegalArgumentException("Clinic code is required");
        if (clinics.existsByClinicCode(code)) throw new IllegalArgumentException("Clinic code already exists");
        validateTimezone(request.getTimezone());
        Clinic clinic = new Clinic();
        clinic.setClinicCode(code);
        clinic.setClinicName(request.getClinicName());
        clinic.setPhone(request.getPhone());
        clinic.setEmail(request.getEmail());
        clinic.setAddressLine1(request.getAddressLine1());
        clinic.setAddressLine2(request.getAddressLine2());
        clinic.setCity(request.getCity());
        clinic.setState(request.getState());
        clinic.setCountry(request.getCountry());
        clinic.setPincode(request.getPincode());
        clinic.setTimezone(request.getTimezone());
        clinic.setLogoUrl(request.getLogoUrl());
        clinic.setClinicName(request.getClinicName().trim());
        try {
            clinic = clinics.saveAndFlush(clinic);
        } catch (DataIntegrityViolationException failure) {
            // Translate only this known constraint, never unrelated persistence failures.
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())
                        && sql.getMessage() != null && sql.getMessage().contains("clinic_clinic_code_key"))
                    throw new IllegalArgumentException("Clinic code already exists", failure);
            }
            throw failure;
        }
        settings.saveAndFlush(ClinicSettings.builder().clinic(clinic).build());
        return response(clinic);
    }
    @Transactional
    public ClinicResponse update(UUID id, UpdateClinicRequest request) {
        Clinic clinic = find(id);
        validateTimezone(request.getTimezone());
        clinic.setClinicName(request.getClinicName());
        clinic.setPhone(request.getPhone());
        clinic.setEmail(request.getEmail());
        clinic.setAddressLine1(request.getAddressLine1());
        clinic.setAddressLine2(request.getAddressLine2());
        clinic.setCity(request.getCity());
        clinic.setState(request.getState());
        clinic.setCountry(request.getCountry());
        clinic.setPincode(request.getPincode());
        clinic.setTimezone(request.getTimezone());
        clinic.setLogoUrl(request.getLogoUrl());
        clinic.setClinicName(request.getClinicName().trim());
        return response(clinics.saveAndFlush(clinic));
    }
    @Transactional
    public ClinicResponse updateStatus(UUID id, UpdateClinicStatusRequest request) {
        Clinic clinic = find(id);
        if (!request.getActive().equals(clinic.getActive())) {
            clinic.setActive(request.getActive());
            clinics.saveAndFlush(clinic);
        }
        return response(clinic);
    }
    @Transactional(readOnly = true)
    public ClinicSettingsResponse getSettings(UUID id) {
        settingsClinic(id);
        return settingsResponse(settings.findByClinicId(id)
                .orElseThrow(() -> new IllegalArgumentException("Clinic settings not found")));
    }
    @Transactional
    public ClinicSettingsResponse updateSettings(UUID id, UpdateClinicSettingsRequest request) {
        Clinic clinic = settingsClinic(id);
        ClinicSettings value = settings.findByClinicId(id)
                .orElseGet(() -> ClinicSettings.builder().clinic(clinic).build());
        value.setCurrency(request.getCurrency());
        value.setDateFormat(request.getDateFormat());
        value.setTimeFormat(request.getTimeFormat());
        value.setAppointmentSlotMinutes(request.getAppointmentSlotMinutes());
        value.setAllowWalkIn(request.getAllowWalkIn());
        value.setEnableWhatsapp(request.getEnableWhatsapp());
        value.setEnableSms(request.getEnableSms());
        value.setEnableEmail(request.getEnableEmail());
        value.setEnableAiAssistant(request.getEnableAiAssistant());
        value.setSettingsJson(request.getSettingsJson());
        return settingsResponse(settings.saveAndFlush(value));
    }
    private Clinic settingsClinic(UUID id) {
        var user = SecurityUtils.getCurrentUser();
        if (user.getClinic() != null) {
            if (!user.getClinic().getId().equals(id)) throw new AccessDeniedException("Clinic context mismatch");
            access.requireActive(user.getClinic());
        }
        // Management intentionally permits inactive clinics for null-clinic platform users.
        return find(id);
    }
    private Clinic find(UUID id) {
        return clinics.findById(id).orElseThrow(() -> new IllegalArgumentException("Clinic not found"));
    }
    private void validateTimezone(String timezone) {
        if (timezone == null) return;
        try { ZoneId.of(timezone); }
        catch (DateTimeException failure) { throw new IllegalArgumentException("Invalid timezone", failure); }
    }
    private ClinicResponse response(Clinic clinic) {
        return ClinicResponse.builder().id(clinic.getId()).clinicCode(clinic.getClinicCode())
                .clinicName(clinic.getClinicName())
                .phone(clinic.getPhone())
                .email(clinic.getEmail())
                .addressLine1(clinic.getAddressLine1())
                .addressLine2(clinic.getAddressLine2())
                .city(clinic.getCity())
                .state(clinic.getState())
                .country(clinic.getCountry())
                .pincode(clinic.getPincode())
                .timezone(clinic.getTimezone())
                .logoUrl(clinic.getLogoUrl()).active(clinic.getActive()).createdAt(clinic.getCreatedAt()).updatedAt(clinic.getUpdatedAt()).build();
    }
    private ClinicSettingsResponse settingsResponse(ClinicSettings value) {
        return ClinicSettingsResponse.builder().id(value.getId()).clinicId(value.getClinic().getId())
                .currency(value.getCurrency())
                .dateFormat(value.getDateFormat())
                .timeFormat(value.getTimeFormat())
                .appointmentSlotMinutes(value.getAppointmentSlotMinutes())
                .allowWalkIn(value.getAllowWalkIn())
                .enableWhatsapp(value.getEnableWhatsapp())
                .enableSms(value.getEnableSms())
                .enableEmail(value.getEnableEmail())
                .enableAiAssistant(value.getEnableAiAssistant())
                .settingsJson(value.getSettingsJson()).createdAt(value.getCreatedAt()).updatedAt(value.getUpdatedAt()).build();
    }
}

