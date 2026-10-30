package tech.sadnes.client.feature.impl.misc;

import baritone.api.BaritoneAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import local.sadnes.OfflineRouting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import tech.sadnes.api.mixin.accessor.ChatHudAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "ChatHelper",
    description = "feature.impl.misc.chathelper",
    displayName = "feature.impl.misc.chathelper",
    category = Category.MISC,
    subcategory = Subcategory.MISC_CHAT
)
public final class ChatHelper extends Feature {
    private static final String[] LANGUAGES = new String[]{"ru", "en", "pl", "kk", "uk"};
    private static final int MAX_CACHE_SIZE = 256;
    private static final int MAX_PENDING_REQUESTS = 8;
    private static final long REQUEST_INTERVAL_MS = 150L;
    private static final long RATE_LIMIT_COOLDOWN_MS = 60000L;
    private static final ExecutorService TRANSLATOR_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sadnes-chat-translator");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).followRedirects(Redirect.NORMAL).build();
    private static final Map<String, String> TRANSLATION_CACHE = new LinkedHashMap<String, String>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Entry<String, String> entry) {
            return this.size() > 256;
        }
    };
    private static final AtomicInteger PENDING_REQUESTS = new AtomicInteger();
    private static long lastRequestTime;
    private static long rateLimitedUntil;
    public final MultiSetting features = new MultiSetting(
        "chathelper.features",
        BooleanSetting.of("infinite", true),
        BooleanSetting.of("compact", true),
        BooleanSetting.of("copy", true),
        BooleanSetting.of("mentions", true)
    );
    private final GroupSetting translator = new GroupSetting("chathelper.translator", false);
    private final MultiSetting translateDirection = new MultiSetting(
        this.translator, "chathelper.translateDirection", BooleanSetting.of("incoming", true), BooleanSetting.of("outgoing", false)
    );
    private final ModeSetting incomingLanguage = new ModeSetting(this.translator, "chathelper.incomingLanguage", "ru", LANGUAGES)
        .setVisibleWhen(() -> this.translateDirection.isSelected("incoming"));
    private final BooleanSetting translateSystem = new BooleanSetting(this.translator, "chathelper.translateSystem", false)
        .setVisibleWhen(() -> this.translateDirection.isSelected("incoming"));
    private final ModeSetting outgoingLanguage = new ModeSetting(this.translator, "chathelper.outgoingLanguage", "en", LANGUAGES)
        .setVisibleWhen(() -> this.translateDirection.isSelected("outgoing"));
    private static int hoveredLineIndex = -1;
    private static long hoveredLineTime;
    private static long lastClickTime;
    private String lastTranslatedOutgoing;

    public static ChatHelper getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(ChatHelper.class);
    }

    public static boolean isInfiniteEnabled() {
        return isFeatureSelected("infinite");
    }

    public static boolean isCompactEnabled() {
        return isFeatureSelected("compact");
    }

    public static boolean isCopyEnabled() {
        return isFeatureSelected("copy");
    }

    public static boolean isMentionsEnabled() {
        return isFeatureSelected("mentions");
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (packetEvent.isReceive() && this.translator.getValue() && minecraftClient.player != null) {
            if (this.translateDirection.isSelected("incoming")) {
                this.translateIncoming(packetEvent);
            }
        }
    }

    public static boolean handleOutgoingMessage(String string) {
        ChatHelper chatHelper = getInstance();
        if (chatHelper != null && chatHelper.isEnabled() && chatHelper.translator.getValue()) {
            if (!chatHelper.translateDirection.isSelected("outgoing")) {
                return false;
            }

            if (string.equals(chatHelper.lastTranslatedOutgoing)) {
                chatHelper.lastTranslatedOutgoing = null;
                return false;
            }

            boolean flag = string.startsWith(BaritoneAPI.getSettings().prefix.value);
            if (!stripFormatting(string).isEmpty() && !string.startsWith("/") && !string.startsWith(".")) {
                if (flag) {
                    return false;
                }

                return translateAsync(string, chatHelper.outgoingLanguage.getValue(), string2 -> {
                    ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
                    if (clientPlayNetworkHandler != null) {
                        if (string2.length() > 256) {
                            chatHelper.lastTranslatedOutgoing = string2.substring(0, 256);
                            clientPlayNetworkHandler.sendChatMessage(chatHelper.lastTranslatedOutgoing);
                        } else {
                            chatHelper.lastTranslatedOutgoing = string2;
                            clientPlayNetworkHandler.sendChatMessage(chatHelper.lastTranslatedOutgoing);
                        }
                    }
                });
            }
        }

        return false;
    }

    private void translateIncoming(PacketEvent packetEvent) {
        String string;
        boolean flag;
        if (packetEvent.getPacket() instanceof ChatMessageS2CPacket chatMessageS2CPacket) {
            Text text = chatMessageS2CPacket.unsignedContent();
            string = text != null ? text.getString() : chatMessageS2CPacket.body().content();
            flag = true;
        } else {
            if (!(packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) || gameMessageS2CPacket.overlay()) {
                return;
            }

            string = gameMessageS2CPacket.content().getString();
            flag = false;
        }

        String string2 = stripFormatting(string);
        if (!string2.isEmpty() && string2.length() <= 512) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            minecraftClient.execute(() -> {
                if (flag || this.translateSystem.getValue() || hasPlayerSenderPrefix(string2, "")) {
                    translateAsync(string2, this.incomingLanguage.getValue(), string3 -> {
                        if (this.isEnabled() && this.translator.getValue() && !string3.equalsIgnoreCase(string2) && minecraftClient.inGameHud != null) {
                            minecraftClient.inGameHud.getChatHud().addMessage(Text.literal("⇄ " + string3).formatted(Formatting.GRAY));
                        }
                    });
                }
            });
        }
    }

    private static String stripFormatting(String string) {
        int i = string.length();
        StringBuilder builder = new StringBuilder(i);
        boolean flag = false;

        for (int i2 = 0; i2 < i; i2++) {
            char c = string.charAt(i2);
            if (c == 167 && i2 + 1 < i) {
                i2++;
            } else {
                if (Character.isLetter(c)) {
                    flag = true;
                }

                builder.append(c);
            }
        }

        return flag ? builder.toString().trim() : "";
    }

    private static boolean translateAsync(String string, String string2, Consumer<String> consumer) {
        if (PENDING_REQUESTS.get() >= 8) {
            return false;
        }

        PENDING_REQUESTS.incrementAndGet();
        TRANSLATOR_EXECUTOR.execute(() -> {
            String string3 = translate(string, string2);
            PENDING_REQUESTS.decrementAndGet();
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().player != null) {
                    consumer.accept(string3);
                }
            });
        });
        return true;
    }

    private static String translate(String string, String string2) {
        String string3 = string2 + "\u0000" + string;
        String string4 = TRANSLATION_CACHE.get(string3);
        if (string4 != null) {
            return string4;
        }

        long l = System.currentTimeMillis();
        if (l < rateLimitedUntil) {
            return string;
        }

        long l2 = 150L - (l - lastRequestTime);
        if (l2 > 0L) {
            try {
                Thread.sleep(l2);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return string;
            }
        }

        lastRequestTime = System.currentTimeMillis();

        try {
            URI uri = URI.create(
                "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=auto&tl=" + string2 + "&q=" + URLEncoder.encode(string, StandardCharsets.UTF_8)
            );
            HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(uri)).timeout(Duration.ofSeconds(6L)).GET().build();
            HttpResponse httpResponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
            String string5 = ((String)httpResponse.body()).trim();
            int i = httpResponse.statusCode();
            if (i == 429 || i == 403 || string5.isEmpty() || string5.charAt(0) != '[') {
                rateLimitedUntil = System.currentTimeMillis() + 60000L;
                return string;
            }

            if (i != 200) {
                return string;
            }

            String string6 = parseTranslation(string5, string, string2);
            TRANSLATION_CACHE.put(string3, string6);
            return string6;
        } catch (InterruptedException interruptedException2) {
            Thread.currentThread().interrupt();
            return string;
        } catch (Exception exception) {
            return string;
        }
    }

    private static String parseTranslation(String string, String string2, String string3) {
        JsonElement jsonElement = JsonParser.parseString(string);
        if (!jsonElement.isJsonArray()) {
            return string2;
        }

        JsonArray jsonArray = jsonElement.getAsJsonArray();
        int i = jsonArray.size();
        StringBuilder builder = new StringBuilder(string2.length());

        for (int i2 = 0; i2 < i; i2++) {
            JsonElement jsonElement2 = jsonArray.get(i2);
            if (jsonElement2.isJsonPrimitive()) {
                builder.append(jsonElement2.getAsString());
            } else if (jsonElement2.isJsonArray() && !jsonElement2.getAsJsonArray().isEmpty()) {
                JsonArray jsonArray2 = jsonElement2.getAsJsonArray();
                JsonElement jsonElement3 = jsonArray2.size() > 1 ? jsonArray2.get(1) : null;
                if (jsonElement3 != null && jsonElement3.isJsonPrimitive() && string3.equalsIgnoreCase(jsonElement3.getAsString())) {
                    return string2;
                }

                JsonElement jsonElement4 = jsonArray2.get(0);
                if (jsonElement4.isJsonPrimitive()) {
                    builder.append(jsonElement4.getAsString());
                }
            }
        }

        String string4 = builder.toString().trim();
        return string4.isEmpty() ? string2 : string4;
    }

    public static boolean isMention(OrderedText orderedText, int i) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player == null) {
            return false;
        } else {
            String string = minecraftClient.player.getName().getString();
            if (!string.isEmpty() && containsIgnoreCase(orderedTextToString(orderedText), string)) {
                String string2 = getChatLineText(i);
                return string2 != null && hasPlayerSenderPrefix(string2, string);
            } else {
                return false;
            }
        }
    }

    private static boolean hasPlayerSenderPrefix(String string, String string2) {
        int i = string.length();
        int i2 = 0;

        while (i2 < i) {
            char c = string.charAt(i2);
            switch (c) {
                case ':':
                case '>':
                case '»':
                case '›':
                    if ((i2 + 1 >= i || string.charAt(i2 + 1) == ' ') && containsOtherPlayerName(string.substring(0, i2), string2)) {
                        return true;
                    }
                default:
                    i2++;
            }
        }

        return false;
    }

    private static boolean containsOtherPlayerName(String string, String string2) {
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (clientPlayNetworkHandler == null) {
            return false;
        }

        for (PlayerListEntry playerListEntry : clientPlayNetworkHandler.getPlayerList()) {
            String string3 = playerListEntry.getProfile().name();
            if (string3 != null && !string3.isEmpty() && !string3.equalsIgnoreCase(string2) && containsIgnoreCase(string, string3)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsIgnoreCase(String string, String string2) {
        int i = string.length() - string2.length();

        for (int i2 = 0; i2 <= i; i2++) {
            if (string.regionMatches(true, i2, string2, 0, string2.length())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isDoubleClick() {
        long l = System.currentTimeMillis();
        if (l - lastClickTime < 200L) {
            lastClickTime = 0L;
            return true;
        } else {
            lastClickTime = l;
            return false;
        }
    }

    public static void setHoveredLine(int i) {
        hoveredLineIndex = i;
        hoveredLineTime = System.currentTimeMillis();
    }

    public static String getHoveredLineText() {
        return hoveredLineIndex >= 0 && System.currentTimeMillis() - hoveredLineTime <= 250L ? getChatLineText(hoveredLineIndex) : null;
    }

    private static String getChatLineText(int i) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.inGameHud != null && i >= 0) {
            ChatHudAccessor chatHudAccessor = (ChatHudAccessor)minecraftClient.inGameHud.getChatHud();
            List list = chatHudAccessor.sadnes$getVisibleMessages();
            int i2 = i + chatHudAccessor.sadnes$getScrolledLines();
            if (i2 >= 0) {
                if (i2 >= list.size()) {
                    return null;
                }

                int i3 = i2;

                while (i3 > 0 && !((Visible)list.get(i3)).endOfEntry()) {
                    i3--;
                }

                int i4 = i3;
                int i5 = list.size();

                while (i4 + 1 < i5 && !((Visible)list.get(i4 + 1)).endOfEntry()) {
                    i4++;
                }

                StringBuilder builder = new StringBuilder();

                for (int i6 = i4; i6 >= i3; i6--) {
                    builder.append(orderedTextToString(((Visible)list.get(i6)).content()));
                }

                String string = builder.toString().trim();
                if (string.isEmpty()) {
                    return null;
                }

                return string;
            }
        }

        return null;
    }

    private static String orderedTextToString(OrderedText orderedText) {
        StringBuilder builder = new StringBuilder();
        orderedText.accept((i, style, i2) -> {
            builder.appendCodePoint(i2);
            return true;
        });
        return builder.toString();
    }

    private static boolean isFeatureSelected(String string) {
        ChatHelper chatHelper = getInstance();
        return chatHelper != null && chatHelper.isEnabled() && chatHelper.features.has(string);
    }
}
