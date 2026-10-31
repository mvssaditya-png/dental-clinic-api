package com.dentalclinic.treatment.controller;
import com.dentalclinic.treatment.dto.*;import com.dentalclinic.treatment.service.TreatmentPlanItemService;
import org.springframework.web.bind.annotation.*;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.http.ResponseEntity;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import java.util.UUID;
@RestController @RequestMapping("/api/treatment-plans/{planId}") @RequiredArgsConstructor
public class TreatmentPlanItemController {
 private final TreatmentPlanItemService service;
 @PutMapping("/items/{itemId}") @PreAuthorize("hasAuthority('TREATMENT_PLAN_EDIT')")
 public TreatmentPlanItemResponse update(@PathVariable UUID planId,@PathVariable UUID itemId,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody UpdateTreatmentPlanItemRequest request){return service.update(planId,itemId,clinicId,match,request);}
 @PostMapping("/items/{itemId}/remove") @PreAuthorize("hasAuthority('TREATMENT_PLAN_EDIT')")
 public ResponseEntity<Void> remove(@PathVariable UUID planId,@PathVariable UUID itemId,@RequestParam(required=false) UUID clinicId,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody RemoveTreatmentPlanItemRequest request){service.remove(planId,itemId,clinicId,match,request);return ResponseEntity.noContent().build();}
 @GetMapping("/item-history") @PreAuthorize("hasAuthority('TREATMENT_PLAN_HISTORY_VIEW')")
 public TreatmentPlanItemService.HistoryPage history(@PathVariable UUID planId,@RequestParam(required=false) UUID clinicId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.history(planId,clinicId,page,size);}
}
