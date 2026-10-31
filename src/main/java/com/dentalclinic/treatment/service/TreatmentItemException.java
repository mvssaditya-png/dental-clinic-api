package com.dentalclinic.treatment.service;
public class TreatmentItemException extends RuntimeException { public final int status; public TreatmentItemException(int status,String message){super(message);this.status=status;} }
