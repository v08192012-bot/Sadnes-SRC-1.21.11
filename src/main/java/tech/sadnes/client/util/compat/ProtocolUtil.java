package tech.sadnes.client.util.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import tech.sadnes.client.util.IMinecraft;

public final class ProtocolUtil {
    private static final boolean VIA_LOADED = FabricLoader.getInstance().isModLoaded("viafabricplus");
    private static boolean failed;

    private ProtocolUtil() {
    }

    public static boolean isAvailable() {
        return true;
    }

    public static boolean isLegacyServer() {
        if (VIA_LOADED && !failed) {
            try {
                return ViaProtocolInfo.isLegacyProtocol();
            } catch (Throwable throwable) {
                failed = true;
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean sendLegacyClickSlot(ClickSlotC2SPacket clickSlotC2SPacket) {
        if (VIA_LOADED && !failed) {
            try {
                if (!ViaClickSlotTranslator.isSupported()) {
                    return false;
                }

                ViaClickSlotTranslator.sendClickSlot(clickSlotC2SPacket);
                return true;
            } catch (Throwable throwable) {
                failed = true;
                return false;
            }
        } else {
            return false;
        }
    }

    public static int getProtocolVersion() {
        if (VIA_LOADED && !failed) {
            try {
                return ViaProtocolInfo.getProtocolVersion();
            } catch (Throwable throwable) {
                failed = true;
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static String getServerVersion() {
        String string = null;
        if (VIA_LOADED && !failed) {
            try {
                string = ViaProtocolInfo.getVersionName();
            } catch (Throwable throwable) {
                failed = true;
            }
        }

        return string != null && !string.isEmpty() ? string : IMinecraft.mc.getGameVersion();
    }

    public static boolean isVersion(String string) {
        return getServerVersion().equals(string);
    }

    public static boolean isOlderThan(String string) {
        return compareVersions(getServerVersion(), string) < 0;
    }

    public static int compareVersions(String string, String string2) {
        String[] strings = string.split("\\.");
        String[] strings2 = string2.split("\\.");
        int i = Math.max(strings.length, strings2.length);

        for (int i2 = 0; i2 < i; i2++) {
            int i3 = i2 < strings.length ? parseLeadingInt(strings[i2]) : 0;
            int i4 = i2 < strings2.length ? parseLeadingInt(strings2[i2]) : 0;
            if (i3 != i4) {
                return Integer.compare(i3, i4);
            }
        }

        return 0;
    }

    private static int parseLeadingInt(String string) {
        int i = 0;
        int i2 = string.length();

        while (i < i2 && Character.isDigit(string.charAt(i))) {
            i++;
        }

        return i == 0 ? 0 : Integer.parseInt(string.substring(0, i));
    }
}
