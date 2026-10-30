package tech.sadnes.client.server;

import com.google.gson.JsonArray;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import local.sadnes.OfflineRouting;
import tech.sadnes.client.network.BackendUrls;

public final class FunTimeEventsApi {
    private static final String ENDPOINT = "/events/funtime";
    private static final long REFRESH_INTERVAL_MS = 5000L;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();
    private static final AtomicBoolean FETCHING = new AtomicBoolean();
    private static volatile List<FunTimeEventsApi.EventInfo> events = List.of();
    private static volatile long lastRequestTime;
    private static volatile long lastUpdateTime;
    private static volatile boolean loaded;

    private FunTimeEventsApi() {
    }

    public static List<FunTimeEventsApi.EventInfo> getEvents() {
        return events;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public static void refresh() {
        long l = System.currentTimeMillis();
        if (l - lastRequestTime >= 5000L && FETCHING.compareAndSet(false, true)) {
            lastRequestTime = l;
            CompletableFuture.runAsync(FunTimeEventsApi::fetchEvents).whenComplete((void2, throwable) -> FETCHING.set(false));
        }
    }

    private static void fetchEvents() {
        try {
            JsonObject jsonObject = JsonParser.parseString(fetchJson()).getAsJsonObject();
            JsonArray jsonArray = jsonObject.getAsJsonArray("response");
            ArrayList arrayList = new ArrayList();

            for (JsonElement jsonElement : jsonArray) {
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    int i = parseServerNumber(getString(jsonObject2, "server", ""));
                    if (i >= 100 && jsonObject2.has("events") && jsonObject2.get("events").isJsonArray()) {
                        for (JsonElement jsonElement2 : jsonObject2.getAsJsonArray("events")) {
                            if (jsonElement2.isJsonObject()) {
                                JsonObject jsonObject3 = jsonElement2.getAsJsonObject();
                                String string = normalizeEventId(getString(jsonObject3, "id", ""));
                                String string2 = getString(jsonObject3, "loot", "");
                                if ("null".equals(string2)) {
                                    string2 = "";
                                }

                                String string3 = getString(jsonObject3, "phase", "").toUpperCase(Locale.ROOT);
                                JsonObject jsonObject4 = jsonObject3.has("location-event") && jsonObject3.get("location-event").isJsonObject()
                                    ? jsonObject3.getAsJsonObject("location-event")
                                    : null;
                                boolean flag = jsonObject4 != null && jsonObject3.has("location-announced") && jsonObject3.get("location-announced").getAsBoolean();
                                boolean flag2 = "system".equals(getString(jsonObject3, "event-type", ""));
                                arrayList.add(
                                    new FunTimeEventsApi.EventInfo(
                                        string,
                                        i,
                                        string.isEmpty(),
                                        string2,
                                        string3,
                                        getInt(jsonObject3, "time-seconds-left"),
                                        flag,
                                        flag ? getInt(jsonObject4, "x") : 0,
                                        flag ? getInt(jsonObject4, "y") : 0,
                                        flag ? getInt(jsonObject4, "z") : 0,
                                        flag2
                                    )
                                );
                            }
                        }
                    }
                }
            }

            arrayList.sort(Comparator.comparingInt(FunTimeEventsApi.EventInfo::anarchy));
            events = arrayList;
            lastUpdateTime = System.currentTimeMillis();
            loaded = true;
        } catch (Exception exception) {
            loaded = false;
        }
    }

    private static String normalizeEventId(String string) {
        if (string.equals("vulkan")) {
            return "volcano";
        } else {
            return string.equals("vote") ? "voting" : string;
        }
    }

    private static int parseServerNumber(String string) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c >= '0' && c <= '9') {
                builder.append(c);
            }
        }

        if (builder.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(builder.toString());
        } catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }

    private static String getString(JsonObject jsonObject, String string, String string2) {
        JsonElement jsonElement = jsonObject.get(string);
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString() ? jsonElement.getAsString() : string2;
    }

    private static int getInt(JsonObject jsonObject, String string) {
        JsonElement jsonElement = jsonObject.get(string);
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber() ? jsonElement.getAsInt() : 0;
    }

    private static String fetchJson() throws Exception {
        HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(BackendUrls.urlNoCache("/events/funtime"))))
            .timeout(Duration.ofSeconds(8L))
            .GET()
            .build();
        return HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
    }

    public record EventInfo(
        String id, int anarchy, boolean upcoming, String loot, String phase, int secondsLeft, boolean located, int x, int y, int z, boolean system
    ) {
    }
}
