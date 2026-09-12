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
        ReplyNotifications.channel(c);
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
                    synchronized(repo){JSONArray old=repo.cached(),now=repo.sync();int count=ReplyNotifications.newReplies(old,now);
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
