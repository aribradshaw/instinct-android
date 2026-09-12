package app.instinct.personal;

import android.app.*;
import android.content.*;
import java.util.*;
import org.json.*;

final class ReplyNotifications {
    static final String CHANNEL="replies";
    static volatile boolean foreground=false;
    static void channel(Context c){c.getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Instinct replies",NotificationManager.IMPORTANCE_DEFAULT));}
    static boolean enabled(Context c){return c.getSharedPreferences("notifications",Context.MODE_PRIVATE).getBoolean("enabled",true);}
    static void enabled(Context c,boolean value){c.getSharedPreferences("notifications",Context.MODE_PRIVATE).edit().putBoolean("enabled",value).apply();if(!value)c.getSystemService(NotificationManager.class).cancel(100);}
    static boolean allowed(Context c){channel(c);NotificationManager nm=c.getSystemService(NotificationManager.class);return nm.areNotificationsEnabled()&&nm.getNotificationChannel(CHANNEL).getImportance()!=NotificationManager.IMPORTANCE_NONE;}
    static void post(Context c,int count,boolean test){
        if(!enabled(c)||!allowed(c)||(!test&&foreground))return;
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_instinct).setContentTitle("Instinct")
            .setContentText(test?"Test notification. Your Instinct alerts are ready.":count==1?"You have a new reply.":count+" new replies.")
            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setOnlyAlertOnce(false).build();
        c.getSystemService(NotificationManager.class).notify(test?102:100,n);
    }
    static int newReplies(JSONArray old,JSONArray now)throws JSONException{
        // Initial import suppression belongs to the persisted sync state, not history length.
        Set<String> known=new HashSet<>();for(int i=0;i<old.length();i++)known.add(old.getJSONObject(i).getString("id"));
        int count=0;for(int i=0;i<now.length();i++){JSONObject m=now.getJSONObject(i);if(!m.optBoolean("outgoing")&&!known.contains(m.getString("id")))count++;}return count;
    }
}
