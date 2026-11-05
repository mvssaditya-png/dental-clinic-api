package com.dentalclinic.odontogram;
import com.dentalclinic.odontogram.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ToothPreconditionTest {
 @Test void adultFdi(){for(int q=1;q<=4;q++)for(int p=1;p<=8;p++)assertEquals(""+(q*10+p),ToothPrecondition.adultNumber(" "+(q*10+p)+" "));for(String value:new String[]{"","19","20","49","51","0011","A","1","-11"})assertEquals(400,assertThrows(OdontogramMutationException.class,()->ToothPrecondition.adultNumber(value)).status);}
 @Test void required(){assertEquals(428,assertThrows(OdontogramMutationException.class,()->ToothPrecondition.parse(null,null)).status);}
 @Test void malformed(){for(String value:new String[]{"0","*","W/\"0\"","\"-1\"","\"9223372036854775808\"","\"0\",\"1\""})assertEquals(400,assertThrows(OdontogramMutationException.class,()->ToothPrecondition.parse(value,null)).status);assertThrows(OdontogramMutationException.class,()->ToothPrecondition.parse("\"0\"","*"));assertThrows(OdontogramMutationException.class,()->ToothPrecondition.parse(null,"x"));}
 @Test void exactExisting(){ToothPrecondition.parse("\"3\"",null).verify(3L);assertEquals(412,assertThrows(OdontogramMutationException.class,()->ToothPrecondition.parse("\"3\"",null).verify(4L)).status);assertThrows(OdontogramMutationException.class,()->ToothPrecondition.parse("\"0\"",null).verify(null));}
 @Test void createOnly(){ToothPrecondition.parse(null,"*").verify(null);assertEquals(412,assertThrows(OdontogramMutationException.class,()->ToothPrecondition.parse(null,"*").verify(0L)).status);}
}
