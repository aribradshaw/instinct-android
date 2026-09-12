package app.instinct.personal;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Base64;

/** Only decoded attachment pixels reach the WebView, never remote email images or SVG. */
final class ImagePreview {
    static final int MAX_BYTES = 12 * 1024 * 1024;
    static byte[] bounded(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) != -1) {
            if (out.size() + count > MAX_BYTES) throw new IOException("Image too large");
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }
    static String create(byte[] bytes) {
        if (bytes.length > MAX_BYTES) return "";
        try {
            Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes)), (decoder, info, source) -> {
                int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                double scale = Math.min(1.0, 640.0 / Math.max(width, height));
                decoder.setTargetSize(Math.max(1, (int)(width * scale)), Math.max(1, (int)(height * scale)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                if (out.size() > 96 * 1024) { out.reset(); bitmap.compress(Bitmap.CompressFormat.JPEG, 45, out); }
                return out.size() > 96 * 1024 ? "" : "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
            } finally { bitmap.recycle(); }
        } catch (IOException | RuntimeException e) { return ""; }
    }
}
