package app.instinct.personal;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.concurrent.*;

public final class SyncJob extends JobService {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private Future<?> task;
    static void schedule(Context c) {
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("replies","Instinct replies",NotificationManager.IMPORTANCE_DEFAULT));
        JobInfo job=new JobInfo.Builder(101,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(15*60*1000L).setPersisted(true).build();
        c.getSystemService(JobScheduler.class).schedule(job);
    }
    @Override public boolean onStartJob(JobParameters p) {
        task=executor.submit(()->{
            boolean retry=false;
            try {
                MailRepository repo=MailRepository.get(this);
                if(repo.connected()) {
                    JSONArray old=repo.cached(); long latest=0;
                    for(int i=0;i<old.length();i++) if(!old.getJSONObject(i).optBoolean("outgoing")) latest=Math.max(latest,old.getJSONObject(i).optLong("time"));
                    JSONArray now=repo.sync(); int count=0;
                    for(int i=0;i<now.length();i++) if(!now.getJSONObject(i).optBoolean("outgoing")&&now.getJSONObject(i).optLong("time")>latest) count++;
                    if(count>0&&latest>0) {
                        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
                        Notification n=new Notification.Builder(this,"replies").setSmallIcon(app.instinct.personal.R.drawable.ic_instinct)
                            .setContentTitle("Instinct").setContentText(count==1?"You have a new reply.":count+" new replies.")
                            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
                        getSystemService(NotificationManager.class).notify(100,n);
                    }
                }
            } catch(Exception e) { retry=true; }
            jobFinished(p,retry);
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters p) { if(task!=null) task.cancel(true); return true; }
    @Override public void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
