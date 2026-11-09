package com.dentalclinic.auth;
import com.dentalclinic.auth.service.LocalOtpDelivery;
import org.springframework.mock.env.MockEnvironment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LocalOtpDeliveryTest {
 @Test void disabledByDefaultProducesNoOutput(){new LocalOtpDelivery(false,new MockEnvironment()).deliver("synthetic-value");}
 @Test void cannotEnableWithoutExclusiveLocalProfileAndLoopback(){assertThrows(IllegalStateException.class,()->new LocalOtpDelivery(true,new MockEnvironment()).validate());var env=new MockEnvironment();env.setActiveProfiles("local-web");assertThrows(IllegalStateException.class,()->new LocalOtpDelivery(true,env).validate());env.setProperty("server.address","0.0.0.0");assertThrows(IllegalStateException.class,()->new LocalOtpDelivery(true,env).validate());}
 @Test void localBoundConfigurationAllowedButProductionProfileRejected(){var env=new MockEnvironment();env.setActiveProfiles("local-web");env.setProperty("server.address","127.0.0.1");new LocalOtpDelivery(true,env).validate();env.setActiveProfiles("local-web","production");assertThrows(IllegalStateException.class,()->new LocalOtpDelivery(true,env).validate());}
}
