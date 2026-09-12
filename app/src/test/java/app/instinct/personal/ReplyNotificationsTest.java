package app.instinct.personal;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplyNotificationsTest {
    private JSONObject message(String id,boolean outgoing,long time)throws Exception{return new JSONObject().put("id",id).put("outgoing",outgoing).put("time",time);}
    @Test public void firstReplyAfterEmptySuccessfulSyncNotifies()throws Exception{assertEquals(1,ReplyNotifications.newReplies(new JSONArray(),new JSONArray().put(message("first",false,1))));}
    @Test public void firstReplyAfterOutgoingMessageNotifies()throws Exception{JSONArray old=new JSONArray().put(message("sent",true,100));assertEquals(1,ReplyNotifications.newReplies(old,new JSONArray(old.toString()).put(message("reply",false,101))));}
    @Test public void senderClockDoesNotHideNewReply()throws Exception{JSONArray old=new JSONArray().put(message("old",false,200));assertEquals(1,ReplyNotifications.newReplies(old,new JSONArray(old.toString()).put(message("new",false,100))));}
    @Test public void repeatedSyncAndOutgoingMailStayQuiet()throws Exception{JSONArray old=new JSONArray().put(message("old",false,100));assertEquals(0,ReplyNotifications.newReplies(old,new JSONArray(old.toString()).put(message("sent",true,200))));}
    @Test public void latestNewReplyAppearsInNotification()throws Exception{JSONArray old=new JSONArray().put(message("old",false,100).put("text","Old"));JSONArray now=new JSONArray(old.toString()).put(message("newer",false,300).put("text","  Latest\nreply  ")).put(message("new",false,200).put("text","Earlier"));assertEquals("Latest reply",ReplyNotifications.latestNewReply(old,now));}
    @Test public void previewsAreBoundedForNotificationShade(){assertEquals("line with spaces",ReplyNotifications.cleanPreview(" line\n with   spaces "));assertEquals(240,ReplyNotifications.cleanPreview("x".repeat(300)).length());}
}
