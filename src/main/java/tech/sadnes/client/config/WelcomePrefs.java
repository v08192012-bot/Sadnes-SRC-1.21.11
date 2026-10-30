package tech.sadnes.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;

public final class WelcomePrefs {
    private static final Logger LOGGER = LoggerFactory.getLogger("WelcomePrefs");
    private static final Gson GSON = new Gson();
    private static Boolean shown;
    private static Boolean tourCompleted;

    private WelcomePrefs() {
    }

    public static boolean isShown() {
        load();
        return shown;
    }

    public static boolean isTourCompleted() {
        load();
        return tourCompleted;
    }

    public static void markShown() {
        load();
        shown = true;
        save();
    }

    public static void markTourCompleted() {
        load();
        tourCompleted = true;
        save();
    }

    private static void load() {
        if (shown == null) {
            shown = false;
            tourCompleted = false;

            try {
                Path path = getPath();
                if (!Files.exists(path)) {
                    return;
                }

                JsonObject jsonObject = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
                if (jsonObject == null) {
                    return;
                }

                shown = jsonObject.has("shown") && jsonObject.get("shown").getAsBoolean();
                tourCompleted = jsonObject.has("tour") && jsonObject.get("tour").getAsBoolean();
            } catch (Exception exception) {
                LOGGER.warn("Failed to load welcome prefs: {}", exception.getMessage());
            }
        }
    }

    private static void save() {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("shown", shown);
            jsonObject.addProperty("tour", tourCompleted);
            Path path = getPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(jsonObject), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save welcome prefs: {}", exception.getMessage());
        }
    }

    private static Path getPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("welcome.json");
    }
}
