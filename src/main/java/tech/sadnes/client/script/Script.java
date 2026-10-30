package tech.sadnes.client.script;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.script.api.ScriptBindings;
import tech.sadnes.client.script.api.ScriptInputApi;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingHolder;
import tech.sadnes.client.setting.ValueSetting;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.ui.screen.ScriptScreen;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.JavaScriptException;
import tech.sadnes.libs.rhino.RhinoException;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class Script implements SettingHolder {
    public static final String SOURCE_DIR = "src";
    public static final String ASSETS_DIR = "assets";
    public static final String MAIN_FILE = "main.js";
    private static Script current;
    private final Path directory;
    private final ScriptPackage scriptPackage;
    private final String name;
    private Script.Metadata metadata = new Script.Metadata("", "", "", "");
    private final Map<String, List<Script.Listener>> listeners = new LinkedHashMap<>();
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<ScriptModule> modules = new ArrayList<>();
    private final List<Script.Timer> timers = new ArrayList<>();
    private final BooleanSetting enabled;
    private final ScriptStorage storage;
    private int nextListenerId;
    private int nextTimerId;
    private ScriptableObject scope;
    private boolean loading;
    private boolean loaded;
    private boolean console;
    private JsonObject savedConfig = new JsonObject();
    private final Map<String, Object> pendingSettingValues = new HashMap<>();
    private Map<String, Object> defaultSettingValues = Map.of();
    private boolean active;
    private boolean errored;
    private String errorMessage;
    private int errorLine;
    private long lastModified;

    public static Script current() {
        return current;
    }

    private Script(Path path, ScriptPackage scriptPackage2, String string, Path path2) {
        this.directory = path;
        this.scriptPackage = scriptPackage2;
        this.name = string;
        this.storage = new ScriptStorage(path2);
        this.enabled = new BooleanSetting("scripts.script.enable", false);
        this.enabled.setDisplayName("$enabled");
        this.enabled.setOnChange(this::onEnabledChanged);
        this.settings.add(this.enabled);
    }

    public Script(Path path) {
        this(path, null, path.getFileName().toString(), path.resolve("data.json"));
    }

    public static Script fromPackage(Path path, ScriptPackage scriptPackage2) {
        String string = path.getFileName().toString();
        String string2 = string.substring(0, string.length() - ".ss".length());
        return new Script(path, scriptPackage2, string2, path.resolveSibling(".data").resolve(string2 + ".json"));
    }

    public static Script createConsole(Path path) {
        Path path2 = path.resolve(".console");
        Script script = new Script(path2, null, "console", path2.resolve("data.json"));
        script.console = true;
        Script script2 = current;
        current = script;
        Context context = ScriptRuntimeSandbox.enter();

        try {
            script.scope = ScriptRuntimeSandbox.newScope(context);
            ScriptBindings.installAll(script.scope, script);
        } finally {
            ScriptRuntimeSandbox.exit();
            current = script2;
        }

        return script;
    }

    public boolean isPackaged() {
        return this.scriptPackage != null;
    }

    public Path getAssetsRoot() {
        return this.scriptPackage == null ? this.directory : this.scriptPackage.getAssetsDir();
    }

    public Path getSourceDir() {
        return this.directory.resolve("src");
    }

    public Path getAssetsDir() {
        return this.getAssetsRoot().resolve("assets");
    }

    public Path getMainFile() {
        Path path = this.getSourceDir().resolve("main.js");
        if (Files.isRegularFile(path)) {
            return path;
        }

        List list = this.listSourceFiles();
        return list.isEmpty() ? path : (Path)list.get(0);
    }

    public List<Path> listSourceFiles() {
        if (this.scriptPackage != null) {
            return List.of();
        }

        Path path = this.getSourceDir();
        if (!Files.isDirectory(path)) {
            return List.of();
        }

        try (Stream stream = Files.walk(path, 4)) {
            return stream.filter(object -> object.toString().toLowerCase(Locale.ROOT).endsWith(".js")).sorted(Comparator.comparing(Path::toString)).toList();
        } catch (IOException ioException) {
            return List.of();
        }
    }

    public boolean load() {
        this.saveConfig();
        this.pendingSettingValues.putAll(this.collectSettingValues());
        this.pendingSettingValues.remove("s:$enabled");
        if (this.scope != null) {
            ScriptAuraHooks.unregisterAll(this);
        }

        this.scope = null;
        this.loaded = false;
        this.listeners.clear();
        this.timers.clear();
        this.modules.clear();
        this.settings.clear();
        this.settings.add(this.enabled);
        this.errored = false;
        this.active = false;
        this.errorMessage = null;
        this.errorLine = 0;
        Path path = this.getMainFile();
        if (this.scriptPackage == null && !Files.isRegularFile(path)) {
            this.reportError("нет src/main.js", 0);
            return false;
        }

        Script script = current;
        current = this;

        try {
            this.lastModified = this.computeLastModified();
            String string = this.scriptPackage != null ? this.scriptPackage.readSource("main.js") : Files.readString(path);
            if (string == null) {
                this.reportError("в пакете нет src/main.js", 0);
                return false;
            }

            this.metadata = parseMetadata(string);
            if (this.isEnabled()) {
                this.loading = true;
                Context context = ScriptRuntimeSandbox.enter();

                try {
                    this.scope = ScriptRuntimeSandbox.newScope(context);
                    ScriptBindings.installAll(this.scope, this);
                    context.evaluateString(this.scope, string, this.scriptPackage != null ? "main.js" : path.getFileName().toString(), 1, null);
                    this.loaded = true;
                    this.defaultSettingValues = this.snapshotSettingValues();
                    ConfigManager configManager = Sadnes.getInstance().getConfigManager();
                    if (configManager != null) {
                        configManager.applyScript(this, this.savedConfig);
                    }

                    this.applySettingValues(this.pendingSettingValues);
                    this.pendingSettingValues.clear();
                } finally {
                    ScriptRuntimeSandbox.exit();
                }

                this.loading = false;
                this.onEnabledChanged();
                return true;
            }
        } catch (RhinoException rhinoException) {
            this.reportError(describeError(rhinoException), rhinoException.lineNumber());
            return false;
        } catch (ScriptRuntimeSandbox.ScriptTimeoutError | Exception exception) {
            this.reportError(String.valueOf(exception.getMessage()), 0);
            return false;
        } finally {
            this.loading = false;
            current = script;
        }

        return true;
    }

    public void include(String string) {
        Path path = ScriptPathValidator.resolveSafe(this.getSourceDir(), string);
        if (!path.toString().toLowerCase(Locale.ROOT).endsWith(".js")) {
            throw Context.reportRuntimeError("include expects a .js source file");
        }

        String string2;
        if (this.scriptPackage != null) {
            string2 = this.scriptPackage.readSource(string);
            if (string2 == null) {
                throw Context.reportRuntimeError("include: нет файла " + string);
            }
        } else {
            if (!Files.isRegularFile(path)) {
                throw Context.reportRuntimeError("include: нет файла " + string);
            }

            try {
                string2 = Files.readString(path);
            } catch (IOException ioException) {
                throw Context.reportRuntimeError("include: " + ioException.getMessage());
            }
        }

        Context context = ScriptRuntimeSandbox.enter();

        try {
            context.evaluateString(this.scope, string2, string, 1, null);
        } finally {
            ScriptRuntimeSandbox.exit();
        }
    }

    public long computeLastModified() {
        if (this.scriptPackage != null) {
            return getModifiedTime(this.directory);
        }

        long l = 0L;

        for (Path path : this.listSourceFiles()) {
            l = Math.max(l, getModifiedTime(path));
        }

        Path path2 = this.getAssetsDir();
        if (!Files.isDirectory(path2)) {
            return l;
        }

        try (Stream<Path> stream = Files.walk(path2, 4)) {
            for (Path path3 : (Iterable<Path>) stream::iterator) {
                String string = path3.toString().toLowerCase(Locale.ROOT);
                if (string.endsWith(".fsh") || string.endsWith(".glsl")) {
                    l = Math.max(l, getModifiedTime(path3));
                }
            }
        } catch (IOException ioException) {
        }

        return l;
    }

    private static long getModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ioException) {
            return 0L;
        }
    }

    public void deactivate() {
        if (this.active) {
            this.active = false;
            this.fireEvent("disable");
            ScriptInputApi.releaseAll(this);
            ScriptScreen.closeFor(this);
        }
    }

    public void unload() {
        this.saveConfig();
        this.deactivate();
        ScriptAuraHooks.unregisterAll(this);
        this.storage.save();
        if (this.scriptPackage != null) {
            this.scriptPackage.cleanup();
        }
    }

    public void setEnabled(boolean flag) {
        this.enabled.setValue(flag);
    }

    public void toggle() {
        if (!Boolean.TRUE.equals(this.enabled.getValue())) {
            this.setEnabled(true);
        } else {
            this.setEnabled(false);
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled.getValue());
    }

    private void onEnabledChanged() {
        if (!this.loading) {
            boolean flag;
            if (Boolean.TRUE.equals(this.enabled.getValue())) {
                flag = !this.errored;
            } else {
                flag = false;
            }

            if (flag && !this.loaded) {
                this.load();
            } else if (flag != this.active) {
                this.active = flag;
                this.fireEvent(this.active ? "enable" : "disable");
                ScriptAuraHooks.refreshAll();
                if (this.hasFeatures()) {
                    ScriptManager.refreshClickGui();
                }

                ConfigManager configManager = Sadnes.getInstance().getConfigManager();
                if (configManager != null) {
                    configManager.markDirty();
                }
            }
        }
    }

    public int addListener(String string, Function function, boolean flag) {
        int i = ++this.nextListenerId;
        this.listeners.computeIfAbsent(string, string2 -> new ArrayList<>()).add(new Script.Listener(i, function, flag));
        return i;
    }

    public boolean removeListener(int i) {
        for (List<Script.Listener> listeners2 : this.listeners.values()) {
            if (listeners2.removeIf(listener -> listener.id == i)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasListeners(String string) {
        List list = this.listeners.get(string);
        return list != null && !list.isEmpty();
    }

    public boolean fireEvent(String string, Object... objects) {
        return this.dispatchEvent(string, false, objects);
    }

    public boolean fireEventAny(String string, Object... objects) {
        return this.dispatchEvent(string, true, objects);
    }

    private boolean dispatchEvent(String string, boolean flag, Object... objects) {
        List<Script.Listener> listeners2 = this.listeners.get(string);
        if (listeners2 != null && !listeners2.isEmpty()) {
            boolean flag2 = false;
            boolean flag3 = false;
            Object[] objects2 = objects;
            Script script = current;
            current = this;
            Context context = ScriptRuntimeSandbox.enter();

            try {
                for (Script.Listener listener : new ArrayList<>(listeners2)) {
                    try {
                        if (objects2 == objects) {
                            objects2 = this.convertArgs(objects);
                        }

                        Object object = listener.fn.call(context, this.scope, this.scope, objects2);
                        if (Context.toBoolean(object)) {
                            flag2 = true;
                        }

                        if (!Boolean.FALSE.equals(object)) {
                            flag3 = true;
                        }

                        if (listener.once) {
                            listeners2.remove(listener);
                        }
                    } catch (RhinoException rhinoException) {
                        this.reportError(describeError(rhinoException), rhinoException.lineNumber());
                        break;
                    } catch (ScriptRuntimeSandbox.ScriptTimeoutError scriptTimeoutError) {
                        this.reportError(scriptTimeoutError.getMessage(), 0);
                        break;
                    } catch (Exception exception) {
                        this.reportError(String.valueOf(exception.getMessage()), 0);
                        break;
                    }
                }
            } finally {
                ScriptRuntimeSandbox.exit();
                current = script;
            }

            return flag ? flag3 : flag2;
        } else {
            return false;
        }
    }

    public Object call(Function function, Object... objects) {
        return this.callWithThis(this.scope, function, objects);
    }

    public Object callWithThis(Scriptable scriptable, Function function, Object... objects) {
        if (this.scope != null && (this.console || this.active || this.loading) && !this.errored) {
            Script script = current;
            current = this;
            Context context = ScriptRuntimeSandbox.enter();

            try {
                return function.call(context, this.scope, scriptable, this.convertArgs(objects));
            } catch (RhinoException rhinoException) {
                this.reportError(describeError(rhinoException), rhinoException.lineNumber());
                return null;
            } catch (ScriptRuntimeSandbox.ScriptTimeoutError | Exception exception) {
                this.reportError(String.valueOf(exception.getMessage()), 0);
            } finally {
                ScriptRuntimeSandbox.exit();
                current = script;
            }

            return null;
        } else {
            return null;
        }
    }

    public Object evaluate(String string) {
        if (this.scope != null && (this.console || this.isActive())) {
            Script script = current;
            current = this;
            Context context = ScriptRuntimeSandbox.enter();

            try {
                return context.evaluateString(this.scope, string, "mcp", 1, null);
            } finally {
                ScriptRuntimeSandbox.exit();
                current = script;
            }
        } else {
            throw new IllegalStateException("script is disabled or not loaded");
        }
    }

    public void runConsoleInput(String string) {
        ScriptConsole.log(this.name, "input", string);
        if (this.scope != null && (this.console || this.isActive())) {
            Script script = current;
            current = this;
            Context context = ScriptRuntimeSandbox.enter();

            try {
                Object object = context.evaluateString(this.scope, string, "console", 1, null);
                ScriptConsole.log(this.name, "result", ScriptValues.stringify(object));
            } catch (RhinoException rhinoException) {
                ScriptConsole.error(this.name, describeError(rhinoException));
            } catch (ScriptRuntimeSandbox.ScriptTimeoutError | RuntimeException exception) {
                ScriptConsole.error(this.name, String.valueOf(exception.getMessage()));
            } finally {
                ScriptRuntimeSandbox.exit();
                current = script;
            }
        } else {
            ScriptConsole.error(this.name, Language.translate("scripts.console.not_loaded", "Скрипт не загружен — выполнить код негде"));
        }
    }

    public int addTimer(long l, boolean flag, Function function) {
        int i = ++this.nextTimerId;
        this.timers.add(new Script.Timer(i, Math.max(0L, l), flag, function));
        return i;
    }

    public boolean removeTimer(int i) {
        return this.timers.removeIf(timer -> timer.id == i);
    }

    public void tickTimers() {
        if (!this.timers.isEmpty()) {
            for (Script.Timer timer : new ArrayList<>(this.timers)) {
                if (timer.stopWatch.isElapsed(timer.intervalMs)) {
                    if (timer.repeat) {
                        timer.stopWatch.reset();
                    } else {
                        this.timers.remove(timer);
                    }

                    this.call(timer.fn);
                    if (this.errored) {
                        return;
                    }
                }
            }
        }
    }

    public void addSetting(Setting<?> setting) {
        this.settings.add(setting);
    }

    public ScriptModule getOrCreateModule(String string, boolean flag) {
        for (ScriptModule scriptModule : this.modules) {
            if (scriptModule.getDisplayName().equals(string)) {
                return scriptModule;
            }
        }

        ScriptModule scriptModule2 = new ScriptModule(this, string, flag);
        this.modules.add(scriptModule2);
        return scriptModule2;
    }

    public boolean hasVisibleSettings() {
        for (Setting setting : this.settings) {
            if (this.isSettingVisible(setting)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, Object> collectSettingValues() {
        HashMap hashMap = new HashMap<>(this.pendingSettingValues);
        hashMap.putAll(this.snapshotSettingValues());
        return hashMap;
    }

    private Map<String, Object> snapshotSettingValues() {
        HashMap hashMap = new HashMap();
        this.writeSettings("s:", this.settings, hashMap);

        for (ScriptModule scriptModule : this.modules) {
            this.writeSettings("m:" + scriptModule.getDisplayName() + ":", scriptModule.getSettings(), hashMap);

            for (ScriptFeature scriptFeature : scriptModule.getHudElements()) {
                this.writeSettings("f:" + scriptModule.getDisplayName() + ":" + scriptFeature.getDisplayName() + ":", scriptFeature.getSettings(), hashMap);
            }
        }

        return hashMap;
    }

    public void applySettingValues(Map<String, Object> map) {
        if (!this.loaded) {
            this.pendingSettingValues.putAll(map);
            this.pendingSettingValues.remove("s:$enabled");
        } else {
            this.readSettings("s:", this.settings, map);

            for (ScriptModule scriptModule : this.modules) {
                this.readSettings("m:" + scriptModule.getDisplayName() + ":", scriptModule.getSettings(), map);

                for (ScriptFeature scriptFeature : scriptModule.getHudElements()) {
                    this.readSettings("f:" + scriptModule.getDisplayName() + ":" + scriptFeature.getDisplayName() + ":", scriptFeature.getSettings(), map);
                }
            }
        }
    }

    public boolean hasFeatures() {
        for (ScriptModule scriptModule : this.modules) {
            if (!scriptModule.getHudElements().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void writeSettings(String string, List<Setting<?>> settings2, Map<String, Object> map) {
        for (Setting setting : this.flattenSettings(settings2)) {
            Object object = setting.getValue();
            if (object instanceof List) {
                object = List.copyOf((List)object);
            }

            map.put(string + setting.getDisplayName(), object);
            if (setting instanceof ValueSetting valueSetting && !valueSetting.getBinds().isEmpty()) {
                map.put(string + setting.getDisplayName() + "$binds", List.copyOf(valueSetting.getBinds()));
            }
        }
    }

    private void readSettings(String string, List<Setting<?>> settings2, Map<String, Object> map) {
        for (Setting setting : this.flattenSettings(settings2)) {
            if (setting != this.enabled) {
                Object object = map.get(string + setting.getDisplayName());
                if (object != null) {
                    try {
                        setting.setValue(object);
                    } catch (Exception exception) {
                    }
                }
            }

            if (map.get(string + setting.getDisplayName() + "$binds") instanceof List list && setting instanceof ValueSetting valueSetting) {
                try {
                    valueSetting.setBinds(list);
                } catch (Exception exception2) {
                }
            }
        }
    }

    private List<Setting<?>> flattenSettings(List<Setting<?>> settings2) {
        ArrayList arrayList = new ArrayList(settings2);

        for (Setting setting : settings2) {
            arrayList.addAll(setting.getAllChildren());
        }

        return arrayList;
    }

    public void reportError(String string, int i) {
        boolean flag = !this.errored;
        this.errored = true;
        this.active = false;
        ScriptInputApi.releaseAll(this);
        this.errorMessage = string != null && !string.isEmpty() ? string : "unknown";
        this.errorLine = i;
        if (flag) {
            ScriptConsole.error(this.name, this.errorMessage + (i > 0 ? " (line " + i + ")" : ""));
            String string2 = i > 0 ? " (" + Language.translate("scripts.error.line", "строка") + " " + i + ")" : "";
            NotificationUtil.error(
                NotificationSources.scripts(), Language.translate("scripts.error", "Ошибка скрипта") + ": " + this.name + " — " + this.errorMessage + string2
            );
            ChatUtil.print("§c[JS] §f" + this.name + " — " + this.errorMessage + string2);
        }
    }

    public void failWith(String string) {
        this.lastModified = this.computeLastModified();
        this.reportError(string, 0);
    }

    public void clearError() {
        this.errored = false;
        this.errorMessage = null;
        this.errorLine = 0;
    }

    public JsonObject getSavedConfig() {
        return this.savedConfig.deepCopy();
    }

    private void saveConfig() {
        if (this.loaded) {
            ConfigManager configManager = Sadnes.getInstance().getConfigManager();
            if (configManager != null) {
                this.savedConfig = configManager.serializeScript(this);
            }
        }
    }

    public void setSavedConfig(JsonObject jsonObject) {
        this.savedConfig = jsonObject.deepCopy();
        this.pendingSettingValues.clear();
        if (this.loaded) {
            this.applySettingValues(this.defaultSettingValues);
        }
    }

    private Object[] convertArgs(Object[] objects) {
        if (objects != null && objects.length != 0) {
            Object[] objects2 = new Object[objects.length];
            int i = 0;

            for (int i2 = objects.length; i < i2; i++) {
                objects2[i] = ScriptValues.toScript(objects[i]);
            }

            return objects2;
        } else {
            return objects;
        }
    }

    private static String describeError(RhinoException rhinoException) {
        String string = rhinoException instanceof JavaScriptException javaScriptException ? Context.toString(javaScriptException.getValue()) : rhinoException.details();
        return string != null && !string.isEmpty() ? string : String.valueOf(rhinoException.getMessage());
    }

    private static Script.Metadata parseMetadata(String string) {
        String string2 = "";
        String string3 = "";
        String string4 = "";
        String string5 = "";
        int i = 0;

        for (String string6 : string.split("\n", 40)) {
            if (i++ >= 30) {
                break;
            }

            String string7 = string6.trim();
            if (string7.startsWith("//")) {
                String string8 = string7.substring(2).trim();
                if (string8.startsWith("@name ")) {
                    string2 = string8.substring(6).trim();
                } else if (string8.startsWith("@author ")) {
                    string3 = string8.substring(8).trim();
                } else if (string8.startsWith("@version ")) {
                    string4 = string8.substring(9).trim();
                } else if (string8.startsWith("@description ")) {
                    string5 = string8.substring(13).trim();
                }
            }
        }

        return new Script.Metadata(string2, string3, string4, string5);
    }

    public String getTitle() {
        return this.metadata.displayName().isEmpty() ? this.name : this.metadata.displayName();
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return this.settings;
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        return setting != this.enabled && !setting.isHidden();
    }

    public Path getDirectory() {
        return this.directory;
    }

    public Script.Metadata getMetadata() {
        return this.metadata;
    }

    public List<ScriptModule> getModules() {
        return this.modules;
    }

    public ScriptStorage getStorage() {
        return this.storage;
    }

    public boolean hasError() {
        return this.errored;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public int getErrorLine() {
        return this.errorLine;
    }

    public long getLastModified() {
        return this.lastModified;
    }

    record Listener(int id, Function fn, boolean once) {
    }

    public record Metadata(String displayName, String author, String version, String description) {
    }

    static final class Timer {
        final int id;
        final long intervalMs;
        final boolean repeat;
        final Function fn;
        final StopWatch stopWatch = new StopWatch();

        Timer(int i, long l, boolean flag, Function function) {
            this.id = i;
            this.intervalMs = l;
            this.repeat = flag;
            this.fn = function;
        }
    }
}
