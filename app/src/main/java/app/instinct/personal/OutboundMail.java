package app.instinct.personal;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.*;

final class OutboundMail {
    static MimeMessage create(Session session,String text,String account,String peer,String subject,String replyId) throws Exception {
        MimeMessage msg=new MimeMessage(session);
        msg.setFrom(new InternetAddress(account));
        msg.setRecipient(Message.RecipientType.TO,new InternetAddress(peer));
        msg.setSubject(subject.toLowerCase(Locale.ROOT).startsWith("re:")?subject:"Re: "+subject,"UTF-8");
        if(replyId!=null&&replyId.startsWith("<")&&!replyId.contains("\r")&&!replyId.contains("\n")) {
            msg.setHeader("In-Reply-To",replyId);msg.setHeader("References",replyId);
        }
        msg.setSentDate(new Date());msg.setText(text,"UTF-8");msg.saveChanges();return msg;
    }
}
