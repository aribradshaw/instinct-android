package app.instinct.personal;

import java.util.*;

final class MailAttachment {
    static final int MAX_BYTES=12*1024*1024, MAX_FILES=8;
    final String name,type;
    final byte[] bytes;
    MailAttachment(String name,String type,byte[] bytes) {
        this.name=name.replaceAll("[\\r\\n\\p{Cntrl}]", "_");
        this.type=type!=null&&type.matches("[a-zA-Z0-9!#$&^_.+-]+/[a-zA-Z0-9!#$&^_.+-]+")?type:"application/octet-stream";
        this.bytes=bytes;
    }
    static void validate(List<MailAttachment> files) {
        long total=0;
        if(files.size()>MAX_FILES)throw new IllegalArgumentException("Attach up to 8 files per message.");
        for(MailAttachment file:files)total+=file.bytes.length;
        if(total>MAX_BYTES)throw new IllegalArgumentException("Attachments must total 12 MB or less. Share a link for larger files.");
    }
}
