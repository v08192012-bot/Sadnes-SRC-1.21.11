package tech.sadnes.client.config;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;

public final class FavoritesPrefs {
    private static final Logger LOGGER = LoggerFactory.getLogger("FavoritesPrefs");
    private static final Gson GSON = new Gson();
    private static final Set<String> FAVORITES = new LinkedHashSet<>();
    private static final String CONFIG_PREFIX = "config:";
    private static boolean loaded;

    private FavoritesPrefs() {
    }

    public static boolean isFavorite(String string) {
        load();
        return FAVORITES.contains(string);
    }

    public static boolean toggle(String string) {
        load();
        if (FAVORITES.contains(string)) {
            FAVORITES.remove(string);
            save();
            return false;
        } else {
            FAVORITES.add(string);
            save();
            return true;
        }
    }

    public static Set<String> getAll() {
        load();
        return FAVORITES;
    }

    public static Set<String> getFavoriteConfigs() {
        load();
        LinkedHashSet linkedHashSet = new LinkedHashSet();

        for (String string : FAVORITES) {
            if (string.startsWith("config:")) {
                linkedHashSet.add(string.substring("config:".length()));
            }
        }

        return linkedHashSet;
    }

    public static void setFavoriteConfigs(Set<String> strings) {
        load();
        FAVORITES.removeIf(string -> string.startsWith("config:"));

        for (String string2 : strings) {
            FAVORITES.add("config:" + string2);
        }

        save();
    }

    private static void load() {
        if (!loaded) {
            loaded = true;

            try {
                Path path = getPath();
                if (!Files.exists(path)) {
                    return;
                }

                String[] strings = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), String[].class);
                if (strings != null) {
                    for (String string : strings) {
                        if (string != null && !string.isBlank()) {
                            FAVORITES.add(string);
                        }
                    }
                }
            } catch (Exception exception) {
                LOGGER.warn("Failed to load favorites: {}", exception.getMessage());
            }
        }
    }

    private static void save() {
        try {
            Path path = getPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(FAVORITES.toArray(new String[0])), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save favorites: {}", exception.getMessage());
        }
    }

    private static Path getPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("favorites.json");
    }
}
