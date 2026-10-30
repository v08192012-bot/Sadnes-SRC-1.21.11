package tech.sadnes.client.util;

import net.fabricmc.loader.api.FabricLoader;
import tech.sadnes.client.ClientInfo;

public final class ModVersion {
    private static final String VERSION = readVersion();

    private ModVersion() {
    }

    public static String getVersion() {
        return VERSION;
    }

    public static boolean matchesVersion(String string) {
        return string != null && !string.isEmpty() ? VERSION.equals(string) : true;
    }

    private static String readVersion() {
        return FabricLoader.getInstance().getModContainer(ClientInfo.MOD_ID).map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }
}
