package com.dentalclinic.consultation.controller;
import com.dentalclinic.casesheet.controller.CaseSheetController;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import java.util.Map;
@RestControllerAdvice(assignableTypes={ConsultationController.class,CaseSheetController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClinicalLifecycleErrors {
 @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
 public ResponseEntity<Map<String,Object>> stale(Exception e){return ResponseEntity.status(412).body(Map.of("status",412,"message","Clinical record changed; reload before saving"));}
 @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
 public ResponseEntity<Map<String,Object>> busy(Exception e){return ResponseEntity.status(409).body(Map.of("status",409,"message","Clinical record is busy; reload before retrying"));}
}
