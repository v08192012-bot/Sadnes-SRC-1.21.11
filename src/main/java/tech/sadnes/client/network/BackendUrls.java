package tech.sadnes.client.network;

import java.net.URI;
import local.sadnes.OfflineRouting;

public final class BackendUrls {
    private static final String DEFAULT_SERVER = "wss://" + OfflineRouting.BACKEND_HOST + "/ws";

    private BackendUrls() {
    }

    public static String url(String path) {
        return getHttpBase() + path;
    }

    public static String urlNoCache(String path) {
        return url(path) + (path.indexOf('?') < 0 ? "?t=" : "&t=") + System.currentTimeMillis();
    }

    public static String getWebSocketUrl() {
        return OfflineRouting.rewrite(URI.create(DEFAULT_SERVER)).toString();
    }

    public static String getHttpBase() {
        URI server = URI.create(getWebSocketUrl());
        String scheme = "wss".equals(server.getScheme()) ? "https" : "http";
        return scheme + "://" + server.getRawAuthority();
    }
}
