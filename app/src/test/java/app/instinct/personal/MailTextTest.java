package app.instinct.personal;
import org.junit.Test;
import static org.junit.Assert.*;
public class MailTextTest {
    @Test public void hidesQuotedHistoryButKeepsReply() { assertEquals("Yes, go ahead.",MailText.conversational("Yes, go ahead.\r\n\r\nOn Fri, Sep 11, 2026, 8:25 PM Instinct wrote:\r\n\r\n> Old email\r\n> More")); }
    @Test public void retainsUnrelatedQuotedProse() { assertEquals("Please use this:\n> quoted text",MailText.conversational("Please use this:\n> quoted text")); }
    @Test public void rejectsLookalikeSenders() { assertFalse(MailText.exactAddress("tester@mail.instinct.com.evil.test","tester@mail.instinct.com")); assertFalse(MailText.exactAddress("other@instinct.com","tester@mail.instinct.com")); }
    @Test public void preservesUnicode() { assertEquals("Let’s do it 🔥",MailText.conversational("Let’s do it 🔥")); }
}
