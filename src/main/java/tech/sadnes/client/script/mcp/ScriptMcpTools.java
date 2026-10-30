package tech.sadnes.client.script.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptConsole;
import tech.sadnes.client.script.ScriptLinter;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptModule;
import tech.sadnes.client.script.ScriptRuntimeSandbox;
import tech.sadnes.client.script.editor.FilePreview;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.libs.rhino.RhinoException;
import tech.sadnes.libs.rhino.Undefined;

final class ScriptMcpTools implements IMinecraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern DECLARATION_PATTERN = Pattern.compile("(?m)^\\s*(?:function|var|let|const)\\s+([A-Za-z_$][\\w$]*)");
    private final ScriptManager scriptManager;
    private final Map<String, ScriptMcpTools.Tool> tools = new LinkedHashMap<>();

    ScriptMcpTools(ScriptManager scriptManager2) {
        this.scriptManager = scriptManager2;
        this.registerTool(
            "list_scripts",
            "List all scripts of the running client: metadata, enabled/failed state, files, modules and settings.",
            "{\"type\":\"object\",\"properties\":{}}",
            this::listScripts
        );
        this.registerTool(
            "read_file",
            "Read a text file from a script folder. Path is relative to the script folder, e.g. src/main.js or assets/config.json.",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"path\":{\"type\":\"string\"}},\"required\":[\"script\",\"path\"]}",
            this::readFile
        );
        this.registerTool(
            "write_file",
            "Write a text file into a script folder and hot-reload the script immediately. The response reports the reload result including syntax and lint errors — always check it. For large risky edits run check_syntax first.",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"path\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},\"required\":[\"script\",\"path\",\"content\"]}",
            this::writeFile
        );
        this.registerTool(
            "create_script",
            "Create a new script: a folder with src/main.js and assets/. Optional preset: 'rotation' (custom aura rotation mode with a math helper) 'render' (module + HUD + world render + custom shader) 'ui' (custom panel in ClickGui + own screen) 'esp' (own ESP element, nametag segments, custom chams and glow shaders) 'clickgui' (a complete module menu built on client.categories() and setting handles), 'scaffold' (bridge building: rotation.lookAt + world.placeTarget/raycast + player.place, sneak at the edge) or one of the combat modules written as scripts — 'autosoup' (interactItem + dropHeld + inventory.swapHand), 'autouse' (food/invisibility from the offhand: container.slots with nutrition/effects, input.hold, rightClick), 'wtap', 'sneaktap' (attack + move_input), 'uppercuts' (aura.rangeHook/hitMomentHook) — and movement modules: 'nulls' (input.physical), 'jesus' (forced_sprint, keep_sprint legacy, hitMomentHook), 'fastladder' (move event), 'elytrabooster' (firework_boost, setting groups), 'elytrafly' (startGliding, move.holdJump), 'nofall' (water bucket: raycast with 'source' fluids + interactItem + place, slime_bounce), 'autododge' (move.escapeFrom + e.steer at 'low' priority, inventory.throwItem), 'highjump' (three modes: slime, packet cancel, snow placement) — presets are working examples, prefer them over a blank start. Returns the actual script name (may get a numeric suffix if taken).",
            "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"preset\":{\"type\":\"string\",\"enum\":[\"rotation\",\"render\",\"ui\",\"esp\",\"clickgui\",\"scaffold\",\"autosoup\",\"autouse\",\"wtap\",\"sneaktap\",\"uppercuts\",\"nulls\",\"jesus\",\"fastladder\",\"elytrabooster\",\"elytrafly\",\"nofall\",\"autododge\",\"highjump\"]}},\"required\":[\"name\"]}",
            this::createScript
        );
        this.registerTool(
            "delete_file",
            "Delete a single file inside a script folder (path relative to the script folder).",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"path\":{\"type\":\"string\"}},\"required\":[\"script\",\"path\"]}",
            this::deleteFile
        );
        this.registerTool(
            "check_syntax",
            "Check JavaScript code for syntax errors and lint issues WITHOUT writing it anywhere. Pass the full intended file content.",
            "{\"type\":\"object\",\"properties\":{\"code\":{\"type\":\"string\"},\"filename\":{\"type\":\"string\"},\"script\":{\"type\":\"string\",\"description\":\"optional: resolve include()-names against this script\"}},\"required\":[\"code\"]}",
            this::checkSyntax
        );
        this.registerTool(
            "reload_script",
            "Reload a script from disk. Returns the compile error if it failed.",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"}},\"required\":[\"script\"]}",
            this::reloadScriptTool
        );
        this.registerTool(
            "set_script_enabled",
            "Enable or disable a script (same as the user's toggle in the GUI).",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"enabled\":{\"type\":\"boolean\"}},\"required\":[\"script\",\"enabled\"]}",
            this::setScriptEnabled
        );
        this.registerTool(
            "set_module_enabled",
            "Enable or disable a module declared by a script via Module(name).",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"module\":{\"type\":\"string\"},\"enabled\":{\"type\":\"boolean\"}},\"required\":[\"script\",\"module\",\"enabled\"]}",
            this::setModuleEnabled
        );
        this.registerTool(
            "get_logs",
            "Recent print() output and runtime errors of scripts, newest last. Use after enabling a script to see what it does.",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\",\"description\":\"filter by script name\"},\"limit\":{\"type\":\"integer\"}}}",
            this::getLogs
        );
        this.registerTool(
            "run_js",
            "Execute a JavaScript snippet in the live scope of a loaded script and return the result. Great for debugging: inspect player state, call API functions, test expressions. Errors do not break the script.",
            "{\"type\":\"object\",\"properties\":{\"script\":{\"type\":\"string\"},\"code\":{\"type\":\"string\"}},\"required\":[\"script\",\"code\"]}",
            this::runJs
        );
        this.registerTool(
            "get_api_docs",
            "Full reference of the scripting API: script structure, events, globals and object members. Read this before writing any script — the API is custom to this client.",
            "{\"type\":\"object\",\"properties\":{}}",
            jsonObject -> McpWorkspaceWriter.buildApiDoc()
        );
    }

    private void registerTool(String string, String string2, String string3, Function<JsonObject, String> function) {
        this.tools.put(string, new ScriptMcpTools.Tool(string2, JsonParser.parseString(string3).getAsJsonObject(), function));
    }

    JsonArray listTools() {
        JsonArray jsonArray = new JsonArray();

        for (Entry entry : this.tools.entrySet()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", (String)entry.getKey());
            jsonObject.addProperty("description", ((ScriptMcpTools.Tool)entry.getValue()).description());
            jsonObject.add("inputSchema", ((ScriptMcpTools.Tool)entry.getValue()).schema());
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    String callTool(String string, JsonObject jsonObject) {
        ScriptMcpTools.Tool tool = this.tools.get(string);
        if (tool == null) {
            throw new IllegalArgumentException("unknown tool: " + string);
        }

        Function function = tool.handler();
        return jsonObject == null ? (String)function.apply(new JsonObject()) : (String)function.apply(jsonObject);
    }

    private String listScripts(JsonObject jsonObject) {
        return this.runOnClientThread(() -> {
            JsonArray jsonArray = new JsonArray();

            for (Script script : this.scriptManager.getScripts()) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("name", script.getDisplayName());
                if (!script.getTitle().equals(script.getDisplayName())) {
                    jsonObject2.addProperty("displayName", script.getTitle());
                }

                if (!script.getMetadata().version().isEmpty()) {
                    jsonObject2.addProperty("version", script.getMetadata().version());
                }

                if (!script.getMetadata().author().isEmpty()) {
                    jsonObject2.addProperty("author", script.getMetadata().author());
                }

                if (!script.getMetadata().description().isEmpty()) {
                    jsonObject2.addProperty("description", script.getMetadata().description());
                }

                jsonObject2.addProperty("enabled", script.isEnabled());
                jsonObject2.addProperty("running", script.isActive());
                if (script.hasError()) {
                    jsonObject2.addProperty("error", script.getErrorMessage());
                }

                jsonObject2.add("files", this.listScriptFiles(script));
                JsonArray jsonArray2 = this.serializeSettings(script.getSettings());
                if (!jsonArray2.isEmpty()) {
                    jsonObject2.add("settings", jsonArray2);
                }

                JsonArray jsonArray3 = new JsonArray();

                for (ScriptModule scriptModule : script.getModules()) {
                    JsonObject jsonObject3 = new JsonObject();
                    jsonObject3.addProperty("name", scriptModule.getDisplayName());
                    if (!scriptModule.getDescription().isEmpty()) {
                        jsonObject3.addProperty("description", scriptModule.getDescription());
                    }

                    jsonObject3.addProperty("enabled", Boolean.TRUE.equals(scriptModule.getEnabledSetting().getValue()));
                    JsonArray jsonArray4 = this.serializeSettings(scriptModule.getSettings());
                    if (!jsonArray4.isEmpty()) {
                        jsonObject3.add("settings", jsonArray4);
                    }

                    jsonArray3.add(jsonObject3);
                }

                if (!jsonArray3.isEmpty()) {
                    jsonObject2.add("modules", jsonArray3);
                }

                jsonArray.add(jsonObject2);
            }

            return jsonArray.isEmpty() ? "no scripts installed; use create_script" : GSON.toJson(jsonArray);
        });
    }

    private JsonArray serializeSettings(List<Setting<?>> settings) {
        JsonArray jsonArray = new JsonArray();

        for (Setting setting : settings) {
            if (!setting.getDisplayName().startsWith("$")) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("name", setting.getName());
                jsonObject.addProperty("type", setting.getClass().getSimpleName().replace("Setting", "").toLowerCase(Locale.ROOT));
                jsonObject.addProperty("value", String.valueOf(setting.getValue()));
                jsonArray.add(jsonObject);
            }
        }

        return jsonArray;
    }

    private JsonArray listScriptFiles(Script script) {
        JsonArray jsonArray = new JsonArray();
        if (script.isPackaged()) {
            return jsonArray;
        }

        try (Stream<Path> stream = Files.walk(script.getAssetsRoot())) {
            stream.filter(path -> Files.isRegularFile(path)).sorted().forEach(path2 -> jsonArray.add(this.relativePath(script, path2)));
        } catch (IOException ioException) {
        }

        return jsonArray;
    }

    private String readFile(JsonObject jsonObject) {
        Script script = this.resolveScript(jsonObject);
        Path path = this.resolveScriptPath(script, this.requireString(jsonObject, "path"));
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("file not found: " + this.requireString(jsonObject, "path") + "; files: " + this.listScriptFiles(script));
        }

        try {
            return !FilePreview.isTextFile(path) ? "binary file, " + Files.size(path) + " bytes" : Files.readString(path);
        } catch (IOException ioException) {
            throw new RuntimeException("read failed: " + ioException.getMessage());
        }
    }

    private String writeFile(JsonObject jsonObject) {
        Script script = this.resolveScript(jsonObject);
        String string = this.requireString(jsonObject, "path");
        String string2 = this.requireString(jsonObject, "content");
        Path path = this.resolveScriptPath(script, string);

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, string2);
        } catch (IOException ioException) {
            throw new RuntimeException("write failed: " + ioException.getMessage());
        }

        StringBuilder builder = new StringBuilder("saved " + string + " (" + string2.getBytes(StandardCharsets.UTF_8).length + " bytes)\n");
        builder.append(this.reloadScript(script));
        if (string.endsWith(".js") && !script.hasError()) {
            this.appendLintReport(builder, string2, string, script, path);
        }

        return builder.toString();
    }

    private void appendLintReport(StringBuilder builder, String string, String string2, Script script, Path path) {
        List<ScriptLinter.Diagnostic> diagnostics = ScriptLinter.lint(string, string2, this.collectKnownGlobals(script, path));
        if (!diagnostics.isEmpty()) {
            builder.append("\nlint warnings:");

            for (ScriptLinter.Diagnostic diagnostic : diagnostics) {
                builder.append("\n- line ").append(diagnostic.line()).append(": ").append(diagnostic.message());
            }
        }
    }

    private Set<String> collectKnownGlobals(Script script, Path path) {
        HashSet hashSet = new HashSet();

        for (Path path2 : script.listSourceFiles()) {
            if (!path2.equals(path)) {
                try {
                    Matcher matcher2 = DECLARATION_PATTERN.matcher(Files.readString(path2));

                    while (matcher2.find()) {
                        hashSet.add(matcher2.group(1));
                    }
                } catch (IOException ioException) {
                }
            }
        }

        return hashSet;
    }

    private String createScript(JsonObject jsonObject) {
        String string = this.optString(jsonObject, "preset", null);
        return this.runOnClientThread(
            () -> {
                Script script = this.scriptManager.createScriptFromPreset(this.requireString(jsonObject, "name"), string);
                if (script == null) {
                    throw new RuntimeException("failed to create script (see game log)");
                } else {
                    return "created script '"
                        + script.getDisplayName()
                        + "'"
                        + (string != null ? " from preset '" + string + "'" : "")
                        + "; files: "
                        + this.listScriptFiles(script)
                        + "; it is disabled until set_script_enabled";
                }
            }
        );
    }

    private String deleteFile(JsonObject jsonObject) {
        Script script = this.resolveScript(jsonObject);
        Path path = this.resolveScriptPath(script, this.requireString(jsonObject, "path"));

        try {
            if (!Files.deleteIfExists(path)) {
                return "file did not exist";
            }
        } catch (IOException ioException) {
            throw new RuntimeException("delete failed: " + ioException.getMessage());
        }

        return "deleted " + this.requireString(jsonObject, "path") + "\n" + this.reloadScript(script);
    }

    private String checkSyntax(JsonObject jsonObject) {
        String string = this.requireString(jsonObject, "code");
        String string2 = this.optString(jsonObject, "filename", "main.js");
        ScriptRuntimeSandbox.SyntaxError syntaxError = ScriptRuntimeSandbox.checkSyntax(string, string2);
        if (syntaxError != null) {
            return "syntax error at line " + syntaxError.line() + ": " + syntaxError.message();
        }

        Set set = Set.of();
        if (jsonObject.has("script")) {
            Script script = this.resolveScript(jsonObject);
            set = this.collectKnownGlobals(script, null);
        }

        List<ScriptLinter.Diagnostic> diagnostics = ScriptLinter.lint(string, string2, set);
        if (diagnostics.isEmpty()) {
            return "ok — no syntax errors, no lint warnings";
        }

        StringBuilder builder = new StringBuilder("no syntax errors; lint warnings:");

        for (ScriptLinter.Diagnostic diagnostic : diagnostics) {
            builder.append("\n- line ").append(diagnostic.line()).append(": ").append(diagnostic.message());
        }

        return builder.toString();
    }

    private String reloadScriptTool(JsonObject jsonObject) {
        return this.reloadScript(this.resolveScript(jsonObject));
    }

    private String reloadScript(Script script) {
        return this.runOnClientThread(() -> {
            this.scriptManager.reloadScript(script);
            return script.hasError() ? "reload FAILED: " + script.getErrorMessage() : "reload: ok";
        });
    }

    private String setScriptEnabled(JsonObject jsonObject) {
        Script script = this.resolveScript(jsonObject);
        boolean flag = jsonObject.get("enabled").getAsBoolean();
        return this.runOnClientThread(
            () -> {
                script.setEnabled(flag);
                return script.hasError()
                    ? "script has an error and cannot run: " + script.getErrorMessage()
                    : "script '" + script.getDisplayName() + "' is now " + (script.isActive() ? "running" : "stopped");
            }
        );
    }

    private String setModuleEnabled(JsonObject jsonObject) {
        Script script = this.resolveScript(jsonObject);
        String string = this.requireString(jsonObject, "module");
        boolean flag = jsonObject.get("enabled").getAsBoolean();
        return this.runOnClientThread(
            () -> {
                for (ScriptModule scriptModule : script.getModules()) {
                    if (scriptModule.getDisplayName().equalsIgnoreCase(string)) {
                        scriptModule.setEnabled(flag);
                        return "module '"
                            + scriptModule.getDisplayName()
                            + "' "
                            + (flag ? "enabled" : "disabled")
                            + (script.isActive() ? "" : "; note: the script itself is not running");
                    }
                }

                ArrayList arrayList = new ArrayList();

                for (ScriptModule scriptModule2 : script.getModules()) {
                    arrayList.add(scriptModule2.getDisplayName());
                }

                throw new IllegalArgumentException("module not found: " + string + "; modules: " + arrayList);
            }
        );
    }

    private String getLogs(JsonObject jsonObject) {
        String string = jsonObject.has("script") ? jsonObject.get("script").getAsString() : null;
        int i = jsonObject.has("limit") ? jsonObject.get("limit").getAsInt() : 50;
        List<ScriptConsole.Entry> entries = ScriptConsole.getEntries(string, Math.clamp(i, 1, 300));
        if (entries.isEmpty()) {
            return "no log entries" + (string != null ? " for script '" + string + "'" : "");
        }

        StringBuilder builder = new StringBuilder();

        for (ScriptConsole.Entry entry : entries) {
            LocalTime localTime = LocalTime.ofInstant(Instant.ofEpochMilli(entry.time()), ZoneId.systemDefault());
            builder.append(String.format("%02d:%02d:%02d", localTime.getHour(), localTime.getMinute(), localTime.getSecond()))
                .append(" [")
                .append(entry.script())
                .append("] ")
                .append(entry.level())
                .append(": ")
                .append(entry.text())
                .append('\n');
        }

        return builder.toString();
    }

    private String runJs(JsonObject jsonObject) {
        Script script = this.resolveScript(jsonObject);
        String string = this.requireString(jsonObject, "code");
        return this.runOnClientThread(() -> {
            Object object;
            try {
                object = script.evaluate(string);
            } catch (RhinoException rhinoException) {
                throw new RuntimeException("JS error: " + rhinoException.getMessage());
            }

            return object != null && !(object instanceof Undefined) ? String.valueOf(object) : "undefined";
        });
    }

    private Script resolveScript(JsonObject jsonObject) {
        String string = this.requireString(jsonObject, "script");
        Script script = this.scriptManager.findScript(string);
        if (script != null && script.isPackaged()) {
            throw new IllegalArgumentException("script '" + script.getDisplayName() + "' is compiled and protected: its code and runtime are not accessible");
        }

        if (script != null) {
            return script;
        }

        ArrayList arrayList = new ArrayList();

        for (Script script2 : this.scriptManager.getScripts()) {
            arrayList.add(script2.getDisplayName());
        }

        throw new IllegalArgumentException("script not found: " + string + "; available: " + arrayList);
    }

    private Path resolveScriptPath(Script script, String string) {
        Path path = script.getAssetsRoot().toAbsolutePath().normalize();
        Path path2 = path.resolve(string.replace('\\', '/')).toAbsolutePath().normalize();
        if (path2.startsWith(path) && !path2.equals(path)) {
            return path2;
        } else {
            throw new IllegalArgumentException("path escapes the script folder: " + string);
        }
    }

    private String relativePath(Script script, Path path) {
        return script.getAssetsRoot().relativize(path).toString().replace('\\', '/');
    }

    private String requireString(JsonObject jsonObject, String string) {
        if (jsonObject.has(string) && !jsonObject.get(string).isJsonNull()) {
            return jsonObject.get(string).getAsString();
        } else {
            throw new IllegalArgumentException("missing argument: " + string);
        }
    }

    private String optString(JsonObject jsonObject, String string, String string2) {
        return jsonObject.has(string) && !jsonObject.get(string).isJsonNull() ? jsonObject.get(string).getAsString() : string2;
    }

    private <T> T runOnClientThread(Supplier<T> supplier) {
        if (mc.isOnThread()) {
            return (T)supplier.get();
        }

        CompletableFuture completableFuture = new CompletableFuture();
        mc.execute(() -> {
            try {
                completableFuture.complete(supplier.get());
            } catch (Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
            }
        });

        try {
            return (T)completableFuture.get(5L, TimeUnit.SECONDS);
        } catch (ExecutionException executionException) {
            Throwable throwable2 = executionException.getCause() == null ? executionException : executionException.getCause();
            throw new RuntimeException(String.valueOf(throwable2.getMessage()));
        } catch (Exception exception) {
            throw new RuntimeException("game thread did not respond in 5s (game frozen or busy)");
        }
    }

    record Tool(String description, JsonObject schema, Function<JsonObject, String> handler) {
    }
}
