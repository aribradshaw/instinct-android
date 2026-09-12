package app.instinct.personal;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class Vault {
    private final Context context;
    Vault(Context context) { this.context = context.getApplicationContext(); }
    private javax.crypto.SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        String alias = "instinct.private.v1";
        if (!ks.containsAlias(alias)) {
            KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        return (javax.crypto.SecretKey) ks.getKey(alias, null);
    }
    synchronized void write(String name, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), name));
        FileOutputStream stream = null;
        try { stream = file.startWrite(); stream.write(cipher.getIV()); stream.write(ciphertext); file.finishWrite(stream); }
        catch (Exception e) { if (stream != null) file.failWrite(stream); throw e; }
    }
    synchronized String read(String name, String fallback) throws Exception {
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), name));
        if (!file.getBaseFile().exists()) return fallback;
        byte[] value = file.readFully();
        if (value.length < 28) throw new IOException("Encrypted storage is incomplete");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, value, 0, 12));
        return new String(cipher.doFinal(value, 12, value.length - 12), StandardCharsets.UTF_8);
    }
    synchronized void clear() {
        for (String name : new String[]{"account.enc", "config.enc", "messages.enc", "draft.enc", "pending.enc", "draft-files.enc", "reply.enc", "sync.enc"})
            new AtomicFile(new File(context.getFilesDir(), name)).delete();
    }
    synchronized void delete(String name) { new AtomicFile(new File(context.getFilesDir(), name)).delete(); }
}
