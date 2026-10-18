package com.dentalclinic.casesheet.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RemovalReasonValidator implements ConstraintValidator<ValidRemovalReason, String> {
    public static boolean valid(String value) {
        if (value == null || value.codePointCount(0, value.length()) > 1000) return false;
        // Reject C0/C1 controls and unpaired UTF-16 surrogates, not Indic joiners or combining marks.
        if (value.codePoints().anyMatch(cp -> Character.isISOControl(cp)
                || (cp >= 0xD800 && cp <= 0xDFFF))) return false;
        return value.codePoints().anyMatch(cp -> !Character.isWhitespace(cp) && !Character.isSpaceChar(cp));
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return valid(value);
    }
}
