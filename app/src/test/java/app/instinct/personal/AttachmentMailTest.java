package app.instinct.personal;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class AttachmentMailTest {
    @Test public void attachmentBytesAndThreadSurviveMimeRoundTrip()throws Exception {
        Session session=Session.getInstance(new Properties());byte[] bytes={0,1,2,10,13,(byte)255};
        MimeMessage message=OutboundMail.create(session,"Please read this","tester@gmail.com","tester@mail.instinct.com","Files","<original@example.com>",List.of(new MailAttachment("résumé.pdf","application/pdf",bytes)));
        ByteArrayOutputStream output=new ByteArrayOutputStream();message.writeTo(output);
        MimeMessage read=new MimeMessage(session,new ByteArrayInputStream(output.toByteArray()));
        assertEquals("<original@example.com>",read.getHeader("In-Reply-To",null));
        Multipart parts=(Multipart)read.getContent();assertEquals(2,parts.getCount());assertTrue(parts.getBodyPart(0).getContent().toString().contains("Please read this"));
        assertEquals("résumé.pdf",MimeUtility.decodeText(parts.getBodyPart(1).getFileName()));
        assertArrayEquals(bytes,parts.getBodyPart(1).getInputStream().readAllBytes());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsCombinedOversize(){MailAttachment.validate(List.of(new MailAttachment("a","text/plain",new byte[7*1024*1024]),new MailAttachment("b","text/plain",new byte[6*1024*1024])));}
    @Test public void stripsFilenameControls(){assertEquals("file__Bcc: other",new MailAttachment("file\r\nBcc: other","bad\r\ntype",new byte[0]).name);assertEquals("application/octet-stream",new MailAttachment("file","bad\r\ntype",new byte[0]).type);}
    @Test public void attachmentOnlyEmailHasTextPart()throws Exception{MimeMessage message=OutboundMail.create(Session.getInstance(new Properties()),"","tester@gmail.com","tester@mail.instinct.com","File",null,List.of(new MailAttachment("note.txt","text/plain",new byte[]{65})));assertEquals(2,((Multipart)message.getContent()).getCount());}
}
