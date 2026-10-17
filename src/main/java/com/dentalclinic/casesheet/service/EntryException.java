package com.dentalclinic.casesheet.service;
public class EntryException extends RuntimeException {
 public final int status;
 public EntryException(int status,String message){super(message);this.status=status;}
}
