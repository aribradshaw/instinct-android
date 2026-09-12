package app.instinct.personal;

import android.content.Context;
import android.text.Html;
import org.json.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.search.*;
import org.eclipse.angus.mail.imap.IMAPFolder;

final class MailRepository {
    static final String SUBJECT = "Instinct conversation";
    private static MailRepository instance;
    private final Vault vault;
    private MailRepository(Context c) { vault = new Vault(c); }
    static synchronized MailRepository get(Context c) { if (instance == null) instance = new MailRepository(c); return instance; }
    synchronized boolean connected() throws Exception { return !vault.read("account.enc", "").isEmpty()&&!account().isEmpty()&&!peer().isEmpty(); }
    synchronized String account() throws Exception { return new JSONObject(vault.read("config.enc","{}")).optString("account"); }
    synchronized String peer() throws Exception { return new JSONObject(vault.read("config.enc","{}")).optString("peer"); }
    synchronized String draft() throws Exception { return vault.read("draft.enc", ""); }
    synchronized void draft(String text) throws Exception { vault.write("draft.enc", text); }
    synchronized JSONArray draftFiles() throws Exception { return new JSONArray(vault.read("draft-files.enc","[]")); }
    synchronized JSONArray fileLabels() throws Exception {
        JSONArray labels=new JSONArray(),files=draftFiles();for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);labels.put(new JSONObject().put("name",f.getString("name")).put("size",f.getInt("size")));}return labels;
    }
    synchronized void addFile(String name,String type,byte[] bytes)throws Exception {
        JSONArray files=draftFiles();MailAttachment safe=new MailAttachment(name,type,bytes);
        files.put(new JSONObject().put("name",safe.name).put("type",safe.type).put("size",bytes.length).put("data",Base64.getEncoder().encodeToString(bytes)));
        MailAttachment.validate(decodeFiles(files));vault.write("draft-files.enc",files.toString());
    }
    private List<MailAttachment> decodeFiles(JSONArray files)throws Exception {
        List<MailAttachment> result=new ArrayList<>();for(int i=0;i<files.length();i++){JSONObject f=files.getJSONObject(i);result.add(new MailAttachment(f.getString("name"),f.getString("type"),Base64.getDecoder().decode(f.getString("data"))));}return result;
    }
    synchronized void removeFile(int index)throws Exception {JSONArray files=draftFiles();if(index>=0&&index<files.length()){files.remove(index);vault.write("draft-files.enc",files.toString());}}
    synchronized String replyId()throws Exception{return vault.read("reply.enc","");}
    synchronized void replyId(String id)throws Exception {
        if(!id.isEmpty()){boolean found=false;JSONArray history=cached();for(int i=0;i<history.length();i++){JSONObject m=history.getJSONObject(i);if(m.getString("id").equals(id)&&!m.optString("status").equals("failed")&&!m.optString("status").equals("unconfirmed"))found=true;}if(!found)throw new IllegalArgumentException("That message is not available to reply to.");}
        vault.write("reply.enc",id);
    }
    synchronized boolean initialized()throws Exception{return syncState().optBoolean("initialized");}
    synchronized JSONObject syncState()throws Exception{return new JSONObject(vault.read("sync.enc","{}"));}
    synchronized void recover(String id)throws Exception {
        JSONArray queue=new JSONArray(vault.read("pending.enc","[]"));
        for(int i=0;i<queue.length();i++){JSONObject m=queue.getJSONObject(i);if(m.getString("id").equals(id)&&m.optString("status").equals("failed")){
            String storedFiles=m.optJSONArray("files")==null?"[]":m.getJSONArray("files").toString();
            if((!draft().isBlank()&&!draft().equals(m.getString("text")))||(draftFiles().length()>0&&!draftFiles().toString().equals(storedFiles)))throw new IllegalStateException("Clear or send your current draft before recovering a different failed message.");
            vault.write("draft.enc",m.getString("text"));vault.write("draft-files.enc",m.optJSONArray("files")==null?"[]":m.getJSONArray("files").toString());vault.write("reply.enc",m.optString("replyId"));return;
        }}throw new IllegalArgumentException("Only definitely failed messages can be recovered. Check Gmail for unconfirmed sends.");
    }
    synchronized void disconnect() { vault.clear(); }
    private Properties properties() {
        Properties p = new Properties();
        for (String protocol : new String[]{"imaps", "smtps"}) {
            p.setProperty("mail." + protocol + ".ssl.checkserveridentity", "true");
            p.setProperty("mail." + protocol + ".connectiontimeout", "15000");
            p.setProperty("mail." + protocol + ".timeout", "20000");
            p.setProperty("mail." + protocol + ".writetimeout", "20000");
        }
        p.setProperty("mail.imaps.peek", "true");
        p.setProperty("mail.smtps.auth", "true");
        return p;
    }
    synchronized void connect(String account,String peer,String password) throws Exception {
        account=account.trim().toLowerCase(Locale.ROOT);peer=peer.trim().toLowerCase(Locale.ROOT);
        InternetAddress from=new InternetAddress(account,true), to=new InternetAddress(peer,true);from.validate();to.validate();
        if(!account.endsWith("@gmail.com")||account.equals(peer)||!peer.endsWith("@mail.instinct.com")) throw new IllegalArgumentException("Enter your Gmail address and the exact @mail.instinct.com address Instinct gave you.");
        String clean = password.replaceAll("\\s", "");
        if (!clean.matches("[a-zA-Z]{16}")) throw new IllegalArgumentException("Use the 16-letter Google app password, not your Gmail password.");
        Session session = Session.getInstance(properties());
        try (Store store = session.getStore("imaps")) { store.connect("imap.gmail.com", 993, account, clean); }
        try (Transport transport = session.getTransport("smtps")) { transport.connect("smtp.gmail.com", 465, account, clean); }
        if(!account().equals(account)||!peer().equals(peer)) vault.clear();
        vault.write("config.enc",new JSONObject().put("account",account).put("peer",peer).toString());
        vault.write("account.enc", clean);
    }
    synchronized JSONArray cached() throws Exception {
        JSONArray result = new JSONArray(vault.read("messages.enc", "[]"));
        JSONArray pending = new JSONArray(vault.read("pending.enc", "[]"));
        for (int i = 0; i < pending.length(); i++) {JSONObject item=pending.getJSONObject(i);item.remove("files");result.put(item);}
        return sorted(result);
    }
    static JSONArray sorted(JSONArray data) throws JSONException {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) list.add(data.getJSONObject(i));
        list.sort(Comparator.comparingLong(a -> a.optLong("time")));
        JSONArray result = new JSONArray(); for (JSONObject value : list) result.put(value); return result;
    }
    private boolean contains(Address[] addresses, String wanted) {
        if (addresses == null) return false;
        for (Address a : addresses) if (a instanceof InternetAddress && MailText.exactAddress(((InternetAddress) a).getAddress(), wanted)) return true;
        return false;
    }
    private String content(Part part, int depth) throws Exception {
        if (depth > 12 || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return "";
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            if (part.getSize() > 1024 * 1024) return "[Large email. Open in Gmail to read.]";
            Object c = part.getContent(); String text = c instanceof String ? (String)c : "";
            return part.isMimeType("text/html") ? Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString() : text;
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multi = (Multipart)part.getContent();
            if (part.isMimeType("multipart/alternative")) {
                for (int i=0;i<multi.getCount();i++) if (multi.getBodyPart(i).isMimeType("text/plain")) return content(multi.getBodyPart(i),depth+1);
            }
            StringBuilder text = new StringBuilder();
            for (int i=0;i<multi.getCount();i++) {
                String child = content(multi.getBodyPart(i),depth+1);
                if (!child.isBlank()) { text.append(child).append("\n"); if (part.isMimeType("multipart/alternative")) break; }
            }
            return text.toString();
        }
        return "";
    }
    private void attachments(Part part, JSONArray target, int depth) throws Exception {
        if (depth > 12) return;
        if (part.getFileName() != null) { target.put(MimeUtility.decodeText(part.getFileName())); return; }
        if (part.isMimeType("multipart/*")) {
            Multipart multi=(Multipart)part.getContent();
            for(int i=0;i<multi.getCount();i++) attachments(multi.getBodyPart(i),target,depth+1);
        }
    }
    synchronized JSONArray sync() throws Exception {return sync(false);}
    synchronized JSONArray sync(boolean older) throws Exception {
        String ACCOUNT=account(),PEER=peer();
        String password = vault.read("account.enc", "");
        if (password.isEmpty()) return cached();
        LinkedHashMap<String,JSONObject> map = new LinkedHashMap<>();
        JSONArray old = new JSONArray(vault.read("messages.enc", "[]"));
        for(int i=0;i<old.length();i++) { JSONObject item=old.getJSONObject(i); map.put(item.getString("id"),item); }
        try (Store store = Session.getInstance(properties()).getStore("imaps")) {
            store.connect("imap.gmail.com", 993, ACCOUNT, password);
            Folder all = null;
            for (Folder f : store.getDefaultFolder().list("*")) {
                if(f instanceof IMAPFolder) for(String attribute : ((IMAPFolder)f).getAttributes())
                    if(attribute.equalsIgnoreCase("\\All")) all=f;
            }
            if(all==null) throw new MessagingException("Gmail All Mail is unavailable over IMAP. Enable Show in IMAP for All Mail in Gmail settings.");
            all.open(Folder.READ_ONLY);
            SearchTerm peers = new OrTerm(new FromTerm(new InternetAddress(PEER)),new RecipientTerm(Message.RecipientType.TO,new InternetAddress(PEER)));
            Message[] messages=all.search(peers);
            FetchProfile uids=new FetchProfile();uids.add(UIDFolder.FetchProfileItem.UID);all.fetch(messages,uids);
            IMAPFolder imap=(IMAPFolder)all;long validity=imap.getUIDValidity();JSONObject cursor=syncState();
            boolean same=cursor.optLong("validity")==validity;
            long newest=same?cursor.optLong("newest"):0,oldest=same?cursor.optLong("oldest",Long.MAX_VALUE):Long.MAX_VALUE;
            List<Message> candidates=new ArrayList<>();
            for(Message m:messages){long uid=imap.getUID(m);if(older?uid<oldest:uid>newest)candidates.add(m);}
            // Initial/older pages start at the end. Subsequent syncs consume every new UID in order.
            int start=older||newest==0?Math.max(0,candidates.size()-200):0;
            int end=Math.min(candidates.size(),start+200);
            Message[] recent=candidates.subList(start,end).toArray(new Message[0]);
            FetchProfile profile=new FetchProfile(); profile.add(FetchProfile.Item.ENVELOPE); profile.add("Message-ID"); profile.add("References");
            all.fetch(recent,profile);
            for(Message msg:recent) {
                long uid=imap.getUID(msg);newest=Math.max(newest,uid);oldest=Math.min(oldest,uid);
                boolean outgoing=contains(msg.getFrom(),ACCOUNT)&&contains(msg.getRecipients(Message.RecipientType.TO),PEER);
                boolean incoming=contains(msg.getFrom(),PEER)&&contains(msg.getAllRecipients(),ACCOUNT);
                if(!incoming&&!outgoing) continue;
                String[] ids=msg.getHeader("Message-ID");
                String id=ids!=null&&ids.length>0?ids[0]:"imap-"+((IMAPFolder)all).getUIDValidity()+"-"+((IMAPFolder)all).getUID(msg);
                if(map.containsKey(id)) continue;
                String raw=content(msg,0).trim(); JSONArray files=new JSONArray(); attachments(msg,files,0);
                Date date=msg.getSentDate()!=null?msg.getSentDate():msg.getReceivedDate();
                map.put(id,new JSONObject().put("id",id).put("outgoing",outgoing).put("text",MailText.conversational(raw)).put("raw",raw)
                    .put("time",date!=null?date.getTime():System.currentTimeMillis()).put("subject",msg.getSubject()).put("attachments",files).put("status",outgoing?"sent":"received"));
            }
            cursor.put("initialized",true).put("validity",validity).put("newest",newest).put("oldest",oldest)
                .put("hasOlder",messages.length>0&&imap.getUID(messages[0])<oldest).put("lastChecked",System.currentTimeMillis());
            // Persist message data before advancing the cursor so an interrupted sync can safely repeat.
            JSONArray saved=new JSONArray();for(JSONObject value:map.values())saved.put(value);vault.write("messages.enc",sorted(saved).toString());
            vault.write("sync.enc",cursor.toString());
            all.close(false);
        }
        JSONArray pending=new JSONArray(vault.read("pending.enc","[]")), keep=new JSONArray();
        for(int i=0;i<pending.length();i++) if(!map.containsKey(pending.getJSONObject(i).getString("id"))) keep.put(pending.getJSONObject(i));
        JSONArray result=new JSONArray(); for(JSONObject item:map.values()) result.put(item);
        vault.write("messages.enc",sorted(result).toString()); vault.write("pending.enc",keep.toString());
        return cached();
    }
    synchronized void send(String text) throws Exception {
        String ACCOUNT=account(),PEER=peer();
        JSONArray files=draftFiles();List<MailAttachment> attachments=decodeFiles(files);
        if((text.isBlank()&&files.length()==0)||text.length()>30000) throw new IllegalArgumentException("Add a message or attachment. Text can be up to 30,000 characters.");
        String password=vault.read("account.enc",""); if(password.isEmpty()) throw new IllegalStateException("Connect Gmail first.");
        Session session=Session.getInstance(properties());
        JSONArray history=cached(); JSONObject reply=null;
        String selected=replyId();
        for(int i=history.length()-1;i>=0;i--) { JSONObject item=history.getJSONObject(i); if(selected.isEmpty()?item.optString("status").equals("received"):item.getString("id").equals(selected)) { reply=item; break; } }
        String subject=reply!=null?reply.optString("subject",SUBJECT):SUBJECT;
        MimeMessage msg=OutboundMail.create(session,text,ACCOUNT,PEER,subject,reply!=null?reply.getString("id"):null,attachments);
        JSONArray names=new JSONArray();for(MailAttachment file:attachments)names.put(file.name);
        JSONObject pending=new JSONObject().put("id",msg.getMessageID()).put("outgoing",true).put("text",text).put("raw",text)
            .put("time",System.currentTimeMillis()).put("subject",msg.getSubject()).put("status","unconfirmed").put("attachments",names).put("files",files).put("replyId",selected);
        JSONArray queue=new JSONArray(vault.read("pending.enc","[]")); queue.put(pending); vault.write("pending.enc",queue.toString());
        boolean submitted=false;
        try (Transport transport=session.getTransport("smtps")) {
            transport.connect("smtp.gmail.com",465,ACCOUNT,password);
            submitted=true; transport.sendMessage(msg,msg.getAllRecipients());
            pending.put("status","sent");pending.remove("files");vault.write("pending.enc",queue.toString());vault.write("draft.enc","");vault.write("draft-files.enc","[]");vault.write("reply.enc","");
        } catch(Exception e) {
            if(pending.optString("status").equals("sent")) return;
            // A disconnect after SMTP DATA can still mean delivery. Never automatically resend.
            if(!submitted) { pending.put("status","failed"); vault.write("pending.enc",queue.toString()); }
            throw e;
        }
    }
    static String friendly(Exception e) {
        if(e instanceof AuthenticationFailedException) return "Google rejected the connection. Check the app password for the Gmail address you entered.";
        if(e instanceof IllegalArgumentException || e instanceof IllegalStateException) return e.getMessage();
        if(e instanceof java.net.UnknownHostException) return "No connection. Your messages and draft are saved on this phone.";
        return "Connection did not finish. Check your internet and Gmail connection, then refresh.";
    }
}
