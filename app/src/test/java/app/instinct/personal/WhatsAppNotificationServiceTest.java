package app.instinct.personal;
import org.junit.Test;
import static org.junit.Assert.*;

public class WhatsAppNotificationServiceTest {
    @Test public void acceptsOnlyOfficialWhatsAppPackages(){assertTrue(WhatsAppNotificationService.supportedPackage("com.whatsapp"));assertTrue(WhatsAppNotificationService.supportedPackage("com.whatsapp.w4b"));assertFalse(WhatsAppNotificationService.supportedPackage("com.example.whatsapp"));}
    @Test public void matchesOnlyExactConfiguredChat(){assertTrue(WhatsAppNotificationService.matches("Instinct"," instinct "));assertFalse(WhatsAppNotificationService.matches("Instinct","Instinct Support"));assertFalse(WhatsAppNotificationService.matches("", "Instinct"));}
    @Test public void phoneIsDigitsOnlyAndBounded(){assertEquals("16025550123",WhatsAppNotificationService.cleanPhone("+1 (602) 555-0123"));assertEquals("",WhatsAppNotificationService.cleanPhone(""));try{WhatsAppNotificationService.cleanPhone("123");fail();}catch(IllegalArgumentException expected){}}
}
