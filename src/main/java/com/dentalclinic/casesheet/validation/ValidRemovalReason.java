package com.dentalclinic.casesheet.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RemovalReasonValidator.class)
public @interface ValidRemovalReason {
    String message() default "Reason must contain nonblank valid Unicode text, at most 1000 code points, without control characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
