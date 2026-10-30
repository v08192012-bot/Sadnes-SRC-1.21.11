package tech.sadnes.client.util.compat;

public final class ViaPacketDeduplicator {
    private static Object lastUser;
    private static float lastYaw;
    private static float lastPitch;
    private static byte lastFlags;

    private ViaPacketDeduplicator() {
    }

    public static boolean shouldSend(Object object, float f, float f2, byte b) {
        if (!ProtocolUtil.isLegacyServer()) {
            return true;
        }

        if (object == lastUser && f == lastYaw && f2 == lastPitch && b == lastFlags) {
            return false;
        }

        lastUser = object;
        lastYaw = f;
        lastPitch = f2;
        lastFlags = b;
        return true;
    }
}
