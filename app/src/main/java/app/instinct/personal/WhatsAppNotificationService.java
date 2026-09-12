package app.instinct.personal;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.service.notification.*;
import java.util.*;

public final class WhatsAppNotificationService extends NotificationListenerService {
    static final String UPDATED="app.instinct.personal.WHATSAPP_UPDATED";
    private static volatile Notification.Action replyAction;
    private static volatile android.app.RemoteInput replyInput;
    private static volatile PendingIntent openChat;
    private static volatile String activeChat="";

    static boolean supportedPackage(String name){return "com.whatsapp".equals(name)||"com.whatsapp.w4b".equals(name);}
    static boolean matches(String configured,String actual){return configured!=null&&actual!=null&&!configured.isBlank()&&configured.trim().equalsIgnoreCase(actual.trim());}
    static String cleanPhone(String value){String clean=value==null?"":value.replaceAll("[^0-9]","");if(!clean.isEmpty()&&(clean.length()<8||clean.length()>15))throw new IllegalArgumentException("Use the full WhatsApp number with country code, or leave it blank.");return clean;}

    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(!supportedPackage(sbn.getPackageName()))return;
        try{
            WhatsAppRepository repo=new WhatsAppRepository(this);if(!repo.configured())return;
            Notification n=sbn.getNotification();Bundle extras=n.extras;
            CharSequence conversation=extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
            CharSequence title=conversation!=null?conversation:extras.getCharSequence(Notification.EXTRA_TITLE);
            if(title==null||!matches(repo.chat(),title.toString()))return;
            activeChat=repo.chat();openChat=n.contentIntent;captureReply(n);
            android.os.Parcelable[] raw=extras.getParcelableArray(Notification.EXTRA_MESSAGES);Bundle[] bundled=null;
            if(raw!=null){bundled=new Bundle[raw.length];for(int i=0;i<raw.length;i++)bundled[i]=(Bundle)raw[i];}
            String latest="";
            if(bundled!=null){
                for(Notification.MessagingStyle.Message message:Notification.MessagingStyle.Message.getMessagesFromBundleArray(bundled)){
                    CharSequence body=message.getText();if(body!=null){latest=body.toString();repo.add(sbn.getKey(),latest,message.getTimestamp(),false);}
                }
            }else{
                CharSequence body=extras.getCharSequence(Notification.EXTRA_TEXT);if(body!=null){latest=body.toString();repo.add(sbn.getKey(),latest,sbn.getPostTime(),false);}
            }
            if(!latest.isBlank())ReplyNotifications.post(this,1,false,latest,"WhatsApp");
            sendBroadcast(new Intent(UPDATED).setPackage(getPackageName()));
        }catch(Exception ignored){ }
    }
    private static void captureReply(Notification notification){
        if(notification.actions==null)return;
        for(Notification.Action action:notification.actions){
            android.app.RemoteInput[] inputs=action.getRemoteInputs();
            if(inputs!=null&&inputs.length>0&&(action.getSemanticAction()==Notification.Action.SEMANTIC_ACTION_REPLY||replyAction==null)){
                replyAction=action;replyInput=inputs[0];return;
            }
        }
    }
    static boolean replyReady(String chat){return replyAction!=null&&replyInput!=null&&matches(activeChat,chat);}
    static void reply(Context context,String chat,String text)throws Exception{
        if(text==null||text.isBlank()||text.length()>4096)throw new IllegalArgumentException("WhatsApp messages must be between 1 and 4,096 characters.");
        if(!replyReady(chat))throw new IllegalStateException("Open the Instinct chat in WhatsApp once, then wait for its next reply before sending here.");
        Intent fill=new Intent();Bundle values=new Bundle();values.putCharSequence(replyInput.getResultKey(),text);android.app.RemoteInput.addResultsToIntent(new android.app.RemoteInput[]{replyInput},fill,values);
        replyAction.actionIntent.send(context,0,fill);new WhatsAppRepository(context).add(UUID.randomUUID().toString(),text,System.currentTimeMillis(),true);
        context.sendBroadcast(new Intent(UPDATED).setPackage(context.getPackageName()));
    }
    static boolean open(Context context,String phone){
        try{if(openChat!=null){openChat.send();return true;}}catch(Exception ignored){ }
        try{if(phone!=null&&!phone.isBlank()){context.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://wa.me/"+phone)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}}catch(Exception ignored){ }
        return false;
    }
}
