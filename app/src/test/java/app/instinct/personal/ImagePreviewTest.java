package app.instinct.personal;

import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

public class ImagePreviewTest {
    @Test public void readsAttachmentBytes() throws Exception {
        byte[] bytes={1,2,3,4};
        assertArrayEquals(bytes,ImagePreview.bounded(new ByteArrayInputStream(bytes)));
    }
    @Test public void acceptsLimit() throws Exception {
        assertEquals(ImagePreview.MAX_BYTES,ImagePreview.bounded(new ByteArrayInputStream(new byte[ImagePreview.MAX_BYTES])).length);
    }
    @Test(expected=IOException.class) public void rejectsOversizedUnknownLengthStream() throws Exception {
        ImagePreview.bounded(new ByteArrayInputStream(new byte[ImagePreview.MAX_BYTES+1]));
    }
}
