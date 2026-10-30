package tech.sadnes.client.network.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.concurrent.atomic.AtomicLong;

public final class SecureEnvelope {
    private static final int MIN_KEY_LENGTH = 16;
    private static final byte[] ENC_LEFT_LABEL = "enc-left".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ENC_RIGHT_LABEL = "enc-right".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CTR_BASE_LABEL = "ctr-base".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MAC_LEFT_LABEL = "mac-left".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MAC_RIGHT_LABEL = "mac-right".getBytes(StandardCharsets.US_ASCII);
    private final SecureRandom secureRandom;
    private final DryGascon2Hash hash;
    private final Shacal2CtrCipher cipher;
    private final byte[] masterKey;
    private final Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Decoder decoder = Base64.getUrlDecoder();
    private final AtomicLong nonceCounter = new AtomicLong();

    public SecureEnvelope(SecureRandom secureRandom2, DryGascon2Hash dryGascon2Hash, Shacal2CtrCipher shacal2CtrCipher, byte[] bytes) {
        this.secureRandom = Objects.requireNonNull(secureRandom2, "secureRandom");
        this.hash = Objects.requireNonNull(dryGascon2Hash, "dryGascon2Hash");
        this.cipher = Objects.requireNonNull(shacal2CtrCipher, "shacal2CtrCipher");
        this.masterKey = Arrays.copyOf(Objects.requireNonNull(bytes, "masterKey"), bytes.length);
        if (this.masterKey.length < 16) {
            throw new SecureEnvelopeException("Master key must be at least 16 bytes");
        }
    }

    public static byte[] decodeBase64(String string) {
        return Base64.getDecoder().decode(string);
    }

    public String encrypt(String string) {
        byte[] bytes = this.generateNonce();
        byte[] bytes2 = string.getBytes(StandardCharsets.UTF_8);
        byte[] bytes3 = this.deriveEncryptionKey(bytes);
        byte[] bytes4 = this.deriveCounterBase(bytes, bytes3);
        byte[] bytes5 = this.cipher.process(bytes3, bytes4, bytes2);
        byte[] bytes6 = this.computeTag(bytes, bytes3, bytes4, bytes5);
        EnvelopePayload envelopePayload = new EnvelopePayload(this.encoder.encodeToString(bytes), this.encoder.encodeToString(bytes5), this.encoder.encodeToString(bytes6));
        return envelopePayload.toJson();
    }

    public String decrypt(String string) {
        try {
            EnvelopePayload envelopePayload = EnvelopePayload.fromJson(string);
            byte[] bytes = this.decoder.decode(envelopePayload.nonce());
            byte[] bytes2 = this.decoder.decode(envelopePayload.ciphertext());
            byte[] bytes3 = this.decoder.decode(envelopePayload.tag());
            byte[] bytes4 = this.deriveEncryptionKey(bytes);
            byte[] bytes5 = this.deriveCounterBase(bytes, bytes4);
            byte[] bytes6 = this.computeTag(bytes, bytes4, bytes5, bytes2);
            if (!MessageDigest.isEqual(bytes3, bytes6)) {
                throw new SecureEnvelopeException("DryGascon2 tag mismatch");
            }

            byte[] bytes7 = this.cipher.process(bytes4, bytes5, bytes2);
            return new String(bytes7, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new SecureEnvelopeException("Invalid secure envelope encoding", illegalArgumentException);
        }
    }

    private byte[] deriveEncryptionKey(byte[] bytes) {
        byte[] bytes2 = this.labeledHash(ENC_LEFT_LABEL, this.masterKey, bytes, this.reversed(this.masterKey));
        byte[] bytes3 = this.labeledHash(ENC_RIGHT_LABEL, this.rotated(this.masterKey, 5), this.reversed(bytes), bytes2);
        return this.xor(bytes2, bytes3);
    }

    private byte[] deriveCounterBase(byte[] bytes, byte[] bytes2) {
        return this.labeledHash(CTR_BASE_LABEL, bytes, bytes2, this.rotated(this.masterKey, 11));
    }

    private byte[] computeTag(byte[] bytes, byte[] bytes2, byte[] bytes3, byte[] bytes4) {
        byte[] bytes5 = this.labeledHash(MAC_LEFT_LABEL, this.masterKey, bytes, bytes2, bytes3, bytes4);
        byte[] bytes6 = this.labeledHash(MAC_RIGHT_LABEL, this.rotated(this.masterKey, 17), this.reversed(bytes), this.reversed(bytes4), this.reversed(bytes3));
        return this.xor(bytes5, bytes6);
    }

    private byte[] generateNonce() {
        Instant instant = Instant.now();
        long l = instant.toEpochMilli();
        long l2 = instant.getEpochSecond();
        long l3 = this.nonceCounter.incrementAndGet();
        long l4 = System.nanoTime();
        byte[] bytes = new byte[16];
        this.secureRandom.nextBytes(bytes);
        byte[] bytes2 = this.concat(
            this.lengthPrefixed(this.longToBytes(l)),
            this.lengthPrefixed(this.longToBytes(l2)),
            this.lengthPrefixed(this.longToBytes(l3)),
            this.lengthPrefixed(this.longToBytes(l4)),
            this.lengthPrefixed(bytes)
        );
        return Arrays.copyOf(this.labeledHash("nonce-dyn".getBytes(StandardCharsets.US_ASCII), bytes2, this.masterKey), 16);
    }

    private byte[] concat(byte[]... byteses) {
        int i = 0;

        for (byte[] bytes : byteses) {
            i += bytes.length;
        }

        byte[] bytes2 = new byte[i];
        int i2 = 0;

        for (byte[] bytes3 : byteses) {
            System.arraycopy(bytes3, 0, bytes2, i2, bytes3.length);
            i2 += bytes3.length;
        }

        return bytes2;
    }

    private byte[] labeledHash(byte[] bytes, byte[]... byteses) {
        byte[][] byteses2 = new byte[byteses.length + 1][];
        byteses2[0] = this.lengthPrefixed(bytes);

        for (int i = 0; i < byteses.length; i++) {
            byteses2[i + 1] = this.lengthPrefixed(byteses[i]);
        }

        return this.hash.hash(this.concat(byteses2));
    }

    private byte[] lengthPrefixed(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + bytes.length);
        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
        return byteBuffer.array();
    }

    private byte[] longToBytes(long l) {
        return ByteBuffer.allocate(8).putLong(l).array();
    }

    private byte[] reversed(byte[] bytes) {
        byte[] bytes2 = Arrays.copyOf(bytes, bytes.length);
        int i = 0;

        for (int i2 = bytes2.length - 1; i < i2; i2--) {
            byte b = bytes2[i];
            bytes2[i] = bytes2[i2];
            bytes2[i2] = b;
            i++;
        }

        return bytes2;
    }

    private byte[] rotated(byte[] bytes, int i) {
        byte[] bytes2 = new byte[bytes.length];

        for (int i2 = 0; i2 < bytes.length; i2++) {
            bytes2[i2] = bytes[(i2 + i) % bytes.length];
        }

        return bytes2;
    }

    private byte[] xor(byte[] bytes, byte[] bytes2) {
        byte[] bytes3 = new byte[Math.min(bytes.length, bytes2.length)];

        for (int i = 0; i < bytes3.length; i++) {
            bytes3[i] = (byte)(bytes[i] ^ bytes2[i]);
        }

        return bytes3;
    }
}
