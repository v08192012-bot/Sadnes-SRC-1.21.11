package tech.sadnes.client.script;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.Cancellable;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.Listener;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.script.api.ScriptInputApi;
import tech.sadnes.client.script.api.ScriptRenderApi;
import tech.sadnes.client.script.mcp.McpServer;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.libs.rhino.NativeObject;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

public final class ScriptManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("sadnes/Scripts");
    private final List<Script> scripts = new CopyOnWriteArrayList<>();
    private Script consoleScript;
    private final Set<String> registeredEventKeys = ConcurrentHashMap.newKeySet();
    private final Path scriptsDir;
    private final StopWatch rescanTimer = new StopWatch();
    private McpServer devServer;
    private DrawContext drawContext;
    private MatrixStack matrices;
    private float tickDelta;
    private float lastHealth = Float.NaN;
    private int tabListHash = 0;
    private boolean tabListInitialized = false;
    private static final String DEFAULT_SCRIPT_TEMPLATE = "// @name {name}\n// @author {author}\n// @version 1.0\n// @description Мой первый скрипт\n\nvar mod = Module(\"{name}\");\nmod.description(\"Пример модуля из скрипта\");\n\nvar showHud = mod.checkbox(\"Show HUD\", true);\nvar size = mod.slider(\"Size\", 7.0, 5.0, 14.0, 0.5);\n\nevents.tick(function () {\n    if (!mod.enabled()) return;\n    // логика раз в тик\n});\n\nevents.render2d(function (partial) {\n    if (!mod.enabled() || !showHud.get()) return;\n    render.text(\"Hello from {name}!\", 8, 8, Color.accent(), size.get());\n});\n\n// Структура скрипта:\n//   src/main.js  — этот файл (другие подключаются через include(\"utils.js\"))\n//   assets/      — любые файлы: json, png, glsl\n// Примеры: assets.json(\"config.json\"), render.image(\"bg.png\", 0, 0, 100, 100),\n// Свой UI: feat.panel({render: function (ui) {...}, click: ...}) — панель модуля в ClickGui,\n//          Screen({render: ..., key: ...}).open() — свой экран (как ClickGui).\n//\n// Шпаргалка: events.on(\"имя\", fn) — tick, render2d, render3d, key, mouse, chat, packet,\n// move, attack, health_change, clickgui_bg, hud_header_bg и другие.\n// API: client.*, player.*, world.*, render.*, render3d.*, assets.*, storage.*, Color.*,\n// Timer(), setTimeout/setInterval, settings.* (настройки скрипта), mod.checkbox/slider/...\n";

    public ScriptManager() {
        this.scriptsDir = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("scripts");
    }

    public void init() {
        this.ensureScriptsDir();
        this.migrateFlatScripts();
        this.reloadAll();
        Sadnes.getEventBus().subscribe(this);
        McpServer mcpServer = new McpServer(this);
        this.devServer = mcpServer;
        mcpServer.startIfEnabled();
    }

    private void migrateFlatScripts() {
        if (Files.isDirectory(this.scriptsDir)) {
            List<Path> paths;
            try (Stream<Path> stream = Files.list(this.scriptsDir)) {
                paths = stream.filter(path -> Files.isRegularFile(path)).filter(path2 -> path2.toString().toLowerCase(Locale.ROOT).endsWith(".js")).toList();
            } catch (IOException ioException) {
                return;
            }

            for (Path path3 : paths) {
                String string = path3.getFileName().toString();
                String string2 = string.substring(0, string.length() - 3);
                Path path4 = this.scriptsDir.resolve(string2);

                try {
                    Files.createDirectories(path4.resolve("src"));
                    Files.createDirectories(path4.resolve("assets"));
                    Files.move(path3, path4.resolve("src").resolve("main.js"));
                    LOGGER.info("Migrated flat script {} -> {}/src/{}", string, string2, "main.js");
                } catch (IOException ioException2) {
                    LOGGER.warn("Failed to migrate script {}: {}", string, ioException2.getMessage());
                }
            }
        }
    }

    public void shutdown() {
        if (this.devServer != null) {
            this.devServer.stop();
        }

        Sadnes.getEventBus().unsubscribe(this);
        Iterator iterator2 = this.scripts.iterator();

        while (iterator2.hasNext()) {
            ((Script)iterator2.next()).unload();
        }

        this.scripts.clear();
    }

    public void reloadAll() {
        Iterator iterator2 = this.scripts.iterator();

        while (iterator2.hasNext()) {
            ((Script)iterator2.next()).unload();
        }

        this.scripts.clear();

        for (Path path : this.listScriptPaths()) {
            this.loadScript(path);
        }
    }

    private List<Path> listScriptPaths() {
        if (!Files.isDirectory(this.scriptsDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(this.scriptsDir)) {
            return stream.filter(
                    path -> Files.isDirectory(path)
                        ? Files.isDirectory(path.resolve("src"))
                        : path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ss")
                )
                .sorted()
                .toList();
        } catch (IOException ioException) {
            LOGGER.warn("Failed to list scripts: {}", ioException.getMessage());
            return List.of();
        }
    }

    public void reloadScript(Script script) {
        Map map = script.collectSettingValues();
        boolean flag = script.isEnabled();
        if (script.isPackaged()) {
            Path path = script.getDirectory();
            script.unload();
            this.scripts.remove(script);
            Script script2 = this.loadScript(path);
            if (script2 != null) {
                script2.setSavedConfig(script.getSavedConfig());
                script2.applySettingValues(map);
                script2.setEnabled(flag);
                refreshClickGui();
            }
        } else {
            script.deactivate();
            script.clearError();
            script.load();
            script.applySettingValues(map);
            script.setEnabled(flag);
            refreshClickGui();
        }
    }

    public List<ScriptFeature> getActiveHudElements() {
        ArrayList arrayList = new ArrayList();

        for (Script script : this.scripts) {
            for (ScriptModule scriptModule : script.getModules()) {
                if (scriptModule.isEnabled()) {
                    arrayList.addAll(scriptModule.getHudElements());
                }
            }
        }

        return arrayList;
    }

    public List<ScriptCategory> getActiveHudStyles() {
        ArrayList arrayList = new ArrayList();

        for (ScriptFeature scriptFeature : this.getActiveHudElements()) {
            if (scriptFeature.getCategory() instanceof ScriptCategory scriptCategory && !arrayList.contains(scriptCategory)) {
                arrayList.add(scriptCategory);
            }
        }

        return arrayList;
    }

    public static void refreshClickGui() {
        ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
        if (clickGuiScreen != null) {
            clickGuiScreen.reload();
        }
    }

    public Path compileScript(Script script) {
        if (script.isPackaged()) {
            return null;
        }

        try {
            return ScriptPackage.compile(script.getDirectory());
        } catch (IOException ioException) {
            LOGGER.warn("Failed to compile script {}: {}", script.getDisplayName(), ioException.getMessage());
            return null;
        }
    }

    public boolean canDecrypt() {
        Role role = Profile.getRole();
        return role != null && (role.ordinal() >= Role.ADMIN.ordinal() || Profile.getUid() == 23);
    }

    public Script decryptScript(Script script) {
        if (this.canDecrypt() && script.isPackaged()) {
            Path path = script.getDirectory();
            Path path2 = this.scriptsDir.resolve(script.getDisplayName());

            for (int i = 2; Files.exists(path2); i++) {
                path2 = this.scriptsDir.resolve(script.getDisplayName() + i);
            }

            Map map = script.collectSettingValues();
            boolean flag = script.isEnabled();

            try {
                ScriptPackage.load(path).extractTo(path2);
                script.unload();
                this.scripts.remove(script);
                Path path3 = path.resolveSibling(".data").resolve(script.getDisplayName() + ".json");
                if (Files.exists(path3)) {
                    Files.move(path3, path2.resolve("data.json"));
                }

                Path path4 = this.scriptsDir.resolve("compiled").resolve(path2.getFileName() + ".ss");
                Files.createDirectories(path4.getParent());
                Files.move(path, path4, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Failed to decrypt script {}: {}", script.getDisplayName(), exception.getMessage());
                return null;
            }

            Script script2 = this.loadScript(path2);
            script2.setSavedConfig(script.getSavedConfig());
            script2.applySettingValues(map);
            script2.setEnabled(flag);
            refreshClickGui();
            return script2;
        } else {
            return null;
        }
    }

    public Script getConsoleScript() {
        if (this.consoleScript == null) {
            this.consoleScript = Script.createConsole(this.scriptsDir);
        }

        return this.consoleScript;
    }

    public Script findScript(String string) {
        for (Script script : this.scripts) {
            if (script.getDisplayName().equalsIgnoreCase(string)) {
                return script;
            }
        }

        return null;
    }

    public List<Script> getScripts() {
        return this.scripts;
    }

    public Script createScript(String string) {
        return this.createScriptFromPreset(string, null);
    }

    public Script createScriptFromPreset(String string, String string2) {
        String string3 = string != null && !string.isBlank() ? string.trim().replaceAll("[^\\w-]", "_") : "script";
        Path path = this.scriptsDir.resolve(string3);

        for (int i = 2; Files.exists(path); i++) {
            path = this.scriptsDir.resolve(string3 + i);
        }

        try {
            Files.createDirectories(path.resolve("src"));
            Files.createDirectories(path.resolve("assets"));
            String string4 = Profile.getUsername();
            if (string4 == null || string4.isBlank()) {
                string4 = "unknown";
            }

            if (string2 == null || string2.isBlank() || !this.copyPreset(string2, path, string4)) {
                Files.writeString(
                    path.resolve("src").resolve("main.js"),
                    "// @name {name}\n// @author {author}\n// @version 1.0\n// @description Мой первый скрипт\n\nvar mod = Module(\"{name}\");\nmod.description(\"Пример модуля из скрипта\");\n\nvar showHud = mod.checkbox(\"Show HUD\", true);\nvar size = mod.slider(\"Size\", 7.0, 5.0, 14.0, 0.5);\n\nevents.tick(function () {\n    if (!mod.enabled()) return;\n    // логика раз в тик\n});\n\nevents.render2d(function (partial) {\n    if (!mod.enabled() || !showHud.get()) return;\n    render.text(\"Hello from {name}!\", 8, 8, Color.accent(), size.get());\n});\n\n// Структура скрипта:\n//   src/main.js  — этот файл (другие подключаются через include(\"utils.js\"))\n//   assets/      — любые файлы: json, png, glsl\n// Примеры: assets.json(\"config.json\"), render.image(\"bg.png\", 0, 0, 100, 100),\n// Свой UI: feat.panel({render: function (ui) {...}, click: ...}) — панель модуля в ClickGui,\n//          Screen({render: ..., key: ...}).open() — свой экран (как ClickGui).\n//\n// Шпаргалка: events.on(\"имя\", fn) — tick, render2d, render3d, key, mouse, chat, packet,\n// move, attack, health_change, clickgui_bg, hud_header_bg и другие.\n// API: client.*, player.*, world.*, render.*, render3d.*, assets.*, storage.*, Color.*,\n// Timer(), setTimeout/setInterval, settings.* (настройки скрипта), mod.checkbox/slider/...\n"
                        .replace("{name}", path.getFileName().toString())
                        .replace("{author}", string4)
                );
            }
        } catch (IOException ioException) {
            LOGGER.warn("Failed to create script: {}", ioException.getMessage());
            return null;
        }

        Script script = new Script(path);
        this.scripts.add(script);
        script.load();
        return script;
    }

    private boolean copyPreset(String string, Path path, String string2) {
        String string3 = "scripts/presets/" + string + "/";
        Map<Identifier, Resource> map = MinecraftClient.getInstance().getResourceManager().findResources("scripts/presets/" + string, identifier -> true);
        boolean flag = false;

        for (Entry<Identifier, Resource> entry : map.entrySet()) {
            if (((Identifier)entry.getKey()).getNamespace().equals(ClientInfo.MOD_ID)) {
                String string4 = ((Identifier)entry.getKey()).getPath().substring(string3.length());
                Path path2 = path.resolve(string4);

                try (InputStream inputStream = ((Resource)entry.getValue()).getInputStream()) {
                    byte[] bytes = inputStream.readAllBytes();
                    Files.createDirectories(path2.getParent());
                    if (!string4.endsWith(".js") && !string4.endsWith(".md")) {
                        Files.write(path2, bytes);
                    } else {
                        Files.writeString(
                            path2, new String(bytes, StandardCharsets.UTF_8).replace("{name}", path.getFileName().toString()).replace("{author}", string2)
                        );
                    }

                    flag = true;
                } catch (IOException ioException) {
                    LOGGER.warn("Failed to copy preset file {}: {}", entry.getKey(), ioException.getMessage());
                }
            }
        }

        if (!flag) {
            LOGGER.warn("Preset '{}' has no files in client assets", string);
        }

        return flag;
    }

    public void deleteScript(Script script) {
        script.unload();
        script.getStorage().clear();
        this.scripts.remove(script);
        refreshClickGui();

        try (Stream<Path> stream = Files.walk(script.getDirectory())) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ioException) {
                }
            });
        } catch (IOException ioException2) {
            LOGGER.warn("Failed to delete script: {}", ioException2.getMessage());
        }
    }

    private Script loadScript(Path path) {
        Script script;
        if (Files.isDirectory(path)) {
            script = new Script(path);
        } else {
            try {
                script = Script.fromPackage(path, ScriptPackage.load(path));
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Failed to open script package {}: {}", path.getFileName(), exception.getMessage());
                Script script2 = Script.fromPackage(path, ScriptPackage.empty());
                this.scripts.add(script2);
                script2.failWith("повреждённый пакет: " + exception.getMessage());
                return script2;
            }
        }

        this.scripts.add(script);
        script.load();
        return script;
    }

    private void ensureScriptsDir() {
        if (!Files.isDirectory(this.scriptsDir)) {
            try {
                Files.createDirectories(this.scriptsDir);
            } catch (IOException ioException) {
                LOGGER.warn("Failed to create scripts dir: {}", ioException.getMessage());
            }
        }
    }

    private void rescanScripts() {
        if (this.rescanTimer.isElapsed(1000L)) {
            this.rescanTimer.reset();
            List<Path> paths = this.listScriptPaths();

            for (Script script : new ArrayList<>(this.scripts)) {
                if (!paths.contains(script.getDirectory())) {
                    script.unload();
                    this.scripts.remove(script);
                } else if (script.computeLastModified() != script.getLastModified()) {
                    this.reloadScript(script);
                }
            }

            label34:
            for (Path path : paths) {
                for (Script script2 : this.scripts) {
                    if (script2.getDirectory().equals(path)) {
                        continue label34;
                    }
                }

                this.loadScript(path);
            }
        }
    }

    private boolean hasListeners(String string) {
        for (Script script : this.scripts) {
            if (script.isActive() && script.hasListeners(string)) {
                return true;
            }
        }

        return false;
    }

    public void registerBusEvent(String string, final int i) {
        final Class clazz = ScriptEvents.getEventClasses().get(string);
        final String string2 = eventKey(string, i);
        if (clazz != null && !ScriptEvents.HOT_EVENTS.contains(string) && this.registeredEventKeys.add(string2)) {
            Sadnes.getEventBus().subscribeListener(new Listener() {
                @Override
                public void invoke(Object object) {
                    ScriptManager.this.dispatchBusEvent(string2, object);
                }

                @Override
                public Class<?> getEventType() {
                    return clazz;
                }

                @Override
                public int getPriority() {
                    return i;
                }

                @Override
                public boolean isStatic() {
                    return false;
                }
            });
        }
    }

    public static String eventKey(String string, int i) {
        return i == 0 ? string : string + "@" + i;
    }

    void dispatchBusEvent(String string, Object object) {
        if (this.hasListeners(string) && MinecraftClient.getInstance().isOnThread()) {
            NativeObject nativeObject = ScriptEventWrapper.wrapEvent(object);
            boolean flag = false;

            for (Script script : this.scripts) {
                if (script.isActive() && script.hasListeners(string) && script.fireEvent(string, nativeObject)) {
                    flag = true;
                }
            }

            if (flag && object instanceof Cancellable) {
                ((Cancellable)object).setCancelled(true);
                return;
            }
        }
    }

    private void dispatch(String string, Object... objects) {
        for (Script script : this.scripts) {
            if (script.isActive() && script.hasListeners(string)) {
                script.fireEvent(string, objects);
            }
        }
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() || screenMouseClickEvent.isRelease()) {
            if (screenMouseClickEvent.getScreen() == null || !screenMouseClickEvent.isPress()) {
                this.handleBindKey(screenMouseClickEvent.getButton(), screenMouseClickEvent.isPress());
            }
        }
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() || screenKeyEvent.isRelease()) {
            if (screenKeyEvent.getScreen() == null || !screenKeyEvent.isPress()) {
                this.handleBindKey(screenKeyEvent.getKey(), screenKeyEvent.isPress());
            }
        }
    }

    private void handleBindKey(int i, boolean flag) {
        for (Script script : this.scripts) {
            Iterator iterator2 = script.getSettings().iterator();

            while (iterator2.hasNext()) {
                ((Setting)iterator2.next()).handleKey(i, flag);
            }

            for (ScriptModule scriptModule : script.getModules()) {
                Iterator iterator3 = scriptModule.getSettings().iterator();

                while (iterator3.hasNext()) {
                    ((Setting)iterator3.next()).handleKey(i, flag);
                }

                for (ScriptFeature scriptFeature : scriptModule.getHudElements()) {
                    Iterator iterator4 = scriptFeature.getSettings().iterator();

                    while (iterator4.hasNext()) {
                        ((Setting)iterator4.next()).handleKey(i, flag);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(WorldTickEvent worldTickEvent) {
        this.rescanScripts();
        ScriptInputApi.tick();

        for (Script script : this.scripts) {
            if (script.isActive()) {
                script.tickTimers();
                script.fireEvent("tick");
            }
        }

        this.checkHealthChange();
        this.checkTabUpdate();
    }

    private void checkHealthChange() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player == null) {
            this.lastHealth = Float.NaN;
        } else {
            float f = minecraftClient.player.getHealth();
            float f2 = this.lastHealth;
            this.lastHealth = f;
            if (!Float.isNaN(f2) && f2 != f && this.hasListeners("health_change")) {
                NativeObject nativeObject = ScriptValues.newObject();
                ScriptValues.putValue(nativeObject, "from", f2);
                ScriptValues.putValue(nativeObject, "to", f);
                this.dispatch("health_change", nativeObject);
            }
        }
    }

    private void checkTabUpdate() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
        if (clientPlayNetworkHandler == null) {
            this.tabListInitialized = false;
            this.tabListHash = 0;
        } else {
            Collection<PlayerListEntry> playerListEntries = clientPlayNetworkHandler.getPlayerList();
            int i = hashTabList(playerListEntries);
            if (!this.tabListInitialized || i != this.tabListHash) {
                this.tabListInitialized = true;
                this.tabListHash = i;
                if (this.hasListeners("tab_update")) {
                    ArrayList arrayList = new ArrayList();

                    for (PlayerListEntry playerListEntry : playerListEntries) {
                        String string = playerListEntry.getProfile().name();
                        if (string != null) {
                            NativeObject nativeObject = ScriptValues.newObject();
                            ScriptValues.putValue(nativeObject, "name", string);
                            ScriptValues.putValue(nativeObject, "ping", playerListEntry.getLatency());
                            arrayList.add(nativeObject);
                        }
                    }

                    this.dispatch("tab_update", ScriptValues.toArray(arrayList));
                }
            }
        }
    }

    private static int hashTabList(Collection<PlayerListEntry> playerListEntries) {
        int i = 0;

        for (PlayerListEntry playerListEntry : playerListEntries) {
            String string = playerListEntry.getProfile().name();
            if (string != null) {
                i = i * 31 + string.hashCode();
            }

            i = i * 31 + playerListEntry.getLatency();
        }

        return i;
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (this.hasListeners("render2d")) {
            this.drawContext = render2DEvent.getContext();

            try {
                this.dispatch("render2d", (double)render2DEvent.getTickDelta());
            } finally {
                this.drawContext = null;
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent render3DEvent) {
        if (this.hasListeners("render3d")) {
            this.matrices = render3DEvent.getMatrices();
            this.tickDelta = render3DEvent.getTickDelta();

            try {
                this.dispatch("render3d", (double)render3DEvent.getTickDelta());
            } finally {
                this.matrices = null;
            }
        }
    }

    public void withMatrices(MatrixStack matrixStack, float f, Runnable runnable) {
        MatrixStack matrixStack2 = this.matrices;
        float f2 = this.tickDelta;
        this.matrices = matrixStack;
        this.tickDelta = f;

        try {
            runnable.run();
        } finally {
            this.matrices = matrixStack2;
            this.tickDelta = f2;
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && this.hasListeners("chat")) {
            minecraftClient.execute(() -> {
                String string = gameMessageS2CPacket.content().getString();
                NativeObject nativeObject = ScriptValues.newObject();
                ScriptValues.putValue(nativeObject, "text", string);
                this.dispatch("chat", nativeObject);
            });
        }

        if (packetEvent.isSend()) {
            if (minecraftClient.isOnThread()) {
                this.handleOutgoingPacket(packetEvent);
            } else {
                if (this.hasListeners("packet") || this.hasListeners("block_interact")) {
                    Packet packet = packetEvent.getPacket();
                    minecraftClient.execute(() -> this.handleOutgoingPacket(new PacketEvent(packet, PacketEvent.Direction.SEND)));
                }
            }
        }
    }

    public boolean handleIncomingPacket(Packet<?> packet) {
        if (!this.hasListeners("packet")) {
            return false;
        }

        PacketEvent packetEvent = new PacketEvent(packet, PacketEvent.Direction.RECEIVE);
        this.dispatchBusEvent("packet", packetEvent);
        return packetEvent.isCancelled();
    }

    private void handleOutgoingPacket(PacketEvent packetEvent) {
        this.dispatchBusEvent("packet", packetEvent);
        if (packetEvent.getPacket() instanceof PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket && this.hasListeners("block_interact")) {
            BlockPos blockPos = playerInteractBlockC2SPacket.getBlockHitResult().getBlockPos();
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "x", blockPos.getX());
            ScriptValues.putValue(nativeObject, "y", blockPos.getY());
            ScriptValues.putValue(nativeObject, "z", blockPos.getZ());
            ScriptValues.putValue(nativeObject, "hand", playerInteractBlockC2SPacket.getHand().name().toLowerCase(Locale.ROOT));
            ScriptValues.putValue(nativeObject, "side", playerInteractBlockC2SPacket.getBlockHitResult().getSide().name().toLowerCase(Locale.ROOT));
            this.dispatch("block_interact", nativeObject);
        }
    }

    public boolean renderClickGuiBackground(DrawContext drawContext2, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        if (!this.hasListeners("clickgui_bg")) {
            return false;
        }

        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", f);
        ScriptValues.putValue(nativeObject, "y", f2);
        ScriptValues.putValue(nativeObject, "w", f3);
        ScriptValues.putValue(nativeObject, "h", f4);
        ScriptValues.putValue(nativeObject, "radius", f5);
        ScriptValues.putValue(nativeObject, "sidebar", f6);
        ScriptValues.putValue(nativeObject, "alpha", f7);
        return this.dispatchRenderHook("clickgui_bg", drawContext2, f7, nativeObject);
    }

    public boolean renderClickGuiScreenBackground(DrawContext drawContext2, float f, float f2, float f3) {
        if (!this.hasListeners("clickgui_screen_bg")) {
            return false;
        }

        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "w", f);
        ScriptValues.putValue(nativeObject, "h", f2);
        ScriptValues.putValue(nativeObject, "alpha", f3);
        return this.dispatchRenderHook("clickgui_screen_bg", drawContext2, f3, nativeObject);
    }

    public boolean renderEditorBackground(DrawContext drawContext2, float f, float f2, float f3, float f4, float f5, float f6) {
        if (!this.hasListeners("editor_bg")) {
            return false;
        }

        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", f);
        ScriptValues.putValue(nativeObject, "y", f2);
        ScriptValues.putValue(nativeObject, "w", f3);
        ScriptValues.putValue(nativeObject, "h", f4);
        ScriptValues.putValue(nativeObject, "radius", f5);
        ScriptValues.putValue(nativeObject, "alpha", f6);
        return this.dispatchRenderHook("editor_bg", drawContext2, f6, nativeObject);
    }

    public boolean renderHudHeaderBackground(DrawContext drawContext2, float f, float f2, float f3, float f4, float f5, float f6) {
        return !this.hasListeners("hud_header_bg")
            ? false
            : this.dispatchRenderHook("hud_header_bg", drawContext2, f5, createHudBgArgs(f, f2, f3, f4, f5, f6));
    }

    public boolean renderHudCellBackground(DrawContext drawContext2, float f, float f2, float f3, float f4, float f5, float f6) {
        return !this.hasListeners("hud_cell_bg")
            ? false
            : this.dispatchRenderHook("hud_cell_bg", drawContext2, f5, createHudBgArgs(f, f2, f3, f4, f5, f6));
    }

    private static NativeObject createHudBgArgs(float f, float f2, float f3, float f4, float f5, float f6) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", f);
        ScriptValues.putValue(nativeObject, "y", f2);
        ScriptValues.putValue(nativeObject, "w", f3);
        ScriptValues.putValue(nativeObject, "h", f4);
        ScriptValues.putValue(nativeObject, "alpha", f5);
        ScriptValues.putValue(nativeObject, "corner", f6);
        return nativeObject;
    }

    private boolean dispatchRenderHook(String string, DrawContext drawContext2, float f, NativeObject nativeObject) {
        boolean[] booleans = new boolean[]{false};
        this.withDrawContext(drawContext2, f, () -> {
            for (Script script : this.scripts) {
                if (script.isActive() && script.hasListeners(string) && script.fireEventAny(string, nativeObject)) {
                    booleans[0] = true;
                }
            }
        });
        return booleans[0];
    }

    public void withDrawContext(DrawContext drawContext2, float f, Runnable runnable) {
        DrawContext drawContext3 = this.drawContext;
        float f2 = ScriptRenderApi.alpha;
        this.drawContext = drawContext2;
        ScriptRenderApi.alpha = f;

        try {
            runnable.run();
        } finally {
            this.drawContext = drawContext3;
            ScriptRenderApi.alpha = f2;
        }
    }

    public Path getScriptsDir() {
        return this.scriptsDir;
    }

    public McpServer getDevServer() {
        return this.devServer;
    }

    public DrawContext getDrawContext() {
        return this.drawContext;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }
}
