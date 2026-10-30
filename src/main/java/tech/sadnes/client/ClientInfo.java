package tech.sadnes.client;

import java.util.List;
import local.sadnes.OfflineRouting;

public final class ClientInfo {
    public static final String NAME = "Sadnes";
    public static final String MOD_ID = "Sadnes".toLowerCase();
    public static final String MC_VERSION = "1.21.11";
    public static final String WEBSITE = OfflineRouting.BACKEND_HOST;
    public static final String BACKEND_URL = OfflineRouting.HTTP_BASE;
    public static final String VERSION_STRING = String.format("%s %s -> %s", "SadnesClient", MC_VERSION, BACKEND_URL);
    public static final String FILE_EXTENSION = ".file";
    public static final List<String> DEVELOPERS = List.of("fuflik", "yeekhira", "ElytraBobik");

    private ClientInfo() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
