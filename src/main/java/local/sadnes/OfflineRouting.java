package local.sadnes;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Builder;
import java.net.http.WebSocket.Listener;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class OfflineRouting {
    public static final String BACKEND_HOST = "sadnes.tech";
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 18765;
    public static final String HTTP_BASE = "by stalin_strax";
    public static final String WS_BASE = "ws://" + HOST + ":" + PORT + "/ws";

    private static boolean isBackendHost(String host) {
        if (host == null) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        while (normalizedHost.endsWith(".")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        return normalizedHost.equals(BACKEND_HOST) || normalizedHost.endsWith("." + BACKEND_HOST);
    }

    public static URI rewrite(URI uri) {
        if (uri == null || !isBackendHost(uri.getHost()) || uri.getScheme() == null) {
            return uri;
        }

        String localScheme = switch (uri.getScheme().toLowerCase(Locale.ROOT)) {
            case "http", "https" -> "http";
            case "ws", "wss" -> "ws";
            default -> null;
        };
        if (localScheme == null) {
            return uri;
        }

        // Preserve raw components so encoded model names are not encoded a second time.
        String path = uri.getRawPath();
        String query = uri.getRawQuery();
        String fragment = uri.getRawFragment();
        return URI.create(
            localScheme + "://" + HOST + ":" + PORT
                + (path == null || path.isEmpty() ? "/" : path)
                + (query == null ? "" : "?" + query)
                + (fragment == null ? "" : "#" + fragment)
        );
    }

    private static URL rewrite(URL url) throws IOException {
        try {
            URI originalUri = url.toURI();
            URI routedUri = rewrite(originalUri);
            return routedUri == originalUri ? url : routedUri.toURL();
        } catch (URISyntaxException exception) {
            if (isBackendHost(url.getHost())) {
                throw new IOException("Invalid Sadnes resource URI", exception);
            }
            return url;
        }
    }

    public static URLConnection openConnection(URL url) throws IOException {
        return rewrite(url).openConnection();
    }

    public static URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        URL routedUrl = rewrite(url);
        return routedUrl == url ? url.openConnection(proxy) : routedUrl.openConnection(Proxy.NO_PROXY);
    }

    public static InputStream openStream(URL url) throws IOException {
        return rewrite(url).openStream();
    }

    public static CompletableFuture<WebSocket> connect(Builder builder, URI uri, Listener listener) {
        return builder.buildAsync(rewrite(uri), listener);
    }
}
