package tech.sadnes.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.cloud.CloudConfigSync;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureBind;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.network.packet.ConfigKind;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptModule;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ColorToggleSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.GradientColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.RangeSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.setting.ValueSetting;
import tech.sadnes.client.setting.WaveCurve;
import tech.sadnes.client.setting.WaveSetting;
import tech.sadnes.client.setting.curve.BezierCurve;
import tech.sadnes.client.setting.curve.CurveSetting;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.util.ModVersion;
import tech.sadnes.client.util.math.StopWatch;
import ua.mintantileak.profile.Profile;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConfigManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final ConfigStorage storage;
    private final SadnesSocketClient cloudClient;
    private final CloudConfigSync cloudSync;
    private volatile Set<String> favoriteConfigs = new LinkedHashSet<>(FavoritesPrefs.getFavoriteConfigs());
    private final List<ConfigEntry> entries = new ArrayList<>();
    private String activeConfig = "default";
    private JsonObject defaultSnapshot;
    private final StopWatch autoSaveTimer = new StopWatch();
    private volatile boolean dirty;
    private volatile boolean applying;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sadnes-config-io");
        thread.setDaemon(true);
        return thread;
    });
    private final Path activeFile = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("active");

    public ConfigManager(ConfigStorage configStorage, SadnesSocketClient sadnesSocketClient) {
        this.storage = configStorage;
        this.cloudClient = sadnesSocketClient;
        this.cloudSync = new CloudConfigSync(ConfigKind.CLIENT, sadnesSocketClient, this::exportJson, this::importCloudConfig);
        this.refresh();
    }

    public void captureDefaults() {
        if (this.defaultSnapshot == null) {
            try {
                this.defaultSnapshot = this.serializeAll();
            } catch (Exception exception) {
                LOGGER.error("Failed to capture default config snapshot", exception);
            }
        }
    }

    public boolean saveDefault() {
        return this.save("default");
    }

    public boolean save(String string) {
        this.activeConfig = string;
        JsonObject jsonObject = this.buildConfigJson(string);
        boolean flag = this.writeConfig(string, jsonObject, true);
        if (flag) {
            this.writeActiveName(string);
        }

        return flag;
    }

    public boolean saveActive() {
        String string = this.activeConfig;
        return this.save(string);
    }

    public void markDirty() {
        if (!this.applying) {
            this.dirty = true;
        }
    }

    public boolean isApplying() {
        return this.applying;
    }

    public void tickAutoSave() {
        if (this.dirty && this.autoSaveTimer.hasPassed(30000.0)) {
            this.dirty = false;
            this.autoSaveTimer.reset();
            this.writeConfig(this.activeConfig, this.buildConfigJson(this.activeConfig), false);
        }
    }

    private JsonObject buildConfigJson(String string) {
        ConfigEntry configEntry = this.getOrCreateEntry(string);
        long l = System.currentTimeMillis();
        if (configEntry.getCreatedAt() == 0L) {
            configEntry.setCreatedAt(l);
        }

        if (configEntry.getOwner() == null || configEntry.getOwner().isEmpty()) {
            configEntry.setOwner(Profile.getUsername());
            configEntry.setOwnerAvatar(Profile.getAvatarUrl());
        }

        configEntry.setModifiedAt(l);
        configEntry.setBuild(ModVersion.getVersion());
        return this.serializeWithMetadata(configEntry);
    }

    private boolean writeConfig(String string, JsonObject jsonObject, boolean flag) {
        Future future = this.ioExecutor.submit(() -> this.storage.write(string, GSON.toJson(jsonObject)));
        if (!flag) {
            return true;
        }

        try {
            return (Boolean)future.get();
        } catch (Exception exception) {
            LOGGER.error("Failed to write config '{}'", string, exception);
            return false;
        }
    }

    public void shutdown() {
        this.ioExecutor.shutdown();

        try {
            if (!this.ioExecutor.awaitTermination(3L, TimeUnit.SECONDS)) {
                this.ioExecutor.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            this.ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean loadDefault() {
        return this.load("default");
    }

    public boolean load(String string) {
        String string2 = this.storage.read(string);
        if (string2 == null) {
            return false;
        }

        try {
            JsonObject jsonObject = GSON.fromJson(string2, JsonObject.class);
            if (jsonObject == null) {
                return false;
            }

            this.applyJson(this.mergeWithDefaults(jsonObject));
            ConfigEntry configEntry = this.getEntry(string);
            if (configEntry != null) {
                this.readMetadata(configEntry, jsonObject);
            }

            this.activeConfig = string;
            this.writeActiveName(string);
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to load config '{}'", string, exception);
            return false;
        }
    }

    public boolean resetToDefaults() {
        if (this.defaultSnapshot == null) {
            return false;
        }

        this.applyJson(this.defaultSnapshot.deepCopy());
        this.activeConfig = "default";
        boolean flag = this.writeConfig("default", this.buildConfigJson("default"), true);
        if (flag) {
            this.writeActiveName("default");
        }

        return flag;
    }

    public void loadLastActive() {
        String string = this.readActiveName();
        if (string != null && this.exists(string)) {
            this.load(string);
        }
    }

    public String exportJson() {
        return GSON.toJson(this.serializeAll());
    }

    public boolean importCloudConfig(String string) {
        if (string == null) {
            return false;
        }

        try {
            JsonObject jsonObject = GSON.fromJson(string, JsonObject.class);
            if (jsonObject == null) {
                return false;
            }

            this.applyJson(this.mergeWithDefaults(jsonObject));
            this.storage.write("default", string);
            this.activeConfig = "default";
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to apply cloud config", exception);
            return false;
        }
    }

    public CloudConfigSync getCloudSync() {
        return this.cloudSync;
    }

    public Set<String> getFavoriteConfigs() {
        return this.favoriteConfigs;
    }

    public boolean isFavorite(String string) {
        return this.favoriteConfigs.contains(string);
    }

    public CompletableFuture<Void> syncFavorites() {
        return this.cloudClient.requestList().thenAccept(strings -> {
            LinkedHashSet linkedHashSet = new LinkedHashSet<>((Collection<? extends String>)strings);

            for (String string : FavoritesPrefs.getFavoriteConfigs()) {
                if (linkedHashSet.add(string)) {
                    this.cloudClient.requestAction1345(string, true);
                }
            }

            this.favoriteConfigs = linkedHashSet;
            FavoritesPrefs.setFavoriteConfigs(linkedHashSet);
        });
    }

    public CompletableFuture<Void> toggleFavorite(String string) {
        boolean flag = !this.favoriteConfigs.contains(string);
        LinkedHashSet linkedHashSet = new LinkedHashSet<>(this.favoriteConfigs);
        if (flag) {
            linkedHashSet.add(string);
        } else {
            linkedHashSet.remove(string);
        }

        this.favoriteConfigs = linkedHashSet;
        FavoritesPrefs.setFavoriteConfigs(linkedHashSet);
        return this.cloudClient.requestAction1345(string, flag);
    }

    public boolean delete(String string) {
        boolean flag = this.storage.delete(string);
        if (flag) {
            this.entries.removeIf(configEntry -> configEntry.getName().equals(string));
            if (string.equals(this.activeConfig)) {
                this.activeConfig = "default";
                this.writeActiveName(this.activeConfig);
            }
        }

        return flag;
    }

    public List<String> listConfigs() {
        return this.storage.list();
    }

    public boolean exists(String string) {
        return this.storage.exists(string);
    }

    public Path getDirectory() {
        return this.storage.getDirectory();
    }

    public void refresh() {
        List<String> strings = this.storage.list();
        this.entries.removeIf(configEntry -> !strings.contains(configEntry.getName()));

        for (String string : strings) {
            ConfigEntry configEntry2 = this.getEntry(string);
            if (configEntry2 == null) {
                configEntry2 = new ConfigEntry(string, this);
                this.entries.add(configEntry2);
            }

            this.readMetadata(configEntry2, null);
        }

        this.entries.sort(Comparator.comparing(ConfigEntry::getName));
    }

    private ConfigEntry getOrCreateEntry(String string) {
        ConfigEntry configEntry = this.getEntry(string);
        if (configEntry == null) {
            configEntry = new ConfigEntry(string, this);
            this.entries.add(configEntry);
            this.entries.sort(Comparator.comparing(ConfigEntry::getName));
        }

        return configEntry;
    }

    private void readMetadata(ConfigEntry configEntry, JsonObject jsonObject) {
        try {
            if (jsonObject == null) {
                String string = this.storage.read(configEntry.getName());
                if (string == null) {
                    return;
                }

                jsonObject = GSON.fromJson(string, JsonObject.class);
            }

            if (jsonObject == null) {
                return;
            }

            JsonObject jsonObject2 = jsonObject.getAsJsonObject(ClientInfo.MOD_ID);
            if (jsonObject2 == null) {
                return;
            }

            Icon icon = jsonObject2.has("icon") ? parseIcon(jsonObject2.get("icon").getAsString()) : null;
            int i = jsonObject2.has("color") ? jsonObject2.get("color").getAsInt() : 0;
            String string2 = jsonObject2.has("note") ? jsonObject2.get("note").getAsString() : "";
            long l = jsonObject2.has("createdAt") ? jsonObject2.get("createdAt").getAsLong() : 0L;
            long l2 = jsonObject2.has("modifiedAt") ? jsonObject2.get("modifiedAt").getAsLong() : 0L;
            String string3 = jsonObject2.has("build") ? jsonObject2.get("build").getAsString() : "";
            String string4 = jsonObject2.has("owner") ? jsonObject2.get("owner").getAsString() : "";
            String string5 = jsonObject2.has("ownerAvatar") ? jsonObject2.get("ownerAvatar").getAsString() : "";
            configEntry.applyMetadata(icon, i, string2, l, l2, string3, string4, string5);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read metadata for config '{}': {}", configEntry.getName(), exception.getMessage());
        }
    }

    private static Icon parseIcon(String string) {
        try {
            return Icon.valueOf(string);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    public ConfigEntry getEntry(String string) {
        for (ConfigEntry configEntry : this.entries) {
            if (configEntry.getName().equals(string)) {
                return configEntry;
            }
        }

        return null;
    }

    public List<ConfigEntry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    public String getActiveConfig() {
        return this.activeConfig;
    }

    private String readActiveName() {
        try {
            if (!Files.exists(this.activeFile)) {
                return null;
            }

            String string = Files.readString(this.activeFile, StandardCharsets.UTF_8).trim();
            return string.isEmpty() ? null : string;
        } catch (Exception exception) {
            return null;
        }
    }

    private void writeActiveName(String string) {
        try {
            Files.createDirectories(this.activeFile.getParent());
            Files.writeString(this.activeFile, string, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to persist active config name: {}", exception.getMessage());
        }
    }

    private JsonObject mergeWithDefaults(JsonObject jsonObject) {
        if (this.defaultSnapshot == null) {
            return jsonObject;
        }

        JsonObject jsonObject2 = this.defaultSnapshot.deepCopy();
        deepMerge(jsonObject2, jsonObject);
        return jsonObject2;
    }

    private static void deepMerge(JsonObject jsonObject, JsonObject jsonObject2) {
        for (Entry entry : jsonObject2.entrySet()) {
            JsonElement jsonElement = (JsonElement)entry.getValue();
            JsonElement jsonElement2 = jsonObject.get((String)entry.getKey());
            if (jsonElement.isJsonObject() && jsonElement2 != null && jsonElement2.isJsonObject()) {
                deepMerge(jsonElement2.getAsJsonObject(), jsonElement.getAsJsonObject());
            } else {
                jsonObject.add((String)entry.getKey(), jsonElement);
            }
        }
    }

    private JsonObject serializeAll() {
        JsonObject jsonObject = new JsonObject();
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("version", "1.21.11");
        jsonObject2.addProperty("build", ModVersion.getVersion());
        jsonObject.add(ClientInfo.MOD_ID, jsonObject2);
        JsonObject jsonObject3 = new JsonObject();

        for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
            jsonObject3.add(this.getFeatureKey(feature), this.serializeFeature(feature));
        }

        jsonObject.add("features", jsonObject3);
        jsonObject.add("drags", this.serializeDrags());
        jsonObject.add("scripts", this.serializeScripts());
        return jsonObject;
    }

    private JsonObject serializeScripts() {
        JsonObject jsonObject = new JsonObject();
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager == null) {
            return jsonObject;
        }

        for (Script script : scriptManager.getScripts()) {
            jsonObject.add(script.getDisplayName(), this.serializeScript(script));
        }

        return jsonObject;
    }

    public JsonObject serializeScript(Script script) {
        JsonObject jsonObject = script.getSavedConfig();

        for (Setting setting : script.getSettings()) {
            this.serializeSetting(jsonObject, setting);
        }

        JsonObject jsonObject2 = new JsonObject();

        for (ScriptModule scriptModule : script.getModules()) {
            JsonObject jsonObject3 = new JsonObject();

            for (Setting setting2 : scriptModule.getSettings()) {
                this.serializeSetting(jsonObject3, setting2);
            }

            JsonObject jsonObject4 = new JsonObject();

            for (ScriptFeature scriptFeature : scriptModule.getHudElements()) {
                JsonObject jsonObject5 = new JsonObject();

                for (Setting setting3 : scriptFeature.getSettings()) {
                    this.serializeSetting(jsonObject5, setting3);
                }

                jsonObject4.add(scriptFeature.getDisplayName(), jsonObject5);
            }

            if (!jsonObject4.isEmpty()) {
                jsonObject3.add("$features", jsonObject4);
            }

            jsonObject2.add(scriptModule.getDisplayName(), jsonObject3);
        }

        if (!jsonObject2.isEmpty()) {
            jsonObject.add("$modules", jsonObject2);
        }

        return jsonObject;
    }

    private JsonObject serializeWithMetadata(ConfigEntry configEntry) {
        JsonObject jsonObject = this.serializeAll();
        JsonObject jsonObject2 = jsonObject.getAsJsonObject(ClientInfo.MOD_ID);
        jsonObject2.addProperty("name", configEntry.getName());
        jsonObject2.addProperty("createdAt", configEntry.getCreatedAt());
        jsonObject2.addProperty("modifiedAt", configEntry.getModifiedAt());
        if (configEntry.getIcon() != null) {
            jsonObject2.addProperty("icon", configEntry.getIcon().name());
        }

        if (configEntry.getColor() != 0) {
            jsonObject2.addProperty("color", configEntry.getColor());
        }

        if (configEntry.getNote() != null && !configEntry.getNote().isEmpty()) {
            jsonObject2.addProperty("note", configEntry.getNote());
        }

        if (configEntry.getOwner() != null && !configEntry.getOwner().isEmpty()) {
            jsonObject2.addProperty("owner", configEntry.getOwner());
        }

        if (configEntry.getOwnerAvatar() != null && !configEntry.getOwnerAvatar().isEmpty()) {
            jsonObject2.addProperty("ownerAvatar", configEntry.getOwnerAvatar());
        }

        return jsonObject;
    }

    private JsonObject serializeDrags() {
        JsonObject jsonObject = new JsonObject();
        DragComponent dragComponent = DragComponent.getInstance();
        if (dragComponent == null) {
            return jsonObject;
        }

        for (DragSetting dragSetting : dragComponent.getDragHandler().getElements()) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("x", dragSetting.position2.x);
            jsonObject2.addProperty("y", dragSetting.position2.y);
            if (dragSetting.isAnchored() && dragSetting.getAnchorTarget() != null) {
                JsonObject jsonObject3 = new JsonObject();
                jsonObject3.addProperty("parent", dragSetting.getAnchorTarget().getName());
                jsonObject3.addProperty("pEx", dragSetting.getAnchorTargetX());
                jsonObject3.addProperty("sEx", dragSetting.getAnchorSelfX());
                jsonObject3.addProperty("ox", dragSetting.getAnchorOffsetX());
                jsonObject3.addProperty("pEy", dragSetting.getAnchorTargetY());
                jsonObject3.addProperty("sEy", dragSetting.getAnchorSelfY());
                jsonObject3.addProperty("oy", dragSetting.getAnchorOffsetY());
                jsonObject2.add("link", jsonObject3);
            }

            if (dragSetting.isScreenAnchored()) {
                JsonObject jsonObject4 = new JsonObject();
                jsonObject4.addProperty("lx", dragSetting.getScreenGridX());
                jsonObject4.addProperty("sx", dragSetting.getScreenSelfAnchorX());
                jsonObject4.addProperty("ly", dragSetting.getScreenGridY());
                jsonObject4.addProperty("sy", dragSetting.getScreenSelfAnchorY());
                jsonObject2.add("guide", jsonObject4);
            }

            jsonObject.add(dragSetting.getName(), jsonObject2);
        }

        return jsonObject;
    }

    private JsonObject serializeFeature(Feature feature) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("state", feature.isEnabled());
        JsonArray jsonArray = new JsonArray();

        for (FeatureBind featureBind : feature.getBinds()) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("key", featureBind.getKey());
            jsonObject2.addProperty("type", featureBind.getMode().name());
            if (!featureBind.isVisible()) {
                jsonObject2.addProperty("visible", false);
            }

            this.writeModifiers(jsonObject2, featureBind.getModifiers());
            jsonArray.add(jsonObject2);
        }

        jsonObject.add("binds", jsonArray);

        for (Setting setting : feature.getSettings()) {
            this.serializeSetting(jsonObject, setting);
        }

        return jsonObject;
    }

    private void serializeSetting(JsonObject jsonObject, Setting<?> setting) {
        String string = setting.getDisplayName();
        if (setting instanceof BooleanSetting booleanSetting) {
            jsonObject.addProperty(string, booleanSetting.getDefaultValue());
        } else if (setting instanceof NumberSetting numberSetting) {
            jsonObject.addProperty(string, numberSetting.getDefaultValue());
        } else if (setting instanceof RangeSetting rangeSetting) {
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(rangeSetting.getDefaultValue()[0]);
            jsonArray.add(rangeSetting.getDefaultValue()[1]);
            jsonObject.add(string, jsonArray);
        } else if (setting instanceof ModeSetting modeSetting) {
            jsonObject.addProperty(string, modeSetting.getDefaultValue());
        } else if (setting instanceof KeybindSetting keybindSetting) {
            jsonObject.addProperty(string, keybindSetting.getDefaultValue());
        } else if (setting instanceof ColorToggleSetting colorToggleSetting) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("state", colorToggleSetting.isEnabled());
            jsonObject2.addProperty("color", colorToggleSetting.getDefaultValue());
            jsonObject.add(string, jsonObject2);
        } else if (setting instanceof GradientColorSetting gradientColorSetting) {
            JsonArray jsonArray2 = new JsonArray();
            jsonArray2.add(gradientColorSetting.getFirstColor());
            jsonArray2.add(gradientColorSetting.getSecondColor());
            jsonObject.add(string, jsonArray2);
        } else if (setting instanceof ColorSetting colorSetting) {
            jsonObject.addProperty(string, colorSetting.getDefaultValue());
        } else if (setting instanceof TextSetting textSetting) {
            jsonObject.addProperty(string, textSetting.getDefaultValue());
        } else if (setting instanceof MultiSetting multiSetting) {
            JsonObject jsonObject3 = new JsonObject();

            for (MultiSetting.Entry entry : multiSetting.getEntries()) {
                this.serializeSetting(jsonObject3, entry.value());
            }

            jsonObject.add(string, jsonObject3);
        } else if (setting instanceof GroupSetting groupSetting) {
            JsonObject jsonObject4 = new JsonObject();
            jsonObject4.addProperty("$state", groupSetting.getDefaultValue());

            for (Setting setting2 : groupSetting.getChildList()) {
                this.serializeSetting(jsonObject4, setting2);
            }

            jsonObject.add(string, jsonObject4);
        } else if (setting instanceof DragSetting dragSetting) {
            JsonObject jsonObject5 = new JsonObject();
            jsonObject5.addProperty("x", dragSetting.position2.x);
            jsonObject5.addProperty("y", dragSetting.position2.y);
            jsonObject.add(string, jsonObject5);
        } else if (setting instanceof CurveSetting curveSetting) {
            JsonArray jsonArray3 = new JsonArray();

            for (float f : curveSetting.getValue().toArray()) {
                jsonArray3.add(f);
            }

            jsonObject.add(string, jsonArray3);
        } else if (setting instanceof WaveSetting waveSetting) {
            JsonArray jsonArray4 = new JsonArray();

            for (float f2 : waveSetting.getValue().toArray()) {
                jsonArray4.add(f2);
            }

            jsonObject.add(string, jsonArray4);
        }

        if (setting instanceof ValueSetting valueSetting) {
            JsonArray jsonArray5 = new JsonArray();

            for (ValueBind valueBind : (List<ValueBind>)valueSetting.getBinds()) {
                JsonObject jsonObject6 = new JsonObject();
                jsonObject6.addProperty("key", valueBind.getKey());
                jsonObject6.addProperty("type", valueBind.getMode().name());
                jsonObject6.addProperty("visible", valueBind.isVisible());
                this.writeModifiers(jsonObject6, valueBind.getModifiers());
                jsonObject6.add("value", this.serializeBindValue(setting, valueBind.getValue()));
                if (valueSetting.isToggleBindActive(valueBind)) {
                    jsonObject6.addProperty("active", true);
                }

                jsonArray5.add(jsonObject6);
            }

            jsonObject.add(string + "$binds", jsonArray5);
        }
    }

    private void writeModifiers(JsonObject jsonObject, int[] ints) {
        if (ints != null && ints.length != 0) {
            JsonArray jsonArray = new JsonArray();

            for (int i : ints) {
                jsonArray.add(i);
            }

            jsonObject.add("modifiers", jsonArray);
        }
    }

    private int[] readModifiers(JsonObject jsonObject) {
        if (jsonObject != null && jsonObject.has("modifiers") && jsonObject.get("modifiers").isJsonArray()) {
            JsonArray jsonArray = jsonObject.getAsJsonArray("modifiers");
            int[] ints = new int[jsonArray.size()];

            for (int i = 0; i < jsonArray.size(); i++) {
                ints[i] = jsonArray.get(i).getAsInt();
            }

            return ints;
        } else {
            return FeatureBind.NO_MODIFIERS;
        }
    }

    private JsonElement serializeBindValue(Setting<?> setting, Object object) {
        if (object == null) {
            return JsonNull.INSTANCE;
        }

        if (setting instanceof BooleanSetting) {
            return new JsonPrimitive((Boolean)object);
        }

        if (setting instanceof NumberSetting) {
            return new JsonPrimitive((Float)object);
        }

        if (setting instanceof ModeSetting) {
            return new JsonPrimitive((String)object);
        }

        if (setting instanceof KeybindSetting) {
            return new JsonPrimitive((Integer)object);
        }

        if (setting instanceof ColorSetting) {
            return new JsonPrimitive((Integer)object);
        }

        JsonArray jsonArray;
        if (setting instanceof MultiSetting) {
            jsonArray = new JsonArray();

            for (String string : (List<String>)object) {
                jsonArray.add(string);
            }
        } else {
            if (!(setting instanceof RangeSetting)) {
                if (setting instanceof GroupSetting) {
                    return new JsonPrimitive((Boolean)object);
                }

                return new JsonPrimitive(String.valueOf(object));
            }

            jsonArray = new JsonArray();

            for (float f : (float[])object) {
                jsonArray.add(f);
            }
        }

        return jsonArray;
    }

    private void applyJson(JsonObject jsonObject) {
        JsonObject jsonObject2 = jsonObject.getAsJsonObject("features");
        if (jsonObject2 != null) {
            this.applying = true;
            boolean flag = Feature.isInitialized();
            Feature.setInitialized(false);

            try {
                for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
                    JsonObject jsonObject3 = this.findFeatureObject(jsonObject2, feature);
                    if (jsonObject3 != null) {
                        this.applyFeature(feature, jsonObject3);
                    }
                }

                this.applyDrags(jsonObject.getAsJsonObject("drags"));
                this.applyScripts(jsonObject.getAsJsonObject("scripts"));
            } finally {
                Feature.setInitialized(flag);
                this.applying = false;
                this.dirty = false;
            }
        }
    }

    private void applyScripts(JsonObject jsonObject) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null && jsonObject != null) {
            for (Script script : scriptManager.getScripts()) {
                JsonObject jsonObject2 = jsonObject.getAsJsonObject(script.getDisplayName());
                if (jsonObject2 != null) {
                    script.setSavedConfig(jsonObject2);
                    this.applyScript(script, jsonObject2);
                    this.applySetting(jsonObject2, script.getSettings().getFirst());
                }
            }
        }
    }

    public void applyScript(Script script, JsonObject jsonObject) {
        for (Setting setting : List.copyOf(script.getSettings())) {
            if (!"$enabled".equals(setting.getDisplayName())) {
                this.applySetting(jsonObject, setting);
            }
        }

        JsonObject jsonObject2 = jsonObject.getAsJsonObject("$modules");
        if (jsonObject2 != null) {
            for (ScriptModule scriptModule : script.getModules()) {
                JsonObject jsonObject3 = jsonObject2.getAsJsonObject(scriptModule.getDisplayName());
                if (jsonObject3 != null) {
                    for (Setting setting2 : scriptModule.getSettings()) {
                        this.applySetting(jsonObject3, setting2);
                    }

                    JsonObject jsonObject4 = jsonObject3.getAsJsonObject("$features");
                    if (jsonObject4 != null) {
                        for (ScriptFeature scriptFeature : scriptModule.getHudElements()) {
                            JsonObject jsonObject5 = jsonObject4.getAsJsonObject(scriptFeature.getDisplayName());
                            if (jsonObject5 != null) {
                                for (Setting setting3 : scriptFeature.getSettings()) {
                                    this.applySetting(jsonObject5, setting3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyDrags(JsonObject jsonObject) {
        DragComponent dragComponent = DragComponent.getInstance();
        if (dragComponent != null && jsonObject != null) {
            List<DragSetting> dragSettings = dragComponent.getDragHandler().getElements();

            for (DragSetting dragSetting : dragSettings) {
                dragSetting.clearAnchor();
                dragSetting.clearScreenAnchor();
            }

            for (DragSetting dragSetting2 : dragSettings) {
                JsonObject jsonObject2 = jsonObject.getAsJsonObject(dragSetting2.getName());
                if (jsonObject2 != null) {
                    this.applyDragPosition(dragSetting2, jsonObject2);
                }
            }

            for (DragSetting dragSetting3 : dragSettings) {
                JsonObject jsonObject3 = jsonObject.getAsJsonObject(dragSetting3.getName());
                if (jsonObject3 != null && jsonObject3.has("link")) {
                    JsonObject jsonObject4 = jsonObject3.getAsJsonObject("link");
                    if (jsonObject4 != null && jsonObject4.has("parent")) {
                        DragSetting dragSetting4 = this.findDrag(dragSettings, jsonObject4.get("parent").getAsString());
                        if (dragSetting4 != null && dragSetting4 != dragSetting3) {
                            int i = jsonObject4.has("pEx") ? jsonObject4.get("pEx").getAsInt() : -1;
                            int i2 = jsonObject4.has("sEx") ? jsonObject4.get("sEx").getAsInt() : -1;
                            float f = jsonObject4.has("ox") ? jsonObject4.get("ox").getAsFloat() : 0.0F;
                            int i3 = jsonObject4.has("pEy") ? jsonObject4.get("pEy").getAsInt() : -1;
                            int i4 = jsonObject4.has("sEy") ? jsonObject4.get("sEy").getAsInt() : -1;
                            float f2 = jsonObject4.has("oy") ? jsonObject4.get("oy").getAsFloat() : 0.0F;
                            if (!this.wouldCreateLinkCycle(dragSetting3, dragSetting4)) {
                                dragSetting3.setAnchor(dragSetting4, i, i2, f, i3, i4, f2);
                            }
                        }
                    }
                }
            }

            for (DragSetting dragSetting5 : dragSettings) {
                JsonObject jsonObject5 = jsonObject.getAsJsonObject(dragSetting5.getName());
                if (jsonObject5 != null && jsonObject5.has("guide")) {
                    JsonObject jsonObject6 = jsonObject5.getAsJsonObject("guide");
                    if (jsonObject6 != null) {
                        int i5 = jsonObject6.has("lx") ? jsonObject6.get("lx").getAsInt() : -1;
                        int i6 = jsonObject6.has("sx") ? jsonObject6.get("sx").getAsInt() : -1;
                        int i7 = jsonObject6.has("ly") ? jsonObject6.get("ly").getAsInt() : -1;
                        int i8 = jsonObject6.has("sy") ? jsonObject6.get("sy").getAsInt() : -1;
                        dragSetting5.setScreenAnchor(i5, i6, i7, i8);
                    }
                }
            }
        }
    }

    private DragSetting findDrag(List<DragSetting> dragSettings, String string) {
        for (DragSetting dragSetting : dragSettings) {
            if (dragSetting.getName().equals(string)) {
                return dragSetting;
            }
        }

        return null;
    }

    private boolean wouldCreateLinkCycle(DragSetting dragSetting, DragSetting dragSetting2) {
        DragSetting dragSetting3 = dragSetting2;

        for (int i = 0; dragSetting3 != null && i < 16; i++) {
            if (dragSetting3 == dragSetting) {
                return true;
            }

            dragSetting3 = dragSetting3.getAnchorTarget();
        }

        return false;
    }

    private void applyFeature(Feature feature, JsonObject jsonObject) {
        feature.setEnabled(false, true);
        if (jsonObject.has("binds")) {
            feature.clearBinds();
            JsonArray jsonArray = jsonObject.getAsJsonArray("binds");
            if (jsonArray != null) {
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    int i = jsonObject2.get("key").getAsInt();
                    BindMode bindMode = BindMode.valueOf(jsonObject2.get("type").getAsString());
                    boolean flag;
                    if (jsonObject2.has("visible")) {
                        flag = jsonObject2.get("visible").getAsBoolean();
                    } else {
                        flag = true;
                    }

                    FeatureBind featureBind = new FeatureBind(i, bindMode, flag);
                    featureBind.setModifiers(this.readModifiers(jsonObject2));
                    feature.addBind(featureBind);
                }
            }
        }

        for (Setting setting : feature.getSettings()) {
            this.applySetting(jsonObject, setting);
        }

        if (jsonObject.has("state") && jsonObject.get("state").getAsBoolean()) {
            feature.setEnabled(true, false);
        }
    }

    private void applySetting(JsonObject jsonObject, Setting<?> setting) {
        String string = setting.getDisplayName();
        JsonElement jsonElement = jsonObject.get(string);
        if (jsonElement == null || jsonElement.isJsonNull()) {
            jsonElement = this.findSettingElement(jsonObject, setting);
        }

        if (jsonElement != null && !jsonElement.isJsonNull()) {
            try {
                if (setting instanceof BooleanSetting booleanSetting) {
                    booleanSetting.setValue(jsonElement.getAsBoolean());
                } else if (setting instanceof NumberSetting numberSetting) {
                    numberSetting.setValue(Float.valueOf(jsonElement.getAsFloat()));
                } else if (setting instanceof RangeSetting rangeSetting && jsonElement.isJsonArray()) {
                    JsonArray jsonArray = jsonElement.getAsJsonArray();
                    if (jsonArray.size() >= 2) {
                        rangeSetting.setValue(new float[]{jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat()});
                    }
                } else if (setting instanceof ModeSetting modeSetting) {
                    String string2 = jsonElement.getAsString();
                    if (modeSetting.getModes().contains(string2)) {
                        modeSetting.setValue(string2);
                    }
                } else if (setting instanceof KeybindSetting keybindSetting) {
                    keybindSetting.setValue(Integer.valueOf(jsonElement.getAsInt()));
                } else if (setting instanceof ColorToggleSetting colorToggleSetting) {
                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                        if (jsonObject2.has("color")) {
                            colorToggleSetting.setValue(Integer.valueOf(jsonObject2.get("color").getAsInt()));
                        }

                        if (jsonObject2.has("state")) {
                            colorToggleSetting.setEnabled(jsonObject2.get("state").getAsBoolean());
                        }
                    } else if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isBoolean()) {
                        colorToggleSetting.setEnabled(jsonElement.getAsBoolean());
                    }
                } else if (setting instanceof GradientColorSetting gradientColorSetting) {
                    if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() >= 2) {
                        JsonArray jsonArray2 = jsonElement.getAsJsonArray();
                        gradientColorSetting.setColors(jsonArray2.get(0).getAsInt(), jsonArray2.get(1).getAsInt());
                    } else if (jsonElement.isJsonPrimitive()) {
                        gradientColorSetting.setColors(jsonElement.getAsInt(), gradientColorSetting.getSecondColor());
                    }
                } else if (setting instanceof ColorSetting colorSetting) {
                    colorSetting.setValue(Integer.valueOf(jsonElement.getAsInt()));
                } else if (setting instanceof TextSetting textSetting) {
                    textSetting.setValue(jsonElement.getAsString());
                } else if (setting instanceof MultiSetting multiSetting && jsonElement.isJsonObject()) {
                    JsonObject jsonObject3 = jsonElement.getAsJsonObject();

                    for (MultiSetting.Entry entry : multiSetting.getEntries()) {
                        String string3 = entry.value().getName();
                        JsonElement jsonElement2 = jsonObject3.get(string3);
                        if (jsonElement2 != null && !jsonElement2.isJsonNull()) {
                            multiSetting.setSelected(string3, jsonElement2.getAsBoolean());
                        }

                        this.applySettingBinds(jsonObject3, entry.value());
                    }
                } else if (setting instanceof GroupSetting groupSetting) {
                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject4 = jsonElement.getAsJsonObject();
                        if (jsonObject4.has("$state")) {
                            groupSetting.setValue(jsonObject4.get("$state").getAsBoolean());
                        }

                        for (Setting setting2 : groupSetting.getChildList()) {
                            this.applySetting(jsonObject4, setting2);
                        }
                    } else if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isBoolean()) {
                        groupSetting.setValue(jsonElement.getAsBoolean());

                        for (Setting setting3 : groupSetting.getChildList()) {
                            this.applySetting(jsonObject, setting3);
                        }
                    }
                } else if (setting instanceof DragSetting dragSetting && jsonElement.isJsonObject()) {
                    this.applyDragPosition(dragSetting, jsonElement.getAsJsonObject());
                } else if (setting instanceof CurveSetting curveSetting && jsonElement.isJsonArray()) {
                    JsonArray jsonArray3 = jsonElement.getAsJsonArray();
                    float[] floats = new float[jsonArray3.size()];

                    for (int i = 0; i < floats.length; i++) {
                        floats[i] = jsonArray3.get(i).getAsFloat();
                    }

                    curveSetting.setValue(BezierCurve.fromArray(floats));
                } else if (setting instanceof WaveSetting waveSetting && jsonElement.isJsonArray()) {
                    JsonArray jsonArray4 = jsonElement.getAsJsonArray();
                    float[] floats2 = new float[jsonArray4.size()];

                    for (int i2 = 0; i2 < floats2.length; i2++) {
                        floats2[i2] = jsonArray4.get(i2).getAsFloat();
                    }

                    waveSetting.setValue(WaveCurve.fromArray(floats2));
                }

                this.applySettingBinds(jsonObject, setting);
            } catch (Exception exception) {
                LOGGER.warn("Failed to deserialize setting '{}': {}", string, exception.getMessage());
            }
        }
    }

    private void applySettingBinds(JsonObject jsonObject, Setting<?> setting) {
        if (setting instanceof ValueSetting valueSetting) {
            JsonArray jsonArray = this.findBindsArray(jsonObject, setting);
            valueSetting.clearBinds();
            if (jsonArray == null) {
                return;
            }

            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                int i = jsonObject2.get("key").getAsInt();
                BindMode bindMode = BindMode.valueOf(jsonObject2.get("type").getAsString());
                boolean flag;
                if (jsonObject2.has("visible")) {
                    flag = jsonObject2.get("visible").getAsBoolean();
                } else {
                    flag = true;
                }

                Object object = this.deserializeBindValue(setting, jsonObject2.get("value"));
                ValueBind valueBind = new ValueBind<>(i, bindMode, object, flag);
                valueBind.setModifiers(this.readModifiers(jsonObject2));
                valueSetting.addBind(valueBind);
            }

            int i2 = 0;

            for (JsonElement jsonElement2 : jsonArray) {
                if (jsonElement2.getAsJsonObject().has("active") && jsonElement2.getAsJsonObject().get("active").getAsBoolean()) {
                    valueSetting.activateBind(i2);
                }

                i2++;
            }
        }
    }

    private JsonElement findSettingElement(JsonObject jsonObject, Setting<?> setting) {
        for (String string : setting.getAliases()) {
            JsonElement jsonElement = jsonObject.get(string);
            if (jsonElement != null && !jsonElement.isJsonNull()) {
                return jsonElement;
            }
        }

        return null;
    }

    private JsonArray findBindsArray(JsonObject jsonObject, Setting<?> setting) {
        JsonArray jsonArray = jsonObject.getAsJsonArray(setting.getDisplayName() + "$binds");
        if (jsonArray != null) {
            return jsonArray;
        }

        for (String string : setting.getAliases()) {
            JsonArray jsonArray2 = jsonObject.getAsJsonArray(string + "$binds");
            if (jsonArray2 != null) {
                return jsonArray2;
            }
        }

        return null;
    }

    private Object deserializeBindValue(Setting<?> setting, JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return null;
        }

        if (setting instanceof BooleanSetting) {
            return jsonElement.getAsBoolean();
        }

        if (setting instanceof NumberSetting) {
            return jsonElement.getAsFloat();
        }

        if (setting instanceof ModeSetting) {
            return jsonElement.getAsString();
        }

        if (setting instanceof KeybindSetting) {
            return jsonElement.getAsInt();
        }

        if (setting instanceof ColorSetting) {
            return jsonElement.getAsInt();
        }

        if (setting instanceof MultiSetting) {
            ArrayList arrayList = new ArrayList();

            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                arrayList.add(jsonElement2.getAsString());
            }

            return arrayList;
        } else if (setting instanceof RangeSetting) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            return jsonArray.size() < 2 ? null : new float[]{jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat()};
        } else {
            return setting instanceof GroupSetting ? jsonElement.getAsBoolean() : jsonElement.getAsString();
        }
    }

    private void applyDragPosition(DragSetting dragSetting, JsonObject jsonObject) {
        if (jsonObject.has("x") && jsonObject.has("y")) {
            dragSetting.setPosition(jsonObject.get("x").getAsFloat(), jsonObject.get("y").getAsFloat()).setPositionInitialized(true);
        }
    }

    private String getFeatureKey(Feature feature) {
        return feature.getDisplayName().replace(" ", "");
    }

    private JsonObject findFeatureObject(JsonObject jsonObject, Feature feature) {
        JsonObject jsonObject2 = jsonObject.getAsJsonObject(this.getFeatureKey(feature));

        for (String string : feature.getInfo().aliases()) {
            if (jsonObject2 != null) {
                return jsonObject2;
            }

            jsonObject2 = jsonObject.getAsJsonObject(string);
        }

        return jsonObject2;
    }
}
