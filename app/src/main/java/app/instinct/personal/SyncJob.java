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
    static int backgroundMinutes(Context c){return Math.max(15,Math.min(1440,c.getSharedPreferences("sync",Context.MODE_PRIVATE).getInt("backgroundMinutes",15)));}
    static void schedule(Context c) {
        ReplyNotifications.channel(c);
        JobInfo job=new JobInfo.Builder(101,new ComponentName(c,SyncJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(backgroundMinutes(c)*60*1000L).setPersisted(true).build();
        c.getSystemService(JobScheduler.class).schedule(job);
    }
    @Override public boolean onStartJob(JobParameters p) {
        task=executor.submit(()->{
            boolean retry=false;
            try {
                MailRepository repo=MailRepository.get(this);
                if(repo.connected()) {
                    synchronized(repo){boolean initialized=repo.initialized();JSONArray old=repo.cached(),now=repo.sync();int count=initialized?ReplyNotifications.newReplies(old,now):0;
                        if(count>0) ReplyNotifications.post(this,count,false);}
                }
            } catch(Exception e) { retry=true; }
            jobFinished(p,retry);
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters p) { if(task!=null) task.cancel(true); return true; }
    @Override public void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
