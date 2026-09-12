package app.instinct.personal;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Properties;
import java.io.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class OutboundMailTest {
    private MimeMessage message(String reply) throws Exception {
        return OutboundMail.create(Session.getInstance(new Properties()),"Hello 🔥\nNew idea","tester@gmail.com","tester@mail.instinct.com","Instinct conversation",reply);
    }
    @Test public void sendsOnlyToVerifiedInstinctAddress() throws Exception {
        MimeMessage m=message("<thread@instinct.example>");
        assertEquals(1,m.getAllRecipients().length);assertEquals("tester@mail.instinct.com",((InternetAddress)m.getAllRecipients()[0]).getAddress());
        assertEquals("tester@gmail.com",((InternetAddress)m.getFrom()[0]).getAddress());
    }
    @Test public void preservesThreadAndUnicodeOnWire() throws Exception {
        MimeMessage m=message("<thread@instinct.example>");ByteArrayOutputStream wire=new ByteArrayOutputStream();m.writeTo(wire);
        MimeMessage read=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(wire.toByteArray()));
        assertEquals("<thread@instinct.example>",read.getHeader("In-Reply-To",null));
        assertEquals(read.getHeader("References",null),read.getHeader("In-Reply-To",null));
        assertEquals("Re: Instinct conversation",read.getSubject());
        assertEquals("Hello 🔥\nNew idea",read.getContent().toString().replace("\r\n","\n"));
    }
    @Test public void eachUserSendHasDistinctMessageId() throws Exception {assertNotEquals(message(null).getMessageID(),message(null).getMessageID());}
    @Test public void blocksHeaderInjectionFromReplyId() throws Exception {assertNull(message("<id>\r\nBcc: somebody@example.com").getHeader("In-Reply-To"));}
}
