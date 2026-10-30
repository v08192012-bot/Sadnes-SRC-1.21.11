package tech.sadnes.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.shader.ShapeBlur;
import tech.sadnes.client.ui.clickgui.component.HoverTooltip;
import tech.sadnes.client.ui.menu.GraphicsQuality;
import tech.sadnes.client.util.color.Theme;

public final class ThemePrefs {
    private static final Logger LOGGER = LoggerFactory.getLogger("ThemePrefs");
    private static final Gson GSON = new Gson();

    private ThemePrefs() {
    }

    public static void load() {
        try {
            Path path = getPath();
            if (!Files.exists(path)) {
                return;
            }

            JsonObject jsonObject = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (jsonObject == null) {
                return;
            }

            if (jsonObject.has("accent")) {
                Theme.setAccent(jsonObject.get("accent").getAsInt());
            }

            if (jsonObject.has("tooltips")) {
                HoverTooltip.setEnabled(jsonObject.get("tooltips").getAsBoolean());
            }

            if (jsonObject.has("text")) {
                Theme.setTextColor(jsonObject.get("text").getAsInt());
            }

            if (jsonObject.has("background")) {
                Theme.setBackground(jsonObject.get("background").getAsInt());
            }

            if (jsonObject.has("scale")) {
                GuiScale.setScale(jsonObject.get("scale").getAsFloat());
            }

            if (jsonObject.has("blur")) {
                ShapeBlur.downscale = Math.max(1, jsonObject.get("blur").getAsInt());
            }

            if (jsonObject.has("shadows")) {
                Theme.setShadowEnabled(jsonObject.get("shadows").getAsBoolean());
            }

            if (jsonObject.has("menuGraphics")) {
                JsonElement jsonElement = jsonObject.get("menuGraphics");
                if (jsonElement.getAsJsonPrimitive().isNumber()) {
                    GraphicsQuality.setByIndex(jsonElement.getAsInt());
                } else {
                    GraphicsQuality.setByName(jsonElement.getAsString());
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load theme prefs: {}", exception.getMessage());
        }
    }

    public static void save() {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("accent", Theme.getPrimary());
            jsonObject.addProperty("tooltips", HoverTooltip.isEnabled());
            jsonObject.addProperty("scale", GuiScale.getScale());
            jsonObject.addProperty("blur", ShapeBlur.downscale);
            jsonObject.addProperty("shadows", Theme.isShadowEnabled());
            jsonObject.addProperty("menuGraphics", GraphicsQuality.getCurrent().name());
            if (Theme.hasCustomText()) {
                jsonObject.addProperty("text", Theme.getTextColor());
            }

            if (Theme.hasCustomBackground()) {
                jsonObject.addProperty("background", Theme.getBackground());
            }

            Path path = getPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(jsonObject), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save theme prefs: {}", exception.getMessage());
        }
    }

    private static Path getPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("theme.json");
    }
}
