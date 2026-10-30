package tech.sadnes.client.network;

public final class ProtocolVersion {
    public static final int PROTOCOL_VERSION = 12;
    public static final int MIN_PROTOCOL_VERSION = 8;
    public static final int FRAME_FORMAT = 1;

    private ProtocolVersion() {
    }

    public static boolean isCompatible(int i, int i2, int i3) {
        return i3 == 1 && i >= 8 && i <= 12 && 12 >= i2;
    }
}
