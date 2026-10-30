package tech.sadnes.client.util.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class DataEncryption {
    private static final String PASSPHRASE = "yeekhiralover";
    private static final byte FORMAT_VERSION = 1;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final SecretKeySpec KEY = new SecretKeySpec(deriveKey("yeekhiralover"), "AES");

    private DataEncryption() {
    }

    public static String encryptString(String string) {
        return Base64.getEncoder().encodeToString(encrypt(string.getBytes(StandardCharsets.UTF_8)));
    }

    public static String decryptString(String string) {
        byte[] bytes = Base64.getDecoder().decode(string);
        if (isEncryptedFormat(bytes)) {
            return new String(decrypt(bytes), StandardCharsets.UTF_8);
        }

        byte[] bytes2 = "yeekhiralover".getBytes(StandardCharsets.UTF_8);
        byte[] bytes3 = new byte[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            bytes3[i] = (byte)(bytes[i] ^ bytes2[i % bytes2.length]);
        }

        return new String(bytes3, StandardCharsets.UTF_8);
    }

    public static byte[] encrypt(byte[] bytes) {
        try {
            byte[] bytes2 = new byte[12];
            RANDOM.nextBytes(bytes2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, KEY, new GCMParameterSpec(128, bytes2));
            byte[] bytes3 = cipher.doFinal(bytes);
            return ByteBuffer.allocate(13 + bytes3.length).put((byte)1).put(bytes2).put(bytes3).array();
        } catch (Exception exception) {
            throw new IllegalStateException("encrypt failed", exception);
        }
    }

    public static byte[] decrypt(byte[] bytes) {
        if (!isEncryptedFormat(bytes)) {
            throw new IllegalStateException("decrypt failed: unknown format");
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(2, KEY, new GCMParameterSpec(128, bytes, 1, 12));
            return cipher.doFinal(bytes, 13, bytes.length - 1 - 12);
        } catch (Exception exception) {
            throw new IllegalStateException("decrypt failed", exception);
        }
    }

    private static boolean isEncryptedFormat(byte[] bytes) {
        return bytes.length > 13 && bytes[0] == 1;
    }

    private static byte[] deriveKey(String string) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(string.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
