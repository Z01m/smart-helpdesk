package com.smarthelpdesk.notificationservice.mail;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Queue payloads can contain short-lived confirmation tokens; encrypt them at rest.
public final class EmailCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    public EmailCipher(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        if (decoded.length != 32) throw new IllegalArgumentException("EMAIL_ENCRYPTION_KEY must encode 32 random bytes");
        key = new SecretKeySpec(decoded, "AES");
    }
    public String encrypt(String plain, String context) {
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = Arrays.copyOf(nonce, nonce.length + ciphertext.length);
            System.arraycopy(ciphertext, 0, envelope, nonce.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(envelope);
        } catch (Exception ex) { throw new IllegalStateException("Email encryption failed", ex); }
    }
    public String decrypt(String encoded, String context) {
        try {
            byte[] envelope = Base64.getDecoder().decode(encoded);
            if (envelope.length < 28) throw new IllegalArgumentException("Invalid encrypted payload");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOf(envelope, 12)));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(envelope, 12, envelope.length - 12), StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("Email payload integrity check failed", ex); }
    }
}
