package com.dentalclinic.clinic.dto;
import lombok.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.LocalDateTime;
@Getter @Setter
public class UpdateClinicStatusRequest {
    @NotNull
    private Boolean active;
}
