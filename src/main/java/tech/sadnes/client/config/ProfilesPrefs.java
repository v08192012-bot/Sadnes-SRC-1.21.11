package tech.sadnes.client.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;

public final class ProfilesPrefs {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProfilesPrefs");
    private static final Gson GSON = new Gson();
    private static final Type PROFILES_TYPE = (new TypeToken<LinkedHashMap<String, LinkedHashMap<String, String>>>() {}).getType();
    private static final LinkedHashMap<String, LinkedHashMap<String, String>> PROFILES = new LinkedHashMap<>();
    private static boolean loaded;

    private ProfilesPrefs() {
    }

    public static List<String> getProfileNames() {
        load();
        return new ArrayList<>(PROFILES.keySet());
    }

    public static Map<String, String> getProfile(String string) {
        load();
        return PROFILES.get(string);
    }

    public static void saveProfile(String string, Map<String, String> map) {
        load();
        if (string != null && !string.isBlank()) {
            PROFILES.put(string.trim(), new LinkedHashMap<>(map));
            save();
        }
    }

    public static void deleteProfile(String string) {
        load();
        if (PROFILES.remove(string) != null) {
            save();
        }
    }

    private static void load() {
        if (!loaded) {
            loaded = true;

            try {
                Path path = getPath();
                if (!Files.exists(path)) {
                    return;
                }

                LinkedHashMap linkedHashMap = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), PROFILES_TYPE);
                if (linkedHashMap != null) {
                    PROFILES.putAll(linkedHashMap);
                }
            } catch (Exception exception) {
                LOGGER.warn("Failed to load profiles: {}", exception.getMessage());
            }
        }
    }

    private static void save() {
        try {
            Path path = getPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(PROFILES, PROFILES_TYPE), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save profiles: {}", exception.getMessage());
        }
    }

    private static Path getPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("profiles.json");
    }
}
