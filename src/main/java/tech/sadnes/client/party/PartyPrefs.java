package tech.sadnes.client.party;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;

public final class PartyPrefs {
    private static final Logger LOGGER = LoggerFactory.getLogger("PartyPrefs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> PINNED = new LinkedHashSet<>();
    private static final Set<String> IGNORED = new LinkedHashSet<>();
    private static Path path;
    private static boolean loaded;

    private PartyPrefs() {
    }

    public static boolean isPinned(String string) {
        ensureLoaded();
        return string != null && PINNED.contains(normalize(string));
    }

    public static boolean isIgnored(String string) {
        ensureLoaded();
        return string != null && IGNORED.contains(normalize(string));
    }

    public static void togglePinned(String string) {
        if (string != null && !string.isBlank()) {
            ensureLoaded();
            String string2 = normalize(string);
            if (!PINNED.remove(string2)) {
                PINNED.add(string2);
                IGNORED.remove(string2);
            }

            save();
        }
    }

    public static void toggleIgnored(String string) {
        if (string != null && !string.isBlank()) {
            ensureLoaded();
            String string2 = normalize(string);
            if (!IGNORED.remove(string2)) {
                IGNORED.add(string2);
                PINNED.remove(string2);
            }

            save();
        }
    }

    private static String normalize(String string) {
        return string.toLowerCase(Locale.ROOT);
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            Path path2 = getPath();
            if (path2 != null && Files.exists(path2)) {
                try {
                    JsonObject jsonObject = GSON.fromJson(Files.readString(path2, StandardCharsets.UTF_8), JsonObject.class);
                    if (jsonObject == null) {
                        return;
                    }

                    readSet(jsonObject, "pinned", PINNED);
                    readSet(jsonObject, "ignored", IGNORED);
                } catch (IOException | RuntimeException exception) {
                    LOGGER.warn("[party] не удалось прочитать пометки: {}", exception.toString());
                }
            }
        }
    }

    private static void readSet(JsonObject jsonObject, String string, Set<String> strings) {
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement != null && jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            int i = 0;

            for (int i2 = jsonArray.size(); i < i2; i++) {
                strings.add(normalize(jsonArray.get(i).getAsString()));
            }
        }
    }

    private static void save() {
        Path path2 = getPath();
        if (path2 != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("pinned", toJsonArray(PINNED));
            jsonObject.add("ignored", toJsonArray(IGNORED));

            try {
                Files.createDirectories(path2.getParent());
                Files.writeString(path2, GSON.toJson(jsonObject), StandardCharsets.UTF_8);
            } catch (IOException ioException) {
                LOGGER.warn("[party] не удалось сохранить пометки: {}", ioException.toString());
            }
        }
    }

    private static JsonArray toJsonArray(Set<String> strings) {
        JsonArray jsonArray = new JsonArray();

        for (String string : strings) {
            jsonArray.add(string);
        }

        return jsonArray;
    }

    private static Path getPath() {
        if (path != null) {
            return path;
        } else {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null && minecraftClient.runDirectory != null) {
                path = minecraftClient.runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("party").resolve("prefs.json");
                return path;
            } else {
                return null;
            }
        }
    }
}
