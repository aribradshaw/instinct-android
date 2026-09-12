package app.instinct.personal;

import android.Manifest;
import android.app.*;
import android.app.job.JobScheduler;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.*;

public final class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private String theme;
    private MailRepository repo;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService storage=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AtomicBoolean syncing=new AtomicBoolean(false),sending=new AtomicBoolean(false);
    private boolean active=false,loaded=false;
    private static final String ORIGIN="https://app.instinct.local/";
    private final Runnable ticker=new Runnable(){ public void run(){ if(active){ refresh(false); handler.postDelayed(this,15000); } } };

    @Override public void onCreate(Bundle b) {
        theme=getSharedPreferences("appearance",MODE_PRIVATE).getString("theme",null);
        if(theme==null) theme=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES?"dark":"light";
        setTheme("light".equals(theme)?R.style.AppTheme_Light:R.style.AppTheme);
        super.onCreate(b); repo=MailRepository.get(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        web=new WebView(this); web.setBackgroundColor(Color.rgb(16,19,16));
        root=new FrameLayout(this);applyAppearance();
        root.setOnApplyWindowInsetsListener((v,insets)->{ android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime()); v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return WindowInsets.CONSUMED; });
        web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(false);
        web.getSettings().setAllowFileAccess(false); web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.getSettings().setSupportZoom(false); web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.addJavascriptInterface(new Bridge(),"Native");
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView w,WebResourceRequest r){
                String url=r.getUrl().toString();
                if(url.startsWith(ORIGIN)) {
                    String path=url.substring(ORIGIN.length()); if(path.isEmpty())path="index.html";
                    if(!path.matches("[a-zA-Z0-9._-]+")) return blocked();
                    try {
                        String mime=path.endsWith(".js")?"text/javascript":path.endsWith(".css")?"text/css":"text/html";
                        return new WebResourceResponse(mime,"UTF-8",getAssets().open(path));
                    } catch(IOException e){return blocked();}
                }
                return blocked();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest request) { if(request.isForMainFrame()&&request.hasGesture()) openExternal(request.getUrl().toString()); return true; }
            @Override public void onPageFinished(WebView w,String url) { if(url.equals(ORIGIN)){loaded=true;event("theme",object("theme",theme));initial();} }
        });
        root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root); web.loadUrl(ORIGIN);
    }
    private void applyAppearance(){
        boolean light="light".equals(theme);
        setTheme(light?R.style.AppTheme_Light:R.style.AppTheme);
        int background=Color.parseColor(light?"#F7F8F2":"#101310");
        web.setBackgroundColor(background);root.setBackgroundColor(background);
        getWindow().setStatusBarColor(background);getWindow().setNavigationBarColor(background);
        WindowInsetsController controller=getWindow().getInsetsController();
        if(controller!=null){int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;controller.setSystemBarsAppearance(light?mask:0,mask);}
    }
    private WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
    private void event(String type,JSONObject data){ runOnUiThread(()->{ if(!isFinishing()&&loaded) web.evaluateJavascript("window.receive("+JSONObject.quote(type)+","+data+")",null); }); }
    private JSONObject object(String key,Object value){JSONObject o=new JSONObject();try{o.put(key,value);}catch(Exception ignored){}return o;}
    private void toast(String message){event("notice",object("message",message));}
    private void state(String label){event("status",object("label",label));}
    private void publish() throws Exception { event("messages",object("messages",repo.cached())); }
    private void initial(){worker.submit(()->{try {
        JSONObject init=new JSONObject().put("connected",repo.connected()).put("account",repo.account()).put("peer",repo.peer()).put("draft",repo.draft()).put("messages",repo.cached());
        if(Intent.ACTION_SEND.equals(getIntent().getAction())) {String shared=getIntent().getStringExtra(Intent.EXTRA_TEXT); if(shared!=null) init.put("draft",shared);}
        event("init",init);notificationState();if(repo.connected()){SyncJob.schedule(this);refresh(false);}
    }catch(Exception e){toast("Could not read encrypted storage. Reconnect Gmail in settings.");}});}
    private void refresh(boolean manual){
        if(!loaded||!syncing.compareAndSet(false,true))return;
        event("sync",object("busy",true));
        worker.submit(()->{try{if(repo.connected()){if(manual)state("Refreshing…");repo.sync();publish();state("Up to date");if(manual)toast("Conversation is up to date.");}}
        catch(Exception e){state("Offline · cached messages");if(manual)toast(MailRepository.friendly(e));}finally{syncing.set(false);event("sync",object("busy",false));}});
    }
    private void notificationState(){try{event("notifications",new JSONObject().put("enabled",ReplyNotifications.enabled(this)).put("allowed",ReplyNotifications.allowed(this)));}catch(JSONException ignored){}}
    private void notificationSettings(){try{startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,ReplyNotifications.CHANNEL));}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);notificationState();}
    private void connectDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);box.setPadding(pad,pad/2,pad,pad/2);
        TextView info=new TextView(this);info.setText("Use the Gmail account Instinct recognizes and the email address Instinct gave you. Create a Google app password named Instinct, then paste its 16 letters below. It grants email access and is stored encrypted on this phone.");info.setTextSize(15);box.addView(info);
        EditText account=new EditText(this);account.setSingleLine();account.setHint("Your Gmail address");account.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);box.addView(account);
        EditText peer=new EditText(this);peer.setSingleLine();peer.setHint("Your Instinct email address");peer.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);box.addView(peer);
        try{account.setText(repo.account());peer.setText(repo.peer());}catch(Exception ignored){}
        EditText password=new EditText(this);password.setSingleLine();password.setHint("16-letter app password");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);box.addView(password);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Connect your Gmail").setView(box).setNegativeButton("Cancel",null).setNeutralButton("Create password",null).setPositiveButton("Connect",null).create();
        dialog.setOnShowListener(d->{
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->openExternal("https://myaccount.google.com/apppasswords?authuser="+Uri.encode(account.getText().toString().trim())));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                String secret=password.getText().toString(),from=account.getText().toString(),to=peer.getText().toString();password.setText("");dialog.dismiss();state("Connecting Gmail…");
                worker.submit(()->{try{repo.connect(from,to,secret);event("connected",new JSONObject().put("connected",true).put("account",repo.account()).put("peer",repo.peer()));SyncJob.schedule(this);runOnUiThread(()->{if(ReplyNotifications.enabled(MainActivity.this)&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);});refresh(true);}
                    catch(Exception e){state("Gmail not connected");toast(MailRepository.friendly(e));}});
            });
        });dialog.show();
    }
    private void openExternal(String url){
        Uri uri=Uri.parse(url);if(!"https".equals(uri.getScheme())&&!"http".equals(uri.getScheme()))return;
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){toast("No browser is available to open this link.");}
    }
    @Override protected void onResume(){super.onResume();active=true;ReplyNotifications.foreground=true;getSystemService(NotificationManager.class).cancel(100);notificationState();handler.postDelayed(ticker,1000);}
    @Override protected void onPause(){active=false;ReplyNotifications.foreground=false;handler.removeCallbacks(ticker);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(ticker);worker.shutdown();storage.shutdown();web.removeJavascriptInterface("Native");web.destroy();super.onDestroy();}
    private final class Bridge {
        @JavascriptInterface public void haptic(){runOnUiThread(()->web.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK));}
        @JavascriptInterface public void notificationState(){runOnUiThread(()->MainActivity.this.notificationState());}
        @JavascriptInterface public void setNotifications(boolean enabled){runOnUiThread(()->{ReplyNotifications.enabled(MainActivity.this,enabled);MainActivity.this.notificationState();if(enabled&&!ReplyNotifications.allowed(MainActivity.this)){if(ReplyNotifications.enabled(MainActivity.this)&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);else notificationSettings();}});}
        @JavascriptInterface public void openNotificationSettings(){runOnUiThread(()->notificationSettings());}
        @JavascriptInterface public void testNotification(){runOnUiThread(()->{ReplyNotifications.post(MainActivity.this,1,true);toast(ReplyNotifications.allowed(MainActivity.this)?"Test notification sent. Check your notification shade.":"Allow Instinct notifications in Android settings first.");});}
        @JavascriptInterface public void setTheme(String value){
            if(!"light".equals(value)&&!"dark".equals(value))return;
            runOnUiThread(()->{theme=value;getSharedPreferences("appearance",MODE_PRIVATE).edit().putString("theme",value).apply();applyAppearance();});
        }
        @JavascriptInterface public void connect(){runOnUiThread(()->connectDialog());}
        @JavascriptInterface public void refresh(){runOnUiThread(()->MainActivity.this.refresh(true));}
        @JavascriptInterface public void saveDraft(String value){if(value.length()<=30000)storage.submit(()->{try{repo.draft(value);}catch(Exception e){toast("Draft could not be saved.");}});}
        @JavascriptInterface public void send(String text){
            if(!sending.compareAndSet(false,true))return;
            state("Sending via Gmail…");
            worker.submit(()->{try{repo.send(text);publish();event("sent",new JSONObject());state("Sent via Gmail");}
                catch(Exception e){try{publish();}catch(Exception ignored){}event("sendError",object("message","Send was not confirmed. Check the message status and Gmail before sending again. Nothing is retried automatically."));state("Check send status");}
                finally{sending.set(false);}});
        }
        @JavascriptInterface public void openGmail(){worker.submit(()->{try{String url="https://mail.google.com/mail/u/?authuser="+Uri.encode(repo.account())+"#search/"+Uri.encode(repo.peer());runOnUiThread(()->openExternal(url));}catch(Exception e){toast("Connect Gmail first.");}});}
        @JavascriptInterface public void openUrl(String url){runOnUiThread(()->openExternal(url));}
        @JavascriptInterface public void disconnect(){runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Disconnect Gmail?").setMessage("Remove the saved app password, conversation cache, and draft from this phone. Your emails stay in Gmail.").setNegativeButton("Cancel",null).setPositiveButton("Disconnect",(d,w)->worker.submit(()->{repo.disconnect();getSystemService(JobScheduler.class).cancel(101);initial();state("Gmail not connected");})).show());}
    }
}
