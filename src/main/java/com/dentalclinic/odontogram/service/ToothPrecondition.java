package com.dentalclinic.odontogram.service;

/** Explicit preconditions for replacement of an existing observation or create-only insertion. */
public record ToothPrecondition(Long version, boolean createOnly) {
    public static ToothPrecondition parse(String match, String noneMatch) {
        if (match == null && noneMatch == null) throw new OdontogramMutationException(428, "If-Match or If-None-Match is required");
        if (match != null && noneMatch != null) throw new OdontogramMutationException(400, "Supply only one tooth precondition");
        if (noneMatch != null) {
            if (!"*".equals(noneMatch)) throw new OdontogramMutationException(400, "If-None-Match must be *");
            return new ToothPrecondition(null, true);
        }
        if (!match.matches("\"[0-9]+\"")) throw new OdontogramMutationException(400, "If-Match must be one quoted nonnegative version");
        try { return new ToothPrecondition(Long.parseLong(match.substring(1, match.length()-1)), false); }
        catch (NumberFormatException e) { throw new OdontogramMutationException(400, "Invalid If-Match version"); }
    }
    public void verify(Long actual) {
        if (createOnly ? actual != null : actual == null || !version.equals(actual))
            throw new OdontogramMutationException(412, "Tooth state changed. Reload the tooth and review before saving.");
    }
    public static String adultNumber(String number) {
        String normalized = number == null ? "" : number.trim();
        if (!normalized.matches("^[1-4][1-8]$")) throw new OdontogramMutationException(400, "A valid adult FDI tooth number is required");
        return normalized;
    }
}
