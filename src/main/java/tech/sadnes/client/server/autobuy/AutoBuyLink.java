package tech.sadnes.client.server.autobuy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoBuyLink {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBuyLink");
    private static final Gson GSON = new Gson();
    private static final int PORT = 47812;
    private static volatile ServerSocket serverSocket;

    private AutoBuyLink() {
    }

    public static boolean isHosting() {
        ServerSocket serverSocket2 = serverSocket;
        return serverSocket2 != null && !serverSocket2.isClosed();
    }

    public static void startServer(Function<AutoBuyLink.VerifyRequest, Boolean> function, Runnable runnable, AutoBuyLink.Handler handler) {
        if (!isHosting()) {
            try {
                serverSocket = new ServerSocket(47812, 4, InetAddress.getLoopbackAddress());
            } catch (Exception exception) {
                LOGGER.error("Port {} is busy — another session is already checking", 47812);
                return;
            }

            Thread thread = new Thread(() -> acceptLoop(function, runnable, handler), "sadnes-autobuy-link");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public static void stopServer() {
        ServerSocket serverSocket2 = serverSocket;
        serverSocket = null;
        if (serverSocket2 != null) {
            try {
                serverSocket2.close();
            } catch (Exception exception) {
            }
        }
    }

    private static void acceptLoop(Function<AutoBuyLink.VerifyRequest, Boolean> function, Runnable runnable, AutoBuyLink.Handler handler) {
        while (isHosting()) {
            ServerSocket serverSocket2 = serverSocket;

            try (
                Socket socket = serverSocket2.accept();
                BufferedReader bufferedReader = openReader(socket);
                PrintWriter printWriter = openWriter(socket);
            ) {
                String string = bufferedReader.readLine();
                if (string != null) {
                    printWriter.println(GSON.toJson(handleRequest(GSON.fromJson(string, JsonObject.class), function, runnable, handler)));
                }
            } catch (Exception exception) {
                if (isHosting()) {
                    LOGGER.error("Verification request failed", exception);
                }
            }
        }
    }

    private static JsonObject handleRequest(JsonObject jsonObject, Function<AutoBuyLink.VerifyRequest, Boolean> function, Runnable runnable, AutoBuyLink.Handler handler) {
        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("ok", true);
        if (jsonObject.has("ping")) {
            return jsonObject2;
        }

        if (jsonObject.has("hop")) {
            runnable.run();
            return jsonObject2;
        }

        if (jsonObject.has("scan")) {
            ArrayList arrayList = new ArrayList();

            for (JsonElement jsonElement : jsonObject.getAsJsonArray("scan")) {
                arrayList.add(jsonElement.getAsString());
            }

            handler.onScan(arrayList);
            return jsonObject2;
        } else {
            if (!jsonObject.has("prices")) {
                jsonObject2.addProperty(
                    "ok",
                    Boolean.TRUE
                        .equals(
                            function.apply(
                                new AutoBuyLink.VerifyRequest(jsonObject.get("entry").getAsString(), jsonObject.get("price").getAsInt(), jsonObject.get("seller").getAsString())
                            )
                        )
                );
                return jsonObject2;
            }

            JsonObject jsonObject3 = new JsonObject();

            for (Entry entry : handler.getPrices().entrySet()) {
                jsonObject3.addProperty((String)entry.getKey(), (Number)entry.getValue());
            }

            jsonObject2.add("prices", jsonObject3);
            return jsonObject2;
        }
    }

    public static boolean ping() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ping", true);
        return Boolean.TRUE.equals(sendForOk(jsonObject));
    }

    public static void sendHop() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("hop", true);
        CompletableFuture.runAsync(() -> sendForOk(jsonObject));
    }

    public static void sendScan(List<String> strings) {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for (String string : strings) {
            jsonArray.add(string);
        }

        jsonObject.add("scan", jsonArray);
        CompletableFuture.runAsync(() -> sendForOk(jsonObject));
    }

    public static Map<String, Integer> requestPrices() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("prices", true);
        JsonObject jsonObject2 = sendRequest(jsonObject);
        if (jsonObject2 != null && jsonObject2.has("prices")) {
            for (Entry entry : jsonObject2.getAsJsonObject("prices").entrySet()) {
                linkedHashMap.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsInt());
            }
        }

        return linkedHashMap;
    }

    public static CompletableFuture<Boolean> requestVerification(AutoBuyLink.VerifyRequest verifyRequest) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("entry", verifyRequest.entry());
        jsonObject.addProperty("price", verifyRequest.price());
        jsonObject.addProperty("seller", verifyRequest.seller());
        return CompletableFuture.supplyAsync(() -> sendForOk(jsonObject));
    }

    private static Boolean sendForOk(JsonObject jsonObject) {
        JsonObject jsonObject2 = sendRequest(jsonObject);
        if (jsonObject2 == null) {
            return null;
        } else {
            return jsonObject2.has("ok") && jsonObject2.get("ok").getAsBoolean() ? true : false;
        }
    }

    private static JsonObject sendRequest(JsonObject jsonObject) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), 47812), 300);
            socket.setSoTimeout(10000);
            openWriter(socket).println(GSON.toJson(jsonObject));
            String string = openReader(socket).readLine();
            return string == null ? null : GSON.fromJson(string, JsonObject.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private static BufferedReader openReader(Socket socket) throws Exception {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private static PrintWriter openWriter(Socket socket) throws Exception {
        return new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
    }

    public interface Handler {
        void onScan(List<String> strings);

        Map<String, Integer> getPrices();
    }

    public record VerifyRequest(String entry, int price, String seller) {
    }
}
