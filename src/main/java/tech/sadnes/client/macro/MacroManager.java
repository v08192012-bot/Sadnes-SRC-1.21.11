package tech.sadnes.client.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.ScreenUtil;
import tech.sadnes.client.util.crypto.DataEncryption;

public final class MacroManager implements IMinecraft {
    private static final Logger LOGGER = LoggerFactory.getLogger("MacroManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<Integer, String> macros = new LinkedHashMap<>();
    private final Path file;

    public MacroManager() {
        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("macro");

        try {
            Files.createDirectories(path);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create macro directory", ioException);
        }

        this.file = path.resolve("macros.scfg");
    }

    public void register() {
        Sadnes.getEventBus().subscribe(this);
    }

    public void unregister() {
        Sadnes.getEventBus().unsubscribe(this);
    }

    public boolean addMacro(int i, String string) {
        if (i != Key.KEY_NONE.getKeyCode() && string != null && !string.isBlank()) {
            this.macros.put(i, string.trim());
            this.save();
            return true;
        } else {
            return false;
        }
    }

    public boolean removeMacro(int i) {
        if (this.macros.remove(i) == null) {
            return false;
        }

        this.save();
        return true;
    }

    public void clearMacros() {
        this.macros.clear();
        this.save();
    }

    public boolean isEmpty() {
        return this.macros.isEmpty();
    }

    public Map<Integer, String> getMacros() {
        return Collections.unmodifiableMap(this.macros);
    }

    @EventHandler
    public void onMouseClick(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() && ScreenUtil.allowsMovement(screenMouseClickEvent.getScreen())) {
            this.runMacro(screenMouseClickEvent.getButton());
        }
    }

    @EventHandler
    public void onKey(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() && ScreenUtil.allowsMovement(screenKeyEvent.getScreen())) {
            this.runMacro(screenKeyEvent.getKey());
        }
    }

    private void runMacro(int i) {
        String string = this.macros.get(i);
        if (string != null) {
            ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
            if (clientPlayNetworkHandler != null && mc.player != null) {
                if (string.startsWith("/")) {
                    clientPlayNetworkHandler.sendChatCommand(string.substring(1));
                } else {
                    clientPlayNetworkHandler.sendChatMessage(string);
                }
            }
        }
    }

    public void save() {
        try {
            JsonArray jsonArray = new JsonArray();

            for (Entry entry : this.macros.entrySet()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("key", (Number)entry.getKey());
                jsonObject.addProperty("message", (String)entry.getValue());
                jsonArray.add(jsonObject);
            }

            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.add("macros", jsonArray);
            Files.writeString(this.file, DataEncryption.encryptString(GSON.toJson(jsonObject2)), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save macros", exception);
        }
    }

    public void load() {
        if (Files.exists(this.file)) {
            try {
                String string = Files.readString(this.file, StandardCharsets.UTF_8);

                String string2;
                try {
                    string2 = DataEncryption.decryptString(string);
                } catch (Exception exception) {
                    string2 = string;
                }

                JsonObject jsonObject = GSON.fromJson(string2, JsonObject.class);
                if (jsonObject == null || !jsonObject.has("macros")) {
                    return;
                }

                this.macros.clear();

                for (JsonElement jsonElement : jsonObject.getAsJsonArray("macros")) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    if (jsonObject2.has("key") && jsonObject2.has("message")) {
                        this.macros.put(jsonObject2.get("key").getAsInt(), jsonObject2.get("message").getAsString());
                    }
                }
            } catch (Exception exception2) {
                LOGGER.error("Failed to load macros", exception2);
            }
        }
    }
}
