package tech.sadnes.client.util.media;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import local.sadnes.OfflineRouting;

public final class LyricsFetcher {
    private static final String API_URL = "https://lrclib.net/api/";
    private static final Pattern TITLE_NOISE_PATTERN = Pattern.compile(
        "\\s*[(\\[][^()\\[\\]]*(official|video|audio|lyrics?|remaster|explicit|hd|mv)[^()\\[\\]]*[)\\]]", 2
    );
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).build();
    private static final AtomicBoolean FETCHING = new AtomicBoolean();
    private static volatile String currentKey = "";
    private static volatile List<LyricsFetcher.LyricLine> lines = List.of();

    private LyricsFetcher() {
    }

    public static List<LyricsFetcher.LyricLine> getLines() {
        return lines;
    }

    public static void request(String string, String string2, long l) {
        if (!string.isEmpty()) {
            String string3 = string + "|" + string2;
            if (!string3.equals(currentKey) && FETCHING.compareAndSet(false, true)) {
                currentKey = string3;
                lines = List.of();
                CompletableFuture.runAsync(() -> fetch(string3, string, string2, l)).whenComplete((void2, throwable) -> FETCHING.set(false));
            }
        }
    }

    public static void clear() {
        currentKey = "";
        lines = List.of();
    }

    public static int getLineIndex(long l) {
        List list = lines;
        int i = list.size();
        int i2 = -1;

        for (int i3 = 0; i3 < i; i2 = i3++) {
            if (((LyricsFetcher.LyricLine)list.get(i3)).timeMs() > l) {
                return i2;
            }
        }

        return i2;
    }

    private static void fetch(String string, String string2, String string3, long l) {
        String string4 = cleanTitle(string2);
        String string5 = fetchExact(string4, string3, l);
        if (string5 == null) {
            string5 = fetchSearch(string4, string3);
        }

        List list = string5 == null ? List.of() : parseLrc(string5);
        if (string.equals(currentKey)) {
            lines = list;
        }
    }

    private static String fetchExact(String string, String string2, long l) {
        StringBuilder builder = new StringBuilder("https://lrclib.net/api/")
            .append("get?track_name=")
            .append(urlEncode(string))
            .append("&artist_name=")
            .append(urlEncode(string2));
        if (l > 0L) {
            builder.append("&duration=").append(Math.round(l / 1000.0));
        }

        JsonObject jsonObject = parseObject(httpGet(builder.toString()));
        return jsonObject == null ? null : getSyncedLyrics(jsonObject);
    }

    private static String fetchSearch(String string, String string2) {
        String string3 = "https://lrclib.net/api/search?track_name=" + urlEncode(string) + "&artist_name=" + urlEncode(string2);
        JsonElement jsonElement = parseJson(httpGet(string3));
        if (jsonElement != null && jsonElement.isJsonArray()) {
            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                if (jsonElement2.isJsonObject()) {
                    String string4 = getSyncedLyrics(jsonElement2.getAsJsonObject());
                    if (string4 != null) {
                        return string4;
                    }
                }
            }
        }

        return null;
    }

    private static String getSyncedLyrics(JsonObject jsonObject) {
        JsonElement jsonElement = jsonObject.get("syncedLyrics");
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            String string = jsonElement.getAsString();
            return string.isBlank() ? null : string;
        } else {
            return null;
        }
    }

    private static List<LyricsFetcher.LyricLine> parseLrc(String string) {
        ArrayList arrayList = new ArrayList();

        for (String string2 : string.split("\n")) {
            String string3 = string2.trim();
            int i = 0;
            ArrayList<Long> longValues = new ArrayList<>(1);

            while (string3.startsWith("[", i)) {
                int i2 = string3.indexOf(93, i);
                if (i2 < 0) {
                    break;
                }

                long l = parseTimestamp(string3.substring(i + 1, i2));
                if (l < 0L) {
                    break;
                }

                longValues.add(l);
                i = i2 + 1;
            }

            if (!longValues.isEmpty()) {
                String string4 = string3.substring(i).trim();

                for (long l2 : longValues) {
                    arrayList.add(new LyricsFetcher.LyricLine(l2, string4));
                }
            }
        }

        arrayList.sort(Comparator.comparingLong(LyricsFetcher.LyricLine::timeMs));
        return List.copyOf(arrayList);
    }

    private static long parseTimestamp(String string) {
        int i = string.indexOf(58);
        if (i <= 0) {
            return -1L;
        }

        try {
            long l = Long.parseLong(string.substring(0, i).trim());
            double d = Double.parseDouble(string.substring(i + 1).trim().replace(',', '.'));
            return l * 60000L + (long)(d * 1000.0);
        } catch (NumberFormatException numberFormatException) {
            return -1L;
        }
    }

    private static String cleanTitle(String string) {
        String string2 = TITLE_NOISE_PATTERN.matcher(string).replaceAll("").trim();
        return string2.isBlank() ? string : string2;
    }

    private static JsonObject parseObject(String string) {
        JsonElement jsonElement = parseJson(string);
        return jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : null;
    }

    private static JsonElement parseJson(String string) {
        if (string == null) {
            return null;
        }

        try {
            return JsonParser.parseString(string);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String httpGet(String string) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(string)))
                .header("User-Agent", "Mozilla/5.0")
                .timeout(Duration.ofSeconds(8L))
                .GET()
                .build();
            HttpResponse httpResponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
            return httpResponse.statusCode() == 200 ? (String)httpResponse.body() : null;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static String urlEncode(String string) {
        return URLEncoder.encode(string, StandardCharsets.UTF_8);
    }

    public record LyricLine(long timeMs, String text) {
    }
}
