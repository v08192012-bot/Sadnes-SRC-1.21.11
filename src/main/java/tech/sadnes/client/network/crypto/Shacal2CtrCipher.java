package tech.sadnes.client.network.crypto;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.bouncycastle.crypto.engines.Shacal2Engine;
import org.bouncycastle.crypto.params.KeyParameter;

public final class Shacal2CtrCipher {
    private static final int COUNTER_BASE_SIZE = 32;

    public byte[] process(byte[] bytes, byte[] bytes2, byte[] bytes3) {
        if (bytes2.length != 32) {
            throw new SecureEnvelopeException("Expected 32-byte counter base for SHACAL-2 CTR");
        }

        Shacal2Engine shacal2Engine = new Shacal2Engine();
        shacal2Engine.init(true, new KeyParameter(bytes));
        int i = shacal2Engine.getBlockSize();
        byte[] bytes4 = new byte[bytes3.length];
        byte[] bytes5 = new byte[i];

        for (int i2 = 0; i2 < bytes3.length; i2 += i) {
            byte[] bytes6 = Arrays.copyOf(bytes2, bytes2.length);
            this.applyCounter(bytes6, (long)i2 / i);
            shacal2Engine.processBlock(bytes6, 0, bytes5, 0);
            int i3 = Math.min(i, bytes3.length - i2);

            for (int i4 = 0; i4 < i3; i4++) {
                bytes4[i2 + i4] = (byte)(bytes3[i2 + i4] ^ bytes5[i4]);
            }
        }

        return bytes4;
    }

    private void applyCounter(byte[] bytes, long l) {
        byte[] bytes2 = ByteBuffer.allocate(8).putLong(l).array();

        for (int i = 0; i < 8; i++) {
            bytes[bytes.length - 8 + i] = (byte)(bytes[bytes.length - 8 + i] ^ bytes2[i]);
            bytes[i] ^= bytes2[7 - i];
        }
    }
}
