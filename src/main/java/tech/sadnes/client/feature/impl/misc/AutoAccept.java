package tech.sadnes.client.feature.impl.misc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "AutoAccept",
    description = "feature.impl.misc.autoaccept",
    displayName = "feature.impl.misc.autoaccept",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SOCIAL
)
public final class AutoAccept extends Feature {
    private final MultiSetting options = new MultiSetting("autoaccept.options", BooleanSetting.of("clan", true), BooleanSetting.of("teleport", false));
    private static final Pattern CLAN_INVITE_PATTERN = Pattern.compile(
        "(?:(?:\\[[^\\]]+\\]|\\u2694)\\s*)*([A-Za-z0-9_]{1,16})\\s+приглашает\\s+[Вв]ас\\s+в\\s+клан\\s+([\\p{L}0-9_\\-]+)"
    );
    private static final Pattern TELEPORT_REQUEST_PATTERN = Pattern.compile("([A-Za-z0-9_]{1,16})\\s+(?:хочет|просит)\\s+телепортироваться\\s+к\\s+[Вв]ам");
    private static final Pattern DONUT_TPA_PATTERN = Pattern.compile(
        "([A-Za-z0-9_]{1,16})\\s+(?:отправил\\s+тебе\\s+запрос\\s+на\\s+телепортацию|sent\\s+you\\s+a\\s+(?:tpa|teleport)\\s+request)", 2
    );
    public final BooleanSetting onlyFriends = new BooleanSetting("autoaccept.onlyFriends", false).setVisibleWhen(() -> this.options.isSelected("teleport"));
    public final BooleanSetting onlyClanFriends = new BooleanSetting("autoaccept.onlyClanFriends", false).setVisibleWhen(() -> this.options.isSelected("clan"));
    private final Map<String, Long> recentTeleportRequests = new HashMap<>();
    private long confirmDeadline;

    public static AutoAccept getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AutoAccept.class);
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && mc.player != null && mc.getNetworkHandler() != null) {
            Text text2;
            String string;
            if (packetEvent.getPacket() instanceof OverlayMessageS2CPacket overlayMessageS2CPacket) {
                text2 = overlayMessageS2CPacket.text();
                string = text2.getString();
            } else if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                text2 = gameMessageS2CPacket.content();
                string = text2.getString();
            } else {
                if (!(packetEvent.getPacket() instanceof ChatMessageS2CPacket chatMessageS2CPacket)) {
                    return;
                }

                string = chatMessageS2CPacket.body().content();
                text2 = chatMessageS2CPacket.unsignedContent();
            }

            if (string != null) {
                if (string.isEmpty()) {
                    return;
                }

                String string2 = stripFormatting(string);
                if (this.options.isSelected("teleport") && ServerManager.isDonut()) {
                    Matcher matcher2 = DONUT_TPA_PATTERN.matcher(string2);
                    if (matcher2.find()) {
                        String string3 = matcher2.group(1);
                        if (!this.onlyFriends.getValue() || Sadnes.getInstance().getFriendManager().isFriend(string3)) {
                            mc.execute(() -> this.acceptTeleport(string3));
                            return;
                        }

                        return;
                    }
                }

                if (this.options.isSelected("teleport")) {
                    Matcher matcher3 = TELEPORT_REQUEST_PATTERN.matcher(string2);
                    if (matcher3.find()) {
                        if (!this.onlyFriends.getValue() || Sadnes.getInstance().getFriendManager().isFriend(matcher3.group(1))) {
                            mc.getNetworkHandler().sendChatCommand("tpaccept");
                            return;
                        }

                        return;
                    }
                }

                if (this.options.isSelected("clan")) {
                    Matcher matcher4 = CLAN_INVITE_PATTERN.matcher(string2);
                    if (matcher4.find()) {
                        String string4 = matcher4.group(1);
                        if (this.onlyClanFriends.getValue() && !Sadnes.getInstance().getFriendManager().isFriend(string4)) {
                            return;
                        }

                        matcher4.group(2);
                        String string5 = findAcceptCommand(text2);
                        if (string5 != null) {
                            this.runAcceptCommand(string5);
                            return;
                        }

                        mc.getNetworkHandler().sendChatCommand("clan accept " + string4);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.confirmDeadline != 0L && mc.player != null && mc.interactionManager != null) {
            if (System.currentTimeMillis() > this.confirmDeadline) {
                this.confirmDeadline = 0L;
                return;
            }

            if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                String string = genericContainerScreen.getTitle().getString().toLowerCase(Locale.ROOT);
                if (!string.contains("принять") && !string.contains("accept")) {
                    return;
                }

                GenericContainerScreenHandler genericContainerScreenHandler = genericContainerScreen.getScreenHandler();
                int i = genericContainerScreenHandler.getRows() * 9;

                for (int i2 = 0; i2 < i; i2++) {
                    String string2 = Registries.ITEM.getId(genericContainerScreenHandler.getSlot(i2).getStack().getItem()).getPath();
                    if (string2.contains("lime") || string2.contains("green")) {
                        mc.interactionManager.clickSlot(genericContainerScreenHandler.syncId, i2, 0, SlotActionType.PICKUP, mc.player);
                        this.confirmDeadline = 0L;
                        return;
                    }
                }
            }
        }
    }

    private void acceptTeleport(String string) {
        if (mc.getNetworkHandler() != null) {
            long l = System.currentTimeMillis();
            this.recentTeleportRequests.values().removeIf(longValue -> l - longValue > 5000L);
            if (!this.recentTeleportRequests.containsKey(string)) {
                this.recentTeleportRequests.put(string, l);
                this.confirmDeadline = l + 5000L;
                mc.getNetworkHandler().sendChatCommand("tpaccept");
            }
        }
    }

    private void runAcceptCommand(String string) {
        if (string != null && mc.getNetworkHandler() != null) {
            String string2 = string.trim();
            if (!string2.isEmpty()) {
                if (string2.startsWith("/")) {
                    mc.getNetworkHandler().sendChatCommand(string2.substring(1));
                } else {
                    mc.getNetworkHandler().sendChatMessage(string2);
                }
            }
        }
    }

    private static String findAcceptCommand(Text text2) {
        if (text2 == null) {
            return null;
        }

        String[] strings = new String[1];
        text2.visit(
            (style, string) -> {
                ClickEvent clickEvent = style.getClickEvent();
                if (clickEvent != null) {
                    String string2 = getClickCommand(clickEvent);
                    if (string2 != null) {
                        String string3 = string2.toLowerCase(Locale.ROOT);
                        String string4 = string.toLowerCase(Locale.ROOT);
                        if (!string3.contains("deny")
                            && !string3.contains("decline")
                            && !string3.contains("отклон")
                            && (string3.contains("accept") || string3.contains("join") || string4.contains("принять") || string4.contains("вступить"))) {
                            strings[0] = string2;
                            return Optional.of(string2);
                        }
                    }
                }

                return Optional.empty();
            },
            Style.EMPTY
        );
        return strings[0];
    }

    private static String getClickCommand(ClickEvent clickEvent) {
        if (clickEvent == null) {
            return null;
        }

        return switch (clickEvent) {
            case ClickEvent.RunCommand(String command) -> command;
            case ClickEvent.SuggestCommand(String command) -> command;
            default -> null;
        };
    }

    private static String stripFormatting(String string) {
        return string == null
            ? ""
            : string.replaceAll("(?i)§[0-9a-fk-or]", "").replaceAll("[\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]", " ");
    }
}
