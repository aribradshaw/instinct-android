package app.instinct.personal;

import android.content.Context;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class WhatsAppRepository {
    private final Vault vault;
    WhatsAppRepository(Context context){vault=new Vault(context);}
    synchronized JSONObject config()throws Exception{return new JSONObject(vault.read("whatsapp-config.enc","{}"));}
    synchronized boolean configured()throws Exception{return !config().optString("chat").isBlank();}
    synchronized String chat()throws Exception{return config().optString("chat");}
    synchronized String phone()throws Exception{return config().optString("phone");}
    synchronized void configure(String chat,String phone)throws Exception{
        chat=chat.trim();phone=WhatsAppNotificationService.cleanPhone(phone);
        if(chat.isBlank()||chat.length()>120)throw new IllegalArgumentException("Enter the exact WhatsApp chat name for Instinct.");
        vault.write("whatsapp-config.enc",new JSONObject().put("chat",chat).put("phone",phone).toString());
    }
    synchronized void disconnect(){vault.delete("whatsapp-config.enc");vault.delete("whatsapp-messages.enc");}
    synchronized JSONArray cached()throws Exception{return new JSONArray(vault.read("whatsapp-messages.enc","[]"));}
    synchronized void add(String sourceId,String text,long time,boolean outgoing)throws Exception{
        text=text==null?"":text.trim();if(text.isEmpty())return;
        JSONArray old=cached();String id="wa-"+digest(sourceId+"\n"+time+"\n"+text+"\n"+outgoing);
        for(int i=0;i<old.length();i++)if(id.equals(old.getJSONObject(i).optString("id")))return;
        old.put(new JSONObject().put("id",id).put("channel","whatsapp").put("outgoing",outgoing).put("text",text).put("raw",text)
            .put("time",time).put("status",outgoing?"submitted":"received").put("attachments",new JSONArray()).put("previews",new JSONArray()));
        while(old.length()>500)old.remove(0);
        vault.write("whatsapp-messages.enc",MailRepository.sorted(old).toString());
    }
    private static String digest(String value)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();for(int i=0;i<12;i++)out.append(String.format("%02x",bytes[i]));return out.toString();
    }
}
