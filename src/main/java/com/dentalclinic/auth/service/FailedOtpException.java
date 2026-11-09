package com.dentalclinic.auth.service;

/** A rejected attempt whose counter must commit; no login state has been changed. */
public class FailedOtpException extends IllegalArgumentException {
    public FailedOtpException(String message) { super(message); }
}
