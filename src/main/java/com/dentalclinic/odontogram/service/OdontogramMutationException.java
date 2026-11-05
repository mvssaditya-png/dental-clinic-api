package com.dentalclinic.odontogram.service;

public class OdontogramMutationException extends RuntimeException {
    public final int status;
    public OdontogramMutationException(int status, String message) { super(message); this.status = status; }
}
