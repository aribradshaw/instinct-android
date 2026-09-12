package app.instinct.personal;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.*;

final class OutboundMail {
    static MimeMessage create(Session session,String text,String account,String peer,String subject,String replyId) throws Exception {
        return create(session,text,account,peer,subject,replyId,Collections.emptyList());
    }
    static MimeMessage create(Session session,String text,String account,String peer,String subject,String replyId,List<MailAttachment> files) throws Exception {
        MailAttachment.validate(files);
        MimeMessage msg=new MimeMessage(session);
        msg.setFrom(new InternetAddress(account));
        msg.setRecipient(Message.RecipientType.TO,new InternetAddress(peer));
        msg.setSubject(subject.toLowerCase(Locale.ROOT).startsWith("re:")?subject:"Re: "+subject,"UTF-8");
        if(replyId!=null&&replyId.startsWith("<")&&!replyId.contains("\r")&&!replyId.contains("\n")) {
            msg.setHeader("In-Reply-To",replyId);msg.setHeader("References",replyId);
        }
        msg.setSentDate(new Date());
        if(files.isEmpty())msg.setText(text,"UTF-8");
        else {
            MimeMultipart multipart=new MimeMultipart("mixed");
            MimeBodyPart body=new MimeBodyPart();body.setText(text,"UTF-8");multipart.addBodyPart(body);
            for(MailAttachment file:files){
                MimeBodyPart attachment=new MimeBodyPart();
                attachment.setDataHandler(new jakarta.activation.DataHandler(new jakarta.mail.util.ByteArrayDataSource(file.bytes,file.type)));
                attachment.setFileName(MimeUtility.encodeText(file.name,"UTF-8",null));attachment.setDisposition(Part.ATTACHMENT);multipart.addBodyPart(attachment);
            }
            msg.setContent(multipart);
        }
        msg.saveChanges();return msg;
    }
}
