package com.dentalclinic.casesheet;
import com.dentalclinic.casesheet.validation.RemovalReasonValidator;
import com.dentalclinic.casesheet.dto.RemoveClinicalEntryRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RemovalReasonTest {
 @Test void acceptsLanguagesAndPreservesJoiners(){for(String s:java.util.List.of("Incorrect tooth number", "\u0c24\u0c2a\u0c4d\u0c2a\u0c41", "\u0917\u0932\u0924", "\u0915\u094d\u200d\u0937", " e\u0301 \u2014 \ud83d\ude00 "))assertTrue(RemovalReasonValidator.valid(s));}
 @Test void countsCodePointsNotUtf16Units(){String emoji="\ud83d\ude00";assertTrue(RemovalReasonValidator.valid(emoji.repeat(1000)));assertFalse(RemovalReasonValidator.valid(emoji.repeat(1001)));assertTrue(RemovalReasonValidator.valid("a".repeat(999)+emoji));assertFalse(RemovalReasonValidator.valid("a".repeat(1000)+emoji));}
 @Test void bmpAndCombiningBoundaries(){assertTrue(RemovalReasonValidator.valid("\u0c24".repeat(1000)));assertFalse(RemovalReasonValidator.valid("\u0c24".repeat(1001)));assertTrue(RemovalReasonValidator.valid("e\u0301".repeat(500)));assertFalse(RemovalReasonValidator.valid("e\u0301".repeat(500)+"a"));}
 @Test void rejectsBlankIncludingUnicodeSpaces(){assertFalse(RemovalReasonValidator.valid(null));for(String s:java.util.List.of(""," ","\u00a0\u2003\u202f","\t\n"))assertFalse(RemovalReasonValidator.valid(s));}
 @Test void rejectsControlsAndMalformedUnicode(){for(String s:java.util.List.of("a\n","a\t","a\u0000","a\u007f","a\u0085","a\ud800","a\udc00"))assertFalse(RemovalReasonValidator.valid(s));}
 @Test void typedDtoUsesSameValidation(){try(var f=Validation.buildDefaultValidatorFactory()){var r=new RemoveClinicalEntryRequest();assertFalse(f.getValidator().validate(r).isEmpty());r.setReason("\ud83d\ude00".repeat(1000));assertTrue(f.getValidator().validate(r).isEmpty());r.setReason("\ud83d\ude00".repeat(1001));assertFalse(f.getValidator().validate(r).isEmpty());}}
}
