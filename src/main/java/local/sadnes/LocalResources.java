package local.sadnes;

import java.io.InputStream;

public final class LocalResources {
    private static final String ROOT = "/sadnes_local";

    public static InputStream open(String backendPath) {
        String path = backendPath.startsWith("/") ? backendPath : "/" + backendPath;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        return LocalResources.class.getResourceAsStream(ROOT + path);
    }

    public static boolean exists(String backendPath) {
        InputStream inputStream = open(backendPath);
        if (inputStream == null) {
            return false;
        }
        try {
            inputStream.close();
        } catch (Exception exception) {
        }
        return true;
    }
}
