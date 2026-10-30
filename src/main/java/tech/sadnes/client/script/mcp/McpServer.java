package tech.sadnes.client.script.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.script.ScriptManager;

public final class McpServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("sadnes/MCP");
    private static final String PROTOCOL_VERSION = "2025-06-18";
    private static final Set<String> SUPPORTED_VERSIONS = Set.of("2024-11-05", "2025-03-26", "2025-06-18");
    private static final int MAX_BODY_SIZE = 4194304;
    private static final String INSTRUCTIONS = "You are connected to a LIVE running sadnes Minecraft client. The tools manage its JavaScript scripts at runtime. Start with get_api_docs — the scripting API is custom to this client, do not guess it. write_file saves and hot-reloads immediately and returns compile/lint errors, so always read its result. get_logs shows print() output and runtime errors. run_js executes code in a live script scope for debugging.";
    private final ScriptManager scriptManager;
    private final ScriptMcpTools tools;
    private final Path configPath;
    private HttpServer server;
    private ExecutorService executor;
    private int basePort = 25599;
    private int port;
    private boolean enabled;

    public McpServer(ScriptManager scriptManager2) {
        this.scriptManager = scriptManager2;
        this.tools = new ScriptMcpTools(scriptManager2);
        this.configPath = scriptManager2.getScriptsDir().getParent().resolve("mcp.json");
        this.loadConfig();
    }

    public void startIfEnabled() {
        if (this.enabled) {
            this.start();
        }
    }

    public boolean isRunning() {
        return this.server != null;
    }

    public String getUrl() {
        return "http://127.0.0.1:" + this.port + "/mcp";
    }

    public boolean toggle() {
        if (this.isRunning()) {
            this.stop();
        } else {
            this.start();
        }

        this.enabled = this.isRunning();
        this.saveConfig();
        return this.isRunning();
    }

    public boolean start() {
        if (this.server != null) {
            return true;
        }

        for (int i = this.basePort; i < this.basePort + 10; i++) {
            try {
                HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), i), 0);
                this.executor = Executors.newFixedThreadPool(2, runnable -> {
                    Thread thread = new Thread(runnable, "sadnes-mcp");
                    thread.setDaemon(true);
                    return thread;
                });
                httpServer.createContext("/mcp", this::handleHttpExchange);
                httpServer.setExecutor(this.executor);
                httpServer.start();
                this.server = httpServer;
                this.port = i;
                McpWorkspaceWriter.writeWorkspaceFiles(this.scriptManager.getScriptsDir(), this.getUrl());
                LOGGER.info("MCP server listening on {}", this.getUrl());
                return true;
            } catch (IOException ioException) {
            }
        }

        LOGGER.warn("MCP server failed to start: ports {}..{} busy", this.basePort, this.basePort + 9);
        return false;
    }

    public void stop() {
        if (this.server != null) {
            this.server.stop(0);
            this.server = null;
            if (this.executor != null) {
                this.executor.shutdownNow();
            }

            this.executor = null;
            LOGGER.info("MCP server stopped");
        }
    }

    private void handleHttpExchange(HttpExchange httpExchange) {
        try {
            label82: {
                label70: {
                    String string = httpExchange.getRequestMethod();
                    byte b = -1;
                    switch (string.hashCode()) {
                        case -531492226:
                            if (string.equals("OPTIONS")) {
                                b = 2;
                            }
                            break;
                        case 2461856:
                            if (string.equals("POST")) {
                                break label82;
                            }
                            break;
                        case 2012838315:
                            if (string.equals("DELETE")) {
                                break label70;
                            }
                    }

                    switch (b) {
                        case 0:
                            break label82;
                        case 1:
                            break;
                        case 2:
                            this.sendStatus(httpExchange, 204);
                            return;
                        default:
                            this.sendStatus(httpExchange, 405);
                            return;
                    }
                }

                this.sendStatus(httpExchange, 200);
                return;
            }

            this.handlePost(httpExchange);
        } catch (Exception exception) {
            LOGGER.warn("MCP request failed: {}", exception.toString());

            try {
                this.sendStatus(httpExchange, 500);
            } catch (IOException ioException) {
            }
        } finally {
            httpExchange.close();
        }
    }

    private void handlePost(HttpExchange httpExchange) throws IOException {
        byte[] bytes = httpExchange.getRequestBody().readNBytes(4194305);
        if (bytes.length > 4194304) {
            this.sendStatus(httpExchange, 413);
        } else {
            JsonElement jsonElement;
            try {
                jsonElement = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            } catch (Exception exception) {
                this.sendJson(httpExchange, this.createError(null, -32700, "parse error").toString());
                return;
            }

            JsonElement jsonElement2;
            if (jsonElement.isJsonArray()) {
                JsonArray jsonArray = new JsonArray();

                for (JsonElement jsonElement3 : jsonElement.getAsJsonArray()) {
                    JsonObject jsonObject = jsonElement3.isJsonObject() ? this.handleRequest(jsonElement3.getAsJsonObject()) : null;
                    if (jsonObject != null) {
                        jsonArray.add(jsonObject);
                    }
                }

                jsonElement2 = jsonArray.isEmpty() ? null : jsonArray;
            } else {
                jsonElement2 = this.handleRequest(jsonElement.getAsJsonObject());
            }

            if (jsonElement2 == null) {
                this.sendStatus(httpExchange, 202);
            } else {
                this.sendJson(httpExchange, jsonElement2.toString());
            }
        }
    }

    private JsonObject handleRequest(JsonObject jsonObject) {
        JsonElement jsonElement = jsonObject.get("id");
        if (jsonElement == null) {
            return null;
        }

        String string = jsonObject.has("method") ? jsonObject.get("method").getAsString() : "";
        JsonObject jsonObject2 = jsonObject.has("params") && jsonObject.get("params").isJsonObject() ? jsonObject.getAsJsonObject("params") : new JsonObject();

        try {
            JsonObject jsonObject3;
            label56: {
                label55: {
                    label54: {
                        label53: {
                            byte b = -1;
                            switch (string.hashCode()) {
                                case 3441010:
                                    if (string.equals("ping")) {
                                        break label54;
                                    }
                                    break;
                                case 498659858:
                                    if (string.equals("tools/call")) {
                                        b = 3;
                                    }
                                    break;
                                case 498935890:
                                    if (string.equals("tools/list")) {
                                        break label53;
                                    }
                                    break;
                                case 871091088:
                                    if (string.equals("initialize")) {
                                        break label55;
                                    }
                            }

                            switch (b) {
                                case 0:
                                    break label55;
                                case 1:
                                    break label54;
                                case 2:
                                    break;
                                case 3:
                                    jsonObject3 = this.handleToolCall(jsonObject2);
                                    break label56;
                                default:
                                    jsonObject3 = null;
                                    break label56;
                            }
                        }

                        JsonObject jsonObject4 = new JsonObject();
                        jsonObject4.add("tools", this.tools.listTools());
                        jsonObject3 = jsonObject4;
                        break label56;
                    }

                    jsonObject3 = new JsonObject();
                    break label56;
                }

                jsonObject3 = this.handleInitialize(jsonObject2);
            }

            JsonObject jsonObject5 = jsonObject3;
            return jsonObject5 == null ? this.createError(jsonElement, -32601, "method not found: " + string) : this.createResult(jsonElement, jsonObject5);
        } catch (Exception exception) {
            return this.createError(jsonElement, -32603, String.valueOf(exception.getMessage()));
        }
    }

    private JsonObject handleInitialize(JsonObject jsonObject) {
        String string = jsonObject.has("protocolVersion") ? jsonObject.get("protocolVersion").getAsString() : "2025-06-18";
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("protocolVersion", SUPPORTED_VERSIONS.contains(string) ? string : "2025-06-18");
        JsonObject jsonObject3 = new JsonObject();
        jsonObject3.add("tools", new JsonObject());
        jsonObject2.add("capabilities", jsonObject3);
        JsonObject jsonObject4 = new JsonObject();
        jsonObject4.addProperty("name", "sadnes-scripts");
        jsonObject4.addProperty("version", "1.0.0");
        jsonObject2.add("serverInfo", jsonObject4);
        jsonObject2.addProperty(
            "instructions",
            "You are connected to a LIVE running sadnes Minecraft client. The tools manage its JavaScript scripts at runtime. Start with get_api_docs — the scripting API is custom to this client, do not guess it. write_file saves and hot-reloads immediately and returns compile/lint errors, so always read its result. get_logs shows print() output and runtime errors. run_js executes code in a live script scope for debugging."
        );
        return jsonObject2;
    }

    private JsonObject handleToolCall(JsonObject jsonObject) {
        String string = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "";
        JsonObject jsonObject2 = jsonObject.has("arguments") && jsonObject.get("arguments").isJsonObject() ? jsonObject.getAsJsonObject("arguments") : new JsonObject();
        JsonObject jsonObject3 = new JsonObject();
        jsonObject3.addProperty("type", "text");
        JsonObject jsonObject4 = new JsonObject();

        try {
            jsonObject3.addProperty("text", this.tools.callTool(string, jsonObject2));
            jsonObject4.addProperty("isError", false);
        } catch (Throwable throwable) {
            jsonObject3.addProperty("text", "Error: " + (throwable.getMessage() == null ? throwable.toString() : throwable.getMessage()));
            jsonObject4.addProperty("isError", true);
        }

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject3);
        jsonObject4.add("content", jsonArray);
        return jsonObject4;
    }

    private JsonObject createResult(JsonElement jsonElement, JsonObject jsonObject) {
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("jsonrpc", "2.0");
        jsonObject2.add("id", jsonElement);
        jsonObject2.add("result", jsonObject);
        return jsonObject2;
    }

    private JsonObject createError(JsonElement jsonElement, int i, String string) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("code", i);
        jsonObject.addProperty("message", string);
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("jsonrpc", "2.0");
        jsonObject2.add("id", jsonElement);
        jsonObject2.add("error", jsonObject);
        return jsonObject2;
    }

    private void sendStatus(HttpExchange httpExchange, int i) throws IOException {
        httpExchange.sendResponseHeaders(i, -1L);
    }

    private void sendJson(HttpExchange httpExchange, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        httpExchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        httpExchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void loadConfig() {
        try {
            if (!Files.isRegularFile(this.configPath)) {
                return;
            }

            JsonObject jsonObject = JsonParser.parseString(Files.readString(this.configPath)).getAsJsonObject();
            if (jsonObject.has("enabled")) {
                this.enabled = jsonObject.get("enabled").getAsBoolean();
            }

            if (jsonObject.has("port")) {
                this.basePort = jsonObject.get("port").getAsInt();
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to read mcp.json: {}", exception.toString());
        }
    }

    private void saveConfig() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("enabled", this.enabled);
        jsonObject.addProperty("port", this.basePort);

        try {
            Files.createDirectories(this.configPath.getParent());
            Files.writeString(this.configPath, jsonObject.toString());
        } catch (IOException ioException) {
            LOGGER.warn("Failed to write mcp.json: {}", ioException.toString());
        }
    }
}
