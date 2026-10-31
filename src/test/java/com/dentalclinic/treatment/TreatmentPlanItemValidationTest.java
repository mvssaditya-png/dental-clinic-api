package com.dentalclinic.treatment;
import com.dentalclinic.treatment.dto.*;
import com.dentalclinic.treatment.service.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TreatmentPlanItemValidationTest {
 @Test void strictVersions(){assertEquals(0,TreatmentPlanItemService.version("\"0\""));assertEquals(Long.MAX_VALUE,TreatmentPlanItemService.version("\"9223372036854775807\""));assertEquals(428,assertThrows(TreatmentItemException.class,()->TreatmentPlanItemService.version(null)).status);for(String invalid:new String[]{"","0","*","W/\"0\"","\"-1\"","\"1\", \"2\"","\"9223372036854775808\""})assertEquals(400,assertThrows(TreatmentItemException.class,()->TreatmentPlanItemService.version(invalid)).status);}
 @Test void unicodeReasonBoundaries(){try(var factory=Validation.buildDefaultValidatorFactory()){var v=factory.getValidator();var r=new RemoveTreatmentPlanItemRequest();String supplementary=new String(Character.toChars(0x1F9B7));for(String valid:new String[]{"Correction","\u0c24\u0c2a\u0c4d\u0c2a\u0c41","\u0917\u0932\u0924",supplementary.repeat(1000)}){r.setReason(valid);assertTrue(v.validate(r).isEmpty());}for(String invalid:new String[]{null,"","   ","\u00a0","bad\nreason",supplementary.repeat(1001)}){r.setReason(invalid);assertFalse(v.validate(r).isEmpty());}}}
 @Test void requiredPositiveIntegerFields(){try(var factory=Validation.buildDefaultValidatorFactory()){var v=factory.getValidator();var r=new UpdateTreatmentPlanItemRequest();assertFalse(v.validate(r).isEmpty());r.setQuantity(0);assertFalse(v.validate(r).isEmpty());r.setQuantity(Integer.MAX_VALUE);assertTrue(v.validate(r).isEmpty());r.setEstimatedVisits(0);assertFalse(v.validate(r).isEmpty());r.setEstimatedVisits(1);assertTrue(v.validate(r).isEmpty());}}
}
