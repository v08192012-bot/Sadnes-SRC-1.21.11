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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import local.sadnes.OfflineRouting;
import net.minecraft.client.MinecraftClient;
import tech.sadnes.client.network.BackendUrls;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;

public final class FunTimeOnlineApi {
    private static final String ENDPOINT = "/events/funtime/online";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();
    private static final AtomicBoolean FETCHING = new AtomicBoolean();

    private FunTimeOnlineApi() {
    }

    public static void joinLeastPopulatedAnarchy() {
        if (FETCHING.compareAndSet(false, true)) {
            CompletableFuture.supplyAsync(FunTimeOnlineApi::fetchLeastPopulatedAnarchy).whenComplete((anarchyOnline, throwable) -> {
                FETCHING.set(false);
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                minecraftClient.execute(() -> {
                    if (throwable == null && anarchyOnline != null && minecraftClient.getNetworkHandler() != null) {
                        minecraftClient.getNetworkHandler().sendChatCommand("an" + anarchyOnline.anarchy());
                        NotificationUtil.info(NotificationSources.reconnect(), "Захожу на анархию " + anarchyOnline.anarchy() + " (онлайн " + anarchyOnline.online() + ").");
                    } else {
                        NotificationUtil.warning(NotificationSources.reconnect(), "Не удалось получить онлайн анархий.");
                    }
                });
            });
        }
    }

    private static FunTimeOnlineApi.AnarchyOnline fetchLeastPopulatedAnarchy() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(BackendUrls.urlNoCache("/events/funtime/online"))))
                .timeout(Duration.ofSeconds(8L))
                .GET()
                .build();
            String string = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            JsonObject jsonObject = JsonParser.parseString(string).getAsJsonObject();
            JsonArray jsonArray = jsonObject.getAsJsonArray("servers");
            int i = -1;
            int i2 = Integer.MAX_VALUE;

            for (JsonElement jsonElement : jsonArray) {
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    if ("ANARCHY".equals(getString(jsonObject2, "category")) && getBoolean(jsonObject2, "status", true)) {
                        int i3 = parseServerNumber(getString(jsonObject2, "key"));
                        if (i3 >= 100 && i3 <= 999) {
                            int i4 = getInt(jsonObject2, "online", Integer.MAX_VALUE);
                            if (i4 < i2) {
                                i2 = i4;
                                i = i3;
                            }
                        }
                    }
                }
            }

            return i <= 0 ? null : new FunTimeOnlineApi.AnarchyOnline(i, i2);
        } catch (Exception exception) {
            return null;
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

    private static String getString(JsonObject jsonObject, String string) {
        JsonElement jsonElement = jsonObject.get(string);
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString() ? jsonElement.getAsString() : "";
    }

    private static int getInt(JsonObject jsonObject, String string, int i) {
        JsonElement jsonElement = jsonObject.get(string);
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isNumber() ? jsonElement.getAsInt() : i;
    }

    private static boolean getBoolean(JsonObject jsonObject, String string, boolean flag) {
        JsonElement jsonElement = jsonObject.get(string);
        return jsonElement != null && jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isBoolean() ? jsonElement.getAsBoolean() : flag;
    }

    public record AnarchyOnline(int anarchy, int online) {
    }
}
