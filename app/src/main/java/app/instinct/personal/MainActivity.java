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
    private WhatsAppRepository whatsApp;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService storage=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AtomicBoolean syncing=new AtomicBoolean(false),sending=new AtomicBoolean(false);
    private boolean active=false,loaded=false;
    private boolean picking=false;
    private static final String ORIGIN="https://app.instinct.local/";
    private final BroadcastReceiver whatsAppUpdates=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){worker.submit(()->{try{publish();whatsAppState();}catch(Exception ignored){}});}};
    private final Runnable ticker=new Runnable(){ public void run(){ if(active){ refresh(false); handler.postDelayed(this,foregroundSeconds()*1000L); } } };

    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override public void onCreate(Bundle b) {
        theme=getSharedPreferences("appearance",MODE_PRIVATE).getString("theme",null);
        if(theme==null) theme=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES?"dark":"light";
        setTheme("light".equals(theme)?R.style.AppTheme_Light:R.style.AppTheme);
        super.onCreate(b); repo=MailRepository.get(this);whatsApp=new WhatsAppRepository(this);
        IntentFilter whatsappFilter=new IntentFilter(WhatsAppNotificationService.UPDATED);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(whatsAppUpdates,whatsappFilter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(whatsAppUpdates,whatsappFilter);
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
    private SharedPreferences appearancePreferences(){return getSharedPreferences("appearance",MODE_PRIVATE);}
    private SharedPreferences syncPreferences(){return getSharedPreferences("sync",MODE_PRIVATE);}
    private int foregroundSeconds(){return Math.max(5,Math.min(300,syncPreferences().getInt("foregroundSeconds",15)));}
    private void cadenceState(){try{event("cadence",new JSONObject().put("foregroundSeconds",foregroundSeconds()).put("backgroundMinutes",SyncJob.backgroundMinutes(this)));}catch(JSONException ignored){}}
    private String accent(){return appearancePreferences().getString("accent","default");}
    private String accentColor(){String saved=accent();if("system".equals(saved)&&Build.VERSION.SDK_INT>=31)return String.format("#%06X",0xFFFFFF&getColor(android.R.color.system_accent1_500));return "#D6F576";}
    private void appearanceState(){try{event("appearance",new JSONObject().put("accent",accent()).put("color",accentColor()));}catch(JSONException ignored){}}
    private WebResourceResponse blocked(){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
    private void event(String type,JSONObject data){ runOnUiThread(()->{ if(!isFinishing()&&loaded) web.evaluateJavascript("window.receive("+JSONObject.quote(type)+","+data+")",null); }); }
    private JSONObject object(String key,Object value){JSONObject o=new JSONObject();try{o.put(key,value);}catch(Exception ignored){}return o;}
    private void toast(String message){event("notice",object("message",message));}
    private void state(String label){event("status",object("label",label));}
    private JSONArray messages()throws Exception{JSONArray all=repo.cached(),wa=whatsApp.cached();for(int i=0;i<wa.length();i++)all.put(wa.getJSONObject(i));return MailRepository.sorted(all);}
    private void publish() throws Exception { event("messages",object("messages",messages()));event("health",repo.syncState()); }
    private void composeState()throws Exception {event("compose",new JSONObject().put("files",repo.fileLabels()).put("replyId",repo.replyId()));}
    private void initial(){worker.submit(()->{try {
        JSONObject init=new JSONObject().put("connected",repo.connected()||whatsApp.configured()).put("account",repo.account()).put("peer",repo.peer()).put("draft",repo.draft()).put("messages",messages());
        if(Intent.ACTION_SEND.equals(getIntent().getAction())) {String shared=getIntent().getStringExtra(Intent.EXTRA_TEXT); if(shared!=null) init.put("draft",shared);}
        event("init",init);composeState();event("health",repo.syncState());notificationState();cadenceState();appearanceState();whatsAppState();if(repo.connected()){SyncJob.schedule(this);refresh(false);}
    }catch(Exception e){toast("Could not read encrypted storage. Reconnect Gmail in settings.");}});}
    private void refresh(boolean manual){
        if(!loaded||!syncing.compareAndSet(false,true))return;
        event("sync",object("busy",true));
        worker.submit(()->{try{if(repo.connected()){if(manual)state("Refreshing…");repo.sync();publish();event("status",object("label",""));}}
        catch(Exception e){state(e instanceof jakarta.mail.AuthenticationFailedException?"Reconnect Gmail": "Connection interrupted · cached messages");if(manual)toast(MailRepository.friendly(e));}finally{syncing.set(false);event("sync",object("busy",false));}});
    }
    private void pickAttachment(){
        if(sending.get()||picking)return;
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        try{picking=true;startActivityForResult(intent,20);}catch(ActivityNotFoundException e){picking=false;toast("No file picker is available.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==21&&result==RESULT_OK&&data!=null){java.util.ArrayList<String> words=data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty())event("dictation",object("text",words.get(0)));return;}
        if(request!=20)return;picking=false;if(result!=RESULT_OK||data==null)return;
        java.util.List<Uri> uris=new java.util.ArrayList<>();
        if(data.getClipData()!=null){for(int i=0;i<Math.min(data.getClipData().getItemCount(),MailAttachment.MAX_FILES+1);i++)uris.add(data.getClipData().getItemAt(i).getUri());}
        else if(data.getData()!=null)uris.add(data.getData());
        event("fileBusy",object("busy",true));
        worker.submit(()->{try{
            for(Uri uri:uris){
                if(!"content".equals(uri.getScheme()))throw new IllegalArgumentException("Choose a file from the Android file picker.");
                String name="attachment";
                try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
                try(InputStream input=getContentResolver().openInputStream(uri);ByteArrayOutputStream output=new ByteArrayOutputStream()){
                    if(input==null)throw new IOException("File unavailable");byte[] buffer=new byte[8192];int count;
                    while((count=input.read(buffer))!=-1){if(output.size()+count>MailAttachment.MAX_BYTES)throw new IllegalArgumentException("Attachments must total 12 MB or less.");output.write(buffer,0,count);}
                    repo.addFile(name==null?"attachment":name,getContentResolver().getType(uri),output.toByteArray());
                }
            }
        }catch(Exception e){toast(e instanceof IllegalArgumentException?e.getMessage():"Could not attach that file. Download it to your phone and try again.");}
        finally{try{composeState();}catch(Exception ignored){}event("fileBusy",object("busy",false));}});
    }
    private void notificationState(){try{event("notifications",new JSONObject().put("enabled",ReplyNotifications.enabled(this)).put("allowed",ReplyNotifications.allowed(this)));}catch(JSONException ignored){}}
    private boolean notificationAccess(){String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return enabled!=null&&enabled.contains(getPackageName()+"/");}
    private void whatsAppState(){try{event("whatsapp",new JSONObject().put("configured",whatsApp.configured()).put("chat",whatsApp.chat()).put("phone",whatsApp.phone()).put("access",notificationAccess()).put("replyReady",WhatsAppNotificationService.replyReady(whatsApp.chat())));}catch(Exception ignored){}}
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
                String secret=password.getText().toString(),from=account.getText().toString(),to=peer.getText().toString();
                if(!from.trim().toLowerCase(java.util.Locale.ROOT).endsWith("@gmail.com")){account.setError("Use your personal Gmail address.");return;}
                if(!to.trim().toLowerCase(java.util.Locale.ROOT).endsWith("@mail.instinct.com")){peer.setError("Use the exact @mail.instinct.com address Instinct gave you.");return;}
                if(!secret.replaceAll("\\s","").matches("[a-zA-Z]{16}")){password.setError("Use a 16-letter Google app password.");return;}
                password.setText("");dialog.dismiss();state("Connecting Gmail…");
                worker.submit(()->{try{repo.connect(from,to,secret);event("connected",new JSONObject().put("connected",true).put("account",repo.account()).put("peer",repo.peer()));SyncJob.schedule(this);runOnUiThread(()->{if(ReplyNotifications.enabled(MainActivity.this)&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);});refresh(true);}
                    catch(Exception e){state("Gmail not connected");toast(MailRepository.friendly(e));}});
            });
        });dialog.show();
    }
    private void whatsAppDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(24*getResources().getDisplayMetrics().density);box.setPadding(pad,pad/2,pad,pad/2);
        TextView info=new TextView(this);info.setText("Instinct will mirror notifications only from this exact WhatsApp chat. Replies use WhatsApp's own notification reply action. No unofficial login, message scraping, or WhatsApp password is used.");info.setTextSize(15);box.addView(info);
        EditText chat=new EditText(this);chat.setSingleLine();chat.setHint("Exact chat name, for example Instinct");box.addView(chat);
        EditText phone=new EditText(this);phone.setSingleLine();phone.setHint("Instinct number with country code (optional)");phone.setInputType(InputType.TYPE_CLASS_PHONE);box.addView(phone);
        try{chat.setText(whatsApp.chat());phone.setText(whatsApp.phone());}catch(Exception ignored){}
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Connect WhatsApp").setView(box).setNegativeButton("Cancel",null).setNeutralButton("Notification access",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception e){toast("Open Android Settings and allow notification access for Instinct.");}});
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{whatsApp.configure(chat.getText().toString(),phone.getText().toString());dialog.dismiss();whatsAppState();initial();if(!notificationAccess())toast("Now allow Instinct notification access, then have Instinct send one WhatsApp reply.");else toast("WhatsApp is connected. The next Instinct reply will appear here.");}catch(Exception e){chat.setError(e.getMessage());}});
        });dialog.show();
    }
    private void openExternal(String url){
        Uri uri=Uri.parse(url);if(!"https".equals(uri.getScheme())&&!"http".equals(uri.getScheme()))return;
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){toast("No browser is available to open this link.");}
    }
    @Override protected void onResume(){super.onResume();active=true;ReplyNotifications.foreground=true;getSystemService(NotificationManager.class).cancel(100);notificationState();cadenceState();appearanceState();whatsAppState();handler.postDelayed(ticker,1000);}
    @Override protected void onPause(){active=false;ReplyNotifications.foreground=false;handler.removeCallbacks(ticker);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(ticker);try{unregisterReceiver(whatsAppUpdates);}catch(Exception ignored){}worker.shutdown();storage.shutdown();web.removeJavascriptInterface("Native");web.destroy();super.onDestroy();}
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
        @JavascriptInterface public void setCadence(String mode,int value){
            if((!"foreground".equals(mode)&&!"background".equals(mode))||value<5)return;
            runOnUiThread(()->{if("foreground".equals(mode)){if(value>300)return;syncPreferences().edit().putInt("foregroundSeconds",value).apply();handler.removeCallbacks(ticker);if(active)handler.postDelayed(ticker,value*1000L);}else{if(value<15||value>1440)return;syncPreferences().edit().putInt("backgroundMinutes",value).apply();SyncJob.schedule(MainActivity.this);}cadenceState();});
        }
        @JavascriptInterface public void setAccent(String value){
            if(!"default".equals(value)&&!"system".equals(value)&&!value.matches("#[0-9A-Fa-f]{6}"))return;
            runOnUiThread(()->{appearancePreferences().edit().putString("accent",value).apply();appearanceState();});
        }
        @JavascriptInterface public void connect(){runOnUiThread(()->connectDialog());}
        @JavascriptInterface public void connectWhatsApp(){runOnUiThread(()->whatsAppDialog());}
        @JavascriptInterface public void openWhatsApp(){runOnUiThread(()->{try{if(!WhatsAppNotificationService.open(MainActivity.this,whatsApp.phone()))toast("Add Instinct's WhatsApp number in connection settings first.");}catch(Exception e){toast("WhatsApp is not connected yet.");}});}
        @JavascriptInterface public void refresh(){runOnUiThread(()->MainActivity.this.refresh(true));}
        @JavascriptInterface public void attach(){runOnUiThread(()->pickAttachment());}
        @JavascriptInterface public void removeAttachment(int index){if(sending.get())return;worker.submit(()->{try{repo.removeFile(index);composeState();}catch(Exception e){toast("Could not remove attachment.");}});}
        @JavascriptInterface public void reply(String id){if(sending.get())return;worker.submit(()->{try{repo.replyId(id);composeState();}catch(Exception e){toast(MailRepository.friendly(e));}});}
        @JavascriptInterface public void recover(String id){if(sending.get())return;worker.submit(()->{try{repo.recover(id);event("draft",object("text",repo.draft()));composeState();toast("Recovered for editing. Tap Send when ready.");}catch(Exception e){toast(MailRepository.friendly(e));}});}
        @JavascriptInterface public void copy(String text){runOnUiThread(()->{getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Instinct message",text));toast("Copied message.");});}
        @JavascriptInterface public void dictate(){runOnUiThread(()->{try{startActivityForResult(new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"Dictate a draft for Instinct"),21);}catch(ActivityNotFoundException e){toast("Use the microphone on your keyboard to dictate a message.");}});}
        @JavascriptInterface public void loadOlder(){if(!syncing.compareAndSet(false,true))return;event("sync",object("busy",true));worker.submit(()->{try{repo.sync(true);publish();}catch(Exception e){toast(MailRepository.friendly(e));}finally{syncing.set(false);event("sync",object("busy",false));}});}
        @JavascriptInterface public void saveDraft(String value){if(value.length()<=30000)worker.submit(()->{try{repo.draft(value);}catch(Exception e){toast("Draft could not be saved.");}});}
        @JavascriptInterface public void send(String text,String channel){
            if(!sending.compareAndSet(false,true))return;
            state("whatsapp".equals(channel)?"Sending through WhatsApp…":"Sending via Gmail…");
            worker.submit(()->{try{if("whatsapp".equals(channel)){WhatsAppNotificationService.reply(MainActivity.this,whatsApp.chat(),text);repo.draft("");publish();event("sent",new JSONObject());state("Handed to WhatsApp");}
                    else{repo.send(text);publish();event("sent",new JSONObject());composeState();state("Sent via Gmail · awaiting reply");}}
                catch(Exception e){try{publish();}catch(Exception ignored){}String message="whatsapp".equals(channel)?e.getMessage():"Send was not confirmed. Check the message status and Gmail before sending again. Nothing is retried automatically.";event("sendError",object("message",message));state("Check send status");}
                finally{sending.set(false);}});
        }
        @JavascriptInterface public void openGmail(){worker.submit(()->{try{String url="https://mail.google.com/mail/u/?authuser="+Uri.encode(repo.account())+"#search/"+Uri.encode(repo.peer());runOnUiThread(()->openExternal(url));}catch(Exception e){toast("Connect Gmail first.");}});}
        @JavascriptInterface public void openUrl(String url){runOnUiThread(()->openExternal(url));}
        @JavascriptInterface public void disconnect(){runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Disconnect Gmail?").setMessage("Remove the saved app password, conversation cache, and draft from this phone. Your emails stay in Gmail.").setNegativeButton("Cancel",null).setPositiveButton("Disconnect",(d,w)->worker.submit(()->{repo.disconnect();getSystemService(JobScheduler.class).cancel(101);initial();state("Gmail not connected");})).show());}
    }
}
