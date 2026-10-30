package tech.sadnes.client.feature.impl.misc;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import tech.sadnes.api.mixin.accessor.BossBarHudAccessor;
import tech.sadnes.api.mixin.accessor.ChatHudAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.AutoAuthStorage;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ButtonSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(
    name = "AutoAuth",
    description = "feature.impl.misc.autoauth",
    displayName = "feature.impl.misc.autoauth",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public final class AutoAuth extends Feature {
    private static final Pattern LOGIN_PATTERN = Pattern.compile("/(login|log|auth|l)\\b");
    private static final Pattern REGISTER_PATTERN = Pattern.compile("/(register|reg|r)\\b");
    private static final String PASSWORD_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final TextSetting password = new TextSetting("autoauth.password", generateRandomPassword())
        .withMinLength(8)
        .withMaxLength(16)
        .setVisibleWhen(() -> !this.randomPassword.getValue() ? true : false);
    private final BooleanSetting randomPassword = new BooleanSetting("autoauth.randomPassword", false);
    private final Setting<Runnable> openStorage = new ButtonSetting("autoauth.openStorage", "autoauth.openStorage.button", AutoAuthStorage::openPreview);
    private final TimerUtil attemptTimer = new TimerUtil();
    private final TimerUtil joinTimer = new TimerUtil();
    private volatile String pendingPrompt;
    private String generatedPassword;
    private int attempts;

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                String string = gameMessageS2CPacket.content().getString().toLowerCase(Locale.ROOT);
                if (this.isAuthPrompt(string)) {
                    this.pendingPrompt = string;
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            if (this.attempts < 3 && this.attemptTimer.hasPassed(2500.0)) {
                String string = this.pendingPrompt;
                this.pendingPrompt = null;
                if (string == null) {
                    string = this.findPromptInBossBars();
                }

                if (string == null) {
                    string = this.findPromptInChat();
                }

                if (string != null) {
                    this.handleAuthPrompt(string);
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.reset();
        this.joinTimer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.reset();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.reset();
    }

    private void handleAuthPrompt(String string) {
        String string2 = AutoAuthStorage.getPassword(this.getServerKey(), this.getUsername());
        Matcher matcher2 = LOGIN_PATTERN.matcher(string);
        Matcher matcher3 = REGISTER_PATTERN.matcher(string);
        boolean flag = matcher3.find();
        boolean flag2;
        if (string2 == null) {
            flag2 = !flag;
        } else {
            flag2 = false;
        }

        if (flag2) {
            this.attemptTimer.reset();
        } else {
            boolean flag3;
            label43: {
                boolean flag4;
                if (matcher2.find()) {
                    if (string2 == null) {
                        flag3 = !flag;
                        break label43;
                    }

                    flag4 = true;
                } else {
                    flag4 = false;
                }

                flag3 = flag4;
            }

            if (flag3) {
                mc.getNetworkHandler().sendChatCommand(matcher2.group(1) + " " + string2);
            } else {
                String string3 = this.resolvePassword(string2);
                if (!ServerManager.isServer("funsky")) {
                    mc.getNetworkHandler().sendChatCommand(matcher3.group(1) + " " + string3 + " " + string3);
                } else {
                    mc.getNetworkHandler().sendChatCommand("register " + string3);
                }

                AutoAuthStorage.setPassword(this.getServerKey(), this.getUsername(), string3);
            }

            this.attempts++;
            this.attemptTimer.reset();
        }
    }

    private String resolvePassword(String string) {
        if (string != null) {
            return string;
        }

        if (this.generatedPassword == null) {
            this.generatedPassword = this.getConfiguredPassword();
        }

        return this.generatedPassword;
    }

    private String findPromptInBossBars() {
        if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            for (ClientBossBar clientBossBar : ((BossBarHudAccessor)mc.inGameHud.getBossBarHud()).sadnes$getBossBars().values()) {
                String string = clientBossBar.getName().getString();
                if (string != null && !string.isEmpty()) {
                    String string2 = string.toLowerCase(Locale.ROOT);
                    if (this.isAuthPrompt(string2)) {
                        return string2;
                    }
                }
            }
        }

        return null;
    }

    private String findPromptInChat() {
        boolean flag;
        if (this.attempts == 0) {
            flag = !this.joinTimer.hasPassed(15000.0);
        } else {
            flag = false;
        }

        if (flag && mc.inGameHud != null) {
            List list = ((ChatHudAccessor)mc.inGameHud.getChatHud()).sadnes$getVisibleMessages();
            int i = Math.min(list.size(), 30);
            StringBuilder builder = new StringBuilder();

            for (int i2 = 0; i2 < i; i2++) {
                builder.setLength(0);
                ((Visible)list.get(i2)).content().accept((i3, style, i4) -> {
                    builder.appendCodePoint(i4);
                    return true;
                });
                String string = builder.toString().toLowerCase(Locale.ROOT);
                if (this.isAuthPrompt(string)) {
                    return string;
                }
            }
        }

        return null;
    }

    private boolean isAuthPrompt(String string) {
        return LOGIN_PATTERN.matcher(string).find() || REGISTER_PATTERN.matcher(string).find();
    }

    private String getConfiguredPassword() {
        return this.randomPassword.getValue() ? generateRandomPassword() : this.password.getValue().trim();
    }

    private static String generateRandomPassword() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        int i = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".length();
        StringBuilder builder = new StringBuilder(12);

        for (int i2 = 0; i2 < 12; i2++) {
            builder.append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".charAt(threadLocalRandom.nextInt(i)));
        }

        return builder.toString();
    }

    private String getServerKey() {
        return mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null
            ? mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT)
            : "singleplayer";
    }

    private String getUsername() {
        return mc.getSession() != null ? mc.getSession().getUsername() : mc.player.getName().getString();
    }

    private void reset() {
        this.pendingPrompt = null;
        this.generatedPassword = null;
        this.attempts = 0;
        this.attemptTimer.setTime(0L);
        this.joinTimer.setTime(0L);
    }
}
