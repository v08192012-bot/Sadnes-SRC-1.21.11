package tech.sadnes.client.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import local.sadnes.OfflineRouting;
import tech.sadnes.client.network.BackendUrls;

public final class HolyWorldEventsApi {
    private static final String EVENTS_ENDPOINT = "/events/holy";
    private static final String SERVERS_ENDPOINT = "/events/holy/servers";
    private static final long REFRESH_INTERVAL_MS = 10000L;
    private static final long SERVER_NAMES_TTL_MS = 300000L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();
    private static final AtomicBoolean FETCHING = new AtomicBoolean();
    private static volatile List<HolyWorldEventsApi.EventInfo> events = List.of();
    private static volatile Map<String, String> serverNames = Map.of();
    private static volatile long lastRequestTime;
    private static volatile long serverNamesUpdateTime;
    private static volatile boolean loaded;

    private HolyWorldEventsApi() {
    }

    public static List<HolyWorldEventsApi.EventInfo> getEvents() {
        return events;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void refresh() {
        long l = System.currentTimeMillis();
        if (l - lastRequestTime >= 10000L && FETCHING.compareAndSet(false, true)) {
            lastRequestTime = l;
            CompletableFuture.runAsync(HolyWorldEventsApi::fetchEvents).whenComplete((void2, throwable) -> FETCHING.set(false));
        }
    }

    private static void fetchEvents() {
        try {
            refreshServerNames();
            JsonObject jsonObject = JsonParser.parseString(fetch(BackendUrls.urlNoCache("/events/holy"))).getAsJsonObject();
            ArrayList arrayList = new ArrayList();

            for (Entry entry : jsonObject.entrySet()) {
                if (((JsonElement)entry.getValue()).isJsonArray()) {
                    String string = (String)entry.getKey();
                    int i = parseAnarchyNumber(string);
                    String string2 = serverNames.getOrDefault(string, string);

                    for (JsonElement jsonElement : ((JsonElement)entry.getValue()).getAsJsonArray()) {
                        if (jsonElement.isJsonObject()) {
                            JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                            String string3 = getString(jsonObject2, "id", "");
                            JsonObject jsonObject3 = jsonObject2.has("metadata") && jsonObject2.get("metadata").isJsonObject() ? jsonObject2.getAsJsonObject("metadata") : jsonObject2;
                            String string4 = getString(jsonObject3, "displayName", string3.isEmpty() ? "?" : string3);
                            arrayList.add(new HolyWorldEventsApi.EventInfo(string3, string4, i, string2, getString(jsonObject3, "rare", "")));
                        }
                    }
                }
            }

            arrayList.sort(Comparator.comparingInt(HolyWorldEventsApi.EventInfo::anarchy));
            events = arrayList;
            loaded = true;
        } catch (Exception exception) {
            loaded = false;
        }
    }

    private static void refreshServerNames() throws Exception {
        long l = System.currentTimeMillis();
        if (serverNames.isEmpty() || l - serverNamesUpdateTime >= 300000L) {
            JsonObject jsonObject = JsonParser.parseString(fetch(BackendUrls.urlNoCache("/events/holy/servers"))).getAsJsonObject();
            HashMap hashMap = new HashMap();

            for (Entry entry : jsonObject.entrySet()) {
                if (((JsonElement)entry.getValue()).isJsonPrimitive()) {
                    hashMap.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
                }
            }

            serverNames = hashMap;
            serverNamesUpdateTime = l;
        }
    }

    private static int parseAnarchyNumber(String string) {
        int i = string.lastIndexOf(95);
        if (i >= 0 && i + 1 < string.length()) {
            try {
                return Integer.parseInt(string.substring(i + 1));
            } catch (NumberFormatException numberFormatException) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static String getString(JsonObject jsonObject, String string, String string2) {
        JsonElement jsonElement = jsonObject.get(string);
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString() ? jsonElement.getAsString() : string2;
    }

    private static String fetch(String string) throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(string)))
            .header("User-Agent", "HWEvents/1.0")
            .timeout(Duration.ofSeconds(8L))
            .GET()
            .build();
        return HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
    }

    public record EventInfo(String type, String name, int anarchy, String server, String rare) {
    }
}
