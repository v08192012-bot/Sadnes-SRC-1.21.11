package tech.sadnes.client.network.crypto;

import java.util.Arrays;

public final class DryGascon2Hash {
    private static final int RATE_BYTES = 16;
    private static final int X_BYTES = 16;
    private static final int STATE_BYTES = 40;
    private static final int OUTPUT_HALF_BYTES = 16;
    private static final int WORD_BYTES = 4;
    private static final int OUTPUT_ROUNDS = 7;
    private static final int OUTPUT_BLOCKS = 2;
    private static final int MIX_BITS = 10;
    private static final int MIX_MASK = 1023;
    private static final int HASH_DOMAIN = 2;
    private static final int SETUP_PAIRS = 3;
    private static final int MIX_ROUNDS = 14;
    private static final int FINAL_DOMAIN = 2;
    private static final byte[] SETUP_KEY = createSetupKey();

    public byte[] hash(byte[] bytes) {
        byte[] bytes2 = new byte[40];
        byte[] bytes3 = new byte[16];
        byte[] bytes4 = new byte[16];
        this.initState(bytes2, bytes3, SETUP_KEY);
        DryGascon2Hash.MixState mixState = new DryGascon2Hash.MixState();
        this.absorb(bytes2, bytes3, bytes4, bytes, 2, true, mixState);
        byte[] bytes5 = Arrays.copyOf(bytes4, 32);
        byte[] bytes6 = this.squeeze(bytes2, bytes4);
        System.arraycopy(bytes6, 0, bytes5, 16, 16);
        return bytes5;
    }

    private void absorb(byte[] bytes, byte[] bytes2, byte[] bytes3, byte[] bytes4, int i, boolean flag, DryGascon2Hash.MixState mixState) {
        int i2 = Math.max(1, (bytes4.length + 16 - 1) / 16);
        byte[] bytes5 = new byte[Math.max(0, bytes4.length - (i2 - 1) * 16)];
        if (bytes5.length > 0) {
            System.arraycopy(bytes4, (i2 - 1) * 16, bytes5, 0, bytes5.length);
        }

        boolean flag2 = bytes5.length != 16;
        byte[] bytes6 = this.pad(bytes5);

        for (int i3 = 0; i3 < i2 - 1; i3++) {
            byte[] bytes7 = Arrays.copyOfRange(bytes4, i3 * 16, (i3 + 1) * 16);
            this.absorbBlock(bytes, bytes2, bytes3, bytes7, null, mixState);
        }

        this.absorbBlock(bytes, bytes2, bytes3, bytes6, new DryGascon2Hash.BlockFlags(flag2, i, flag), mixState);
    }

    private void absorbBlock(byte[] bytes, byte[] bytes2, byte[] bytes3, byte[] bytes4, DryGascon2Hash.BlockFlags blockFlags, DryGascon2Hash.MixState mixState) {
        if (bytes4.length != 16) {
            throw new SecureEnvelopeException("DryGascon2 requires 16-byte rate blocks");
        }

        if (blockFlags != null) {
            mixState.domainBits = blockFlags.padded() ? 1 : 0;
            if (blockFlags.finalizeFlag()) {
                mixState.domainBits |= 2;
            }

            mixState.domainBits = mixState.domainBits | blockFlags.domain() << 2;
        }

        this.mixBlock(bytes, bytes2, bytes4, mixState);
        this.squeeze(bytes, bytes3);
    }

    private byte[] squeeze(byte[] bytes, byte[] bytes2) {
        Arrays.fill(bytes2, (byte)0);

        for (int i = 0; i < 7; i++) {
            byte[] bytes3 = this.permute(bytes, i);
            System.arraycopy(bytes3, 0, bytes, 0, 40);

            for (int i2 = 0; i2 < 2; i2++) {
                int i3 = i2 * 4 * 4;
                byte[] bytes4 = Arrays.copyOfRange(bytes, i3, Math.min(i3 + 16, bytes.length));
                if (bytes4.length < 16) {
                    bytes4 = Arrays.copyOf(bytes4, 16);
                }

                byte[] bytes5 = new byte[16];
                int i4 = i2 * 4;

                for (int i5 = 0; i5 < 16; i5++) {
                    bytes5[i5] = bytes4[(i5 + i4) % 16];
                }

                for (int i6 = 0; i6 < 16; i6++) {
                    bytes2[i6] ^= bytes5[i6];
                }
            }
        }

        return Arrays.copyOf(bytes2, bytes2.length);
    }

    private void mixBlock(byte[] bytes, byte[] bytes2, byte[] bytes3, DryGascon2Hash.MixState mixState) {
        byte b = 0;

        for (int i = 0; i < 13; i++) {
            this.mixStep(bytes, bytes2, bytes3, b, mixState.domainBits);
            b += 10;
            byte[] bytes4 = this.permute(bytes, 0);
            System.arraycopy(bytes4, 0, bytes, 0, 40);
        }

        this.mixStep(bytes, bytes2, bytes3, b, mixState.domainBits);
        mixState.domainBits = 0;
    }

    private void mixStep(byte[] bytes, byte[] bytes2, byte[] bytes3, int i, int i2) {
        byte[] bytes4 = Arrays.copyOf(bytes3, bytes3.length + 1);
        bytes4[bytes4.length - 1] = (byte)i2;
        int i3 = extractBits(bytes4, i, 10);

        for (int i4 = 0; i4 < 5; i4++) {
            int i5 = i3 & 3;
            i3 >>>= 2;
            int i6 = i4 * 8;
            int i7 = i5 * 4;

            for (int i8 = 0; i8 < 4; i8++) {
                bytes[i6 + i8] = (byte)(bytes[i6 + i8] ^ bytes2[i7 + i8]);
            }
        }
    }

    private byte[] permute(byte[] bytes, int i) {
        long[] longs = new long[5];

        for (int i2 = 0; i2 < longs.length; i2++) {
            longs[i2] = readLongLE(bytes, i2 * 8);
        }

        this.coreRound(longs, i);
        byte[] bytes2 = new byte[40];

        for (int i3 = 0; i3 < longs.length; i3++) {
            writeLongLE(bytes2, i3 * 8, longs[i3]);
        }

        return bytes2;
    }

    private void coreRound(long[] longs, int i) {
        int i2 = longs.length / 2;
        longs[i2] ^= (long)(15 - i) << 4 | i;

        for (int i3 = 0; i3 <= i2; i3++) {
            int i4 = 2 * i3;
            int i5 = (longs.length + i4 - 1) % longs.length;
            longs[i4] ^= longs[i5];
        }

        long[] longs2 = new long[longs.length];

        for (int i6 = 0; i6 < longs.length; i6++) {
            longs2[i6] = ~longs[i6] & longs[(i6 + 1) % longs.length];
        }

        for (int i7 = 0; i7 < longs.length; i7++) {
            longs[i7] ^= longs2[(i7 + 1) % longs.length];
        }

        for (int i8 = 0; i8 <= i2; i8++) {
            int i9 = 2 * i8 % longs.length;
            int i10 = (i9 + 1) % longs.length;
            longs[i10] ^= longs[i9];
        }

        longs[i2] = ~longs[i2];
        longs[0] ^= this.rotateInterleaved(longs[0], 19) ^ this.rotateInterleaved(longs[0], 28);
        longs[1] ^= this.rotateInterleaved(longs[1], 61) ^ this.rotateInterleaved(longs[1], 38);
        longs[2] ^= this.rotateInterleaved(longs[2], 1) ^ this.rotateInterleaved(longs[2], 6);
        longs[3] ^= this.rotateInterleaved(longs[3], 10) ^ this.rotateInterleaved(longs[3], 17);
        longs[4] ^= this.rotateInterleaved(longs[4], 7) ^ this.rotateInterleaved(longs[4], 40);
    }

    private long rotateInterleaved(long l, int i) {
        int i2 = (int)l;
        int i3 = (int)(l >>> 32);
        int i4 = i / 2;
        int i5;
        int i6;
        if ((i & 1) == 1) {
            int i7 = Integer.rotateRight(i3, i4);
            int i8 = Integer.rotateRight(i2, (i4 + 1) % 32);
            i5 = i7;
            i6 = i8;
        } else {
            i5 = Integer.rotateRight(i2, i4);
            i6 = Integer.rotateRight(i3, i4);
        }

        return (long)i6 << 32 | i5 & 4294967295L;
    }

    private byte[] pad(byte[] bytes) {
        if (bytes.length == 16) {
            return bytes;
        }

        byte[] bytes2 = Arrays.copyOf(bytes, 16);
        bytes2[bytes.length] = 1;
        return bytes2;
    }

    private void initState(byte[] bytes, byte[] bytes2, byte[] bytes3) {
        if (bytes3.length != 32) {
            throw new SecureEnvelopeException("DryGascon2 hash expects a 32-byte setup key");
        }

        byte[] bytes4 = new byte[40];

        for (int i = 0; i < 40; i++) {
            bytes4[i] = bytes3[i % 16];
        }

        System.arraycopy(bytes4, 0, bytes, 0, 40);
        System.arraycopy(bytes3, 16, bytes2, 0, 16);
        boolean flag = false;

        for (int i2 = 0; i2 < 3; i2++) {
            for (int i3 = i2 + 1; i3 < 4; i3++) {
                flag |= Arrays.equals(Arrays.copyOfRange(bytes2, i2 * 4, (i2 + 1) * 4), Arrays.copyOfRange(bytes2, i3 * 4, (i3 + 1) * 4));
            }
        }

        int i4 = flag ? -8 : -1;

        for (int i5 = 0; i5 < 4; i5++) {
            int i6 = readIntLE(bytes2, i5 * 4, 4);
            int i7 = i6 & i4 | i5 & ~i4;
            writeIntLE(bytes2, i5 * 4, i7);
        }
    }

    private static byte[] createSetupKey() {
        String string = "243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89452821E638D01377BE5466CF34E90C6CC0AC29B7C97C50DD3F84D5B5B54709179216D5D98979FB1BD1310BA698DFB5AC2FFD72DBD01ADFB7B8E1AFED6A267E96BA7C9045F12C7F9924A19947B3916CF70801F2E2858EFC16636920D871574E69";
        byte[] bytes = hexToBytes(string);
        return Arrays.copyOf(bytes, 32);
    }

    private static byte[] hexToBytes(String string) {
        byte[] bytes = new byte[string.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            int i2 = i * 2;
            bytes[i] = (byte)Integer.parseInt(string.substring(i2, i2 + 2), 16);
        }

        return bytes;
    }

    private static int readIntLE(byte[] bytes, int i, int i2) {
        int i3 = 0;

        for (int i4 = 0; i4 < i2; i4++) {
            i3 |= (bytes[i + i4] & 255) << i4 * 8;
        }

        return i3;
    }

    private static long readLongLE(byte[] bytes, int i) {
        long l = 0L;

        for (int i2 = 0; i2 < 8; i2++) {
            l |= (long)(bytes[i + i2] & 0xFF) << i2 * 8;
        }

        return l;
    }

    private static void writeIntLE(byte[] bytes, int i, int i2) {
        for (int i3 = 0; i3 < 4; i3++) {
            bytes[i + i3] = (byte)(i2 >>> i3 * 8);
        }
    }

    private static void writeLongLE(byte[] bytes, int i, long l) {
        for (int i2 = 0; i2 < 8; i2++) {
            bytes[i + i2] = (byte)(l >>> i2 * 8);
        }
    }

    private static int extractBits(byte[] bytes, int i, int i2) {
        int i3 = 0;

        for (int i4 = 0; i4 < i2; i4++) {
            int i5 = i + i4;
            int i6 = i5 / 8;
            if (i6 < bytes.length) {
                int i7 = i5 % 8;
                int i8 = bytes[i6] >>> i7 & 1;
                i3 |= i8 << i4;
            }
        }

        return i3;
    }

    record BlockFlags(boolean padded, int domain, boolean finalizeFlag) {
    }

    static final class MixState {
        int domainBits;
    }
}
