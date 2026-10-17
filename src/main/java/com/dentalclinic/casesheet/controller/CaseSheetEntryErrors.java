package com.dentalclinic.casesheet.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import java.util.Map;
@RestControllerAdvice(assignableTypes=CaseSheetEntryController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CaseSheetEntryErrors {
 @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
 public ResponseEntity<Map<String,Object>> staleVersion(Exception ignored){return ResponseEntity.status(412).body(Map.of("status",412,"message","Entry changed; reload before trying again"));}
 @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
 public ResponseEntity<Map<String,Object>> lockConflict(Exception ignored){return ResponseEntity.status(409).body(Map.of("status",409,"message","Case sheet is busy; reload before trying again"));}
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
 public ResponseEntity<Map<String,Object>> invalidRequest(Exception ignored){return ResponseEntity.badRequest().body(Map.of("status",400,"message","Invalid request fields or identifiers"));}
}
