package tech.sadnes.client.util.crypto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AssetCrypto {
    private static final byte[] MAGIC = new byte[]{-98, 83, 65, 49};
    private static final byte VERSION = 1;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int HEADER_LENGTH = MAGIC.length + 1 + 12;
    private static final int PREFIX_LENGTH = MAGIC.length + 1;
    private static final String PASSPHRASE = "sadnes_asset_guard_2026";
    private static final SecretKeySpec KEY = new SecretKeySpec(deriveKey("sadnes_asset_guard_2026"), "AES");
    private static final SecureRandom RANDOM = new SecureRandom();

    private AssetCrypto() {
    }

    public static InputStream wrap(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }

        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, PREFIX_LENGTH);
        byte[] bytes = new byte[PREFIX_LENGTH];
        int i = readFully(pushbackInputStream, bytes);
        boolean flag = i == PREFIX_LENGTH && hasMagic(bytes) && bytes[MAGIC.length] == 1;
        if (i > 0) {
            pushbackInputStream.unread(bytes, 0, i);
        }

        return !flag ? pushbackInputStream : new ByteArrayInputStream(decrypt(pushbackInputStream.readAllBytes()));
    }

    public static byte[] decrypt(byte[] bytes) {
        if (bytes != null && bytes.length >= HEADER_LENGTH + 16 && hasMagic(bytes) && bytes[MAGIC.length] == 1) {
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(2, KEY, new GCMParameterSpec(128, bytes, MAGIC.length + 1, 12));
                return cipher.doFinal(bytes, HEADER_LENGTH, bytes.length - HEADER_LENGTH);
            } catch (Exception exception) {
                return bytes;
            }
        } else {
            return bytes;
        }
    }

    public static byte[] encrypt(byte[] bytes) {
        try {
            byte[] bytes2 = new byte[12];
            RANDOM.nextBytes(bytes2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(1, KEY, new GCMParameterSpec(128, bytes2));
            byte[] bytes3 = cipher.doFinal(bytes);
            byte[] bytes4 = new byte[HEADER_LENGTH + bytes3.length];
            System.arraycopy(MAGIC, 0, bytes4, 0, MAGIC.length);
            bytes4[MAGIC.length] = 1;
            System.arraycopy(bytes2, 0, bytes4, MAGIC.length + 1, 12);
            System.arraycopy(bytes3, 0, bytes4, HEADER_LENGTH, bytes3.length);
            return bytes4;
        } catch (Exception exception) {
            throw new IllegalStateException("asset encrypt failed", exception);
        }
    }

    private static boolean hasMagic(byte[] bytes) {
        if (bytes.length < MAGIC.length) {
            return false;
        }

        int i = 0;

        for (int i2 = MAGIC.length; i < i2; i++) {
            if (bytes[i] != MAGIC[i]) {
                return false;
            }
        }

        return true;
    }

    private static int readFully(InputStream inputStream, byte[] bytes) throws IOException {
        int i = 0;

        while (i < bytes.length) {
            int i2 = inputStream.read(bytes, i, bytes.length - i);
            if (i2 < 0) {
                return i;
            }

            i += i2;
        }

        return i;
    }

    private static byte[] deriveKey(String string) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(string.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
