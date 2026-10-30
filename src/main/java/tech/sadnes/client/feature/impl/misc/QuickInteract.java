package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.combat.AntiBot;
import tech.sadnes.client.friend.FriendManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.popup.QuickActionsPopup;
import tech.sadnes.client.ui.clickgui.popup.QuickInteractPopup;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "QuickInteract",
    description = "feature.impl.misc.quickinteract",
    displayName = "feature.impl.misc.quickinteract",
    category = Category.MISC,
    subcategory = Subcategory.MISC_CHAT
)
public final class QuickInteract extends Feature {
    public final BooleanSetting customPhrases = new BooleanSetting("quickinteract.custom phrases", false);
    public final TextSetting duelCommand = new TextSetting("quickinteract.duel command", "duel").setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting reportCommand = new TextSetting("quickinteract.report command", "report").setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting cancelText = new TextSetting("quickinteract.cancel text", "Отменить вызов").setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting acceptText = new TextSetting("quickinteract.accept text", "Принять").setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting declineText = new TextSetting("quickinteract.decline text", "Отказать").setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting teleportText = new TextSetting("quickinteract.teleport text", "Отменить телепортацию")
        .setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting startedText = new TextSetting("quickinteract.started text", "Дуэль началась").setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting tpAcceptText = new TextSetting("quickinteract.tp accept text", "Принять запрос").setVisibleWhen(this.customPhrases::getValue);
    public final TextSetting tpDeclineText = new TextSetting("quickinteract.tp decline text", "Отклонить запрос").setVisibleWhen(this.customPhrases::getValue);
    private static final long REQUEST_TIMEOUT_MS = 120000L;
    private static final long TELEPORT_TIMEOUT_MS = 30000L;
    private static final Pattern NAME_SPLIT_PATTERN = Pattern.compile("[^A-Za-z0-9_]+");
    private final QuickInteractPopup contextMenu = new QuickInteractPopup(this);
    private final QuickActionsPopup actionButtons = new QuickActionsPopup();
    private final Map<QuickInteract.QuickAction, ClickEvent> pendingActions = new EnumMap<>(QuickInteract.QuickAction.class);
    private final StopWatch requestTimer = new StopWatch();
    private String requestPlayer;

    public static QuickInteract getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(QuickInteract.class);
    }

    private boolean isChatOrGuiOpen() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGuiScreen;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.contextMenu.reset();
        this.actionButtons.hide();
        this.pendingActions.clear();
        this.requestPlayer = null;
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (!this.isChatOrGuiOpen()) {
            if (this.contextMenu.isOpen() || this.contextMenu.isVisible()) {
                this.contextMenu.reset();
            }

            this.renderActionButtons(render2DEvent.getContext(), -1.0, -1.0);
        }
    }

    private void renderActionButtons(DrawContext drawContext, double d, double d2) {
        String string = this.getActiveRequestPlayer();
        if (string != null && (!this.contextMenu.isOpen() || !string.equals(this.contextMenu.getPlayerName()))) {
            PlayerEntity playerEntity = this.findPlayer(string);
            if (playerEntity != null && ProjectionUtil.isVisible(playerEntity.getBoundingBox())) {
                Vec3d vec3d = ProjectionUtil.getInterpolatedPos(playerEntity, mc.getRenderTickCounter().getTickProgress(false));
                Vector2f vector2f = ProjectionUtil.worldToScreen(vec3d.x, vec3d.y + playerEntity.getHeight() / 2.0, vec3d.z);
                if (vector2f == null) {
                    this.actionButtons.hide();
                } else {
                    this.actionButtons.render(drawContext, string, this.getActionsFor(string), vector2f.x, vector2f.y, d, d2);
                }
            } else {
                this.actionButtons.hide();
            }
        } else {
            this.actionButtons.hide();
        }
    }

    private PlayerEntity findPlayer(String string) {
        if (mc.world == null) {
            return null;
        }

        for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
            if (abstractClientPlayerEntity != mc.player && abstractClientPlayerEntity.isAlive() && string.equals(abstractClientPlayerEntity.getGameProfile().name())) {
                return abstractClientPlayerEntity;
            }
        }

        return null;
    }

    public boolean isFriend(String string) {
        return Sadnes.getInstance().getFriendManager().isFriend(string);
    }

    public void toggleFriend(String string) {
        FriendManager friendManager = Sadnes.getInstance().getFriendManager();
        if (friendManager.isFriend(string)) {
            friendManager.removeFriend(string);
        } else {
            friendManager.addFriend(string);
        }
    }

    public void sendDuel(String string) {
        this.sendPlayerCommand(this.duelCommand.getValue(), string);
        this.requestPlayer = string;
        this.contextMenu.close();
    }

    public void sendReport(String string) {
        this.sendPlayerCommand(this.reportCommand.getValue(), string);
        this.contextMenu.close();
    }

    public String getActiveRequestPlayer() {
        if (this.pendingActions.isEmpty()) {
            return null;
        }

        long l = this.pendingActions.containsKey(QuickInteract.QuickAction.CANCEL_TELEPORT) ? 30000L : 120000L;
        if (this.requestTimer.isElapsed(l)) {
            this.pendingActions.clear();
        }

        return this.pendingActions.isEmpty() ? null : this.requestPlayer;
    }

    public List<QuickInteract.QuickAction> getActionsFor(String string) {
        return string != null && string.equals(this.getActiveRequestPlayer()) ? List.copyOf(this.pendingActions.keySet()) : List.of();
    }

    public void performAction(QuickInteract.QuickAction quickAction) {
        ClickEvent clickEvent = this.pendingActions.get(quickAction);
        this.pendingActions.clear();
        this.actionButtons.hide();
        this.contextMenu.close();
        this.executeClickEvent(clickEvent);
    }

    private void executeClickEvent(ClickEvent clickEvent) {
        if (clickEvent == null || mc.getNetworkHandler() == null) {
            return;
        }

        switch (clickEvent) {
            case ClickEvent.RunCommand runCommand -> this.sendChatOrCommand(runCommand.command());
            case ClickEvent.SuggestCommand suggestCommand -> this.sendChatOrCommand(suggestCommand.command());
            case ClickEvent.Custom custom -> mc.getNetworkHandler().sendPacket(new CustomClickActionC2SPacket(custom.id(), custom.payload()));
            default -> {
            }
        }
    }

    private void sendChatOrCommand(String string) {
        String string2 = string.trim();
        if (!string2.isEmpty()) {
            if (string2.startsWith("/")) {
                mc.getNetworkHandler().sendChatCommand(string2.substring(1));
            } else {
                mc.getNetworkHandler().sendChatMessage(string2);
            }
        }
    }

    private void sendPlayerCommand(String string, String string2) {
        if (mc.getNetworkHandler() != null && string != null) {
            String string3 = string.trim();
            if (string3.startsWith("/")) {
                string3 = string3.substring(1);
            }

            if (!string3.isEmpty()) {
                mc.getNetworkHandler().sendChatCommand(string3 + " " + string2);
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay()) {
            Text text = gameMessageS2CPacket.content();
            mc.execute(() -> this.handleChatMessage(text));
        }
    }

    private void handleChatMessage(Text text) {
        String string = text.getString();
        if (containsIgnoreCase(string, this.startedText.getValue())) {
            this.pendingActions.clear();
            this.actionButtons.hide();
            if (this.contextMenu.isOpen() && this.requestPlayer != null && this.requestPlayer.equals(this.contextMenu.getPlayerName())) {
                this.contextMenu.close();
            }
        } else {
            List list = collectClickSegments(text);
            EnumMap enumMap = new EnumMap<>(QuickInteract.QuickAction.class);
            putAction(enumMap, QuickInteract.QuickAction.ACCEPT_TELEPORT, list, this.tpAcceptText.getValue());
            putAction(enumMap, QuickInteract.QuickAction.DECLINE_TELEPORT, list, this.tpDeclineText.getValue());
            putAction(enumMap, QuickInteract.QuickAction.ACCEPT, list, this.acceptText.getValue());
            putAction(enumMap, QuickInteract.QuickAction.DECLINE, list, this.declineText.getValue());
            putAction(enumMap, QuickInteract.QuickAction.CANCEL_DUEL, list, this.cancelText.getValue());
            putAction(enumMap, QuickInteract.QuickAction.CANCEL_TELEPORT, list, this.teleportText.getValue());
            if (enumMap.isEmpty()) {
                if (!this.pendingActions.isEmpty() && containsWord(string, this.requestPlayer)) {
                    this.pendingActions.clear();
                    this.actionButtons.hide();
                }
            } else {
                String string2 = this.findPlayerName(string);
                if (string2 != null) {
                    this.requestPlayer = string2;
                }

                if (this.requestPlayer != null) {
                    this.pendingActions.clear();
                    this.pendingActions.putAll(enumMap);
                    this.requestTimer.reset();
                }
            }
        }
    }

    private static void putAction(
        Map<QuickInteract.QuickAction, ClickEvent> map, QuickInteract.QuickAction quickAction, List<QuickInteract.ClickSegment> clickSegments, String string
    ) {
        ClickEvent clickEvent = findClickEvent(clickSegments, string);
        if (clickEvent != null && !map.containsValue(clickEvent)) {
            map.put(quickAction, clickEvent);
        }
    }

    private static List<QuickInteract.ClickSegment> collectClickSegments(Text text2) {
        ArrayList arrayList = new ArrayList(4);
        text2.visit((style, string) -> {
            ClickEvent clickEvent2 = style.getClickEvent();
            if (clickEvent2 == null) {
                return Optional.empty();
            }

            if (!arrayList.isEmpty()) {
                QuickInteract.ClickSegment clickSegment = (QuickInteract.ClickSegment)arrayList.getLast();
                if (clickSegment != null && clickEvent2.equals(clickSegment.clickEvent)) {
                    clickSegment.text.append(string);
                    return Optional.empty();
                }
            }

            arrayList.add(new QuickInteract.ClickSegment(clickEvent2, string));
            return Optional.empty();
        }, Style.EMPTY);
        return arrayList;
    }

    private static ClickEvent findClickEvent(List<QuickInteract.ClickSegment> clickSegments, String string) {
        for (QuickInteract.ClickSegment clickSegment : clickSegments) {
            if (containsIgnoreCase(clickSegment.text.toString(), string)) {
                return clickSegment.clickEvent;
            }
        }

        return null;
    }

    private static boolean containsIgnoreCase(String string, String string2) {
        if (string != null && string2 != null) {
            String string3 = string2.trim().toLowerCase(Locale.ROOT);
            return !string3.isEmpty() && string.toLowerCase(Locale.ROOT).contains(string3);
        } else {
            return false;
        }
    }

    private static boolean containsWord(String string, String string2) {
        for (String string3 : NAME_SPLIT_PATTERN.split(string)) {
            if (string3.equals(string2)) {
                return true;
            }
        }

        return false;
    }

    private String findPlayerName(String string) {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            String string2 = mc.player.getGameProfile().name();

            for (String string3 : NAME_SPLIT_PATTERN.split(string)) {
                if (string3.length() >= 3 && !string3.equalsIgnoreCase(string2)) {
                    for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
                        if (string3.equals(playerListEntry.getProfile().name())) {
                            return string3;
                        }
                    }
                }
            }
        }

        return null;
    }

    public boolean isInteracting() {
        return this.contextMenu.isOpen() || this.contextMenu.isVisible() || this.getActiveRequestPlayer() != null;
    }

    public void renderInChat(DrawContext drawContext, int i, int i2, float f) {
        ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
        if (mc.currentScreen instanceof ChatScreen && clickGuiScreen != null) {
            clickGuiScreen.getOpenAnimation().forceValue(1.0);
        }

        this.renderActionButtons(drawContext, i, i2);
        this.contextMenu.render(drawContext, i, i2, f);
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        if (mc.currentScreen instanceof ChatScreen && screenKeyEvent.isPress()) {
            this.handleClick(screenKeyEvent.getKey(), screenKeyEvent.getMouseX(), screenKeyEvent.getMouseY(), false);
        }
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (mc.currentScreen instanceof ChatScreen && this.contextMenu.isOpen()) {
            if (screenMouseClickEvent.isPress() && screenMouseClickEvent.getButton() == 256) {
                this.contextMenu.close();
            }
        }
    }

    public boolean mouseClicked(Click click, boolean flag) {
        return this.handleClick(click.button(), click.x(), click.y(), flag);
    }

    public boolean keyPressed(KeyInput keyInput) {
        if (this.contextMenu.isOpen() && keyInput.key() == 256) {
            this.contextMenu.close();
            return true;
        } else {
            return false;
        }
    }

    private boolean handleClick(int i, double d, double d2, boolean flag) {
        if (i == 0 && !flag) {
            QuickInteract.QuickAction quickAction = this.actionButtons.getHoveredAction(d, d2);
            if (quickAction != null) {
                this.performAction(quickAction);
                return true;
            }
        }

        if (this.contextMenu.isOpen()) {
            if (this.contextMenu.isMouseOver(d, d2)) {
                return this.contextMenu.mouseClicked(new Click(d, d2, new MouseInput(i, 0)), false);
            }

            this.contextMenu.close();
        }

        if (i == 1 && !flag) {
            QuickInteract.ScreenBounds screenBounds = this.findPlayerAt(d, d2);
            if (screenBounds == null) {
                return false;
            }

            this.contextMenu.open(screenBounds.name, screenBounds.maxX + 6.0F, screenBounds.minY, (int)GuiScale.getScaledWidth(), (int)GuiScale.getScaledHeight());
            return true;
        } else {
            return false;
        }
    }

    private QuickInteract.ScreenBounds findPlayerAt(double d, double d2) {
        if (mc.player != null && mc.world != null) {
            for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
                if (abstractClientPlayerEntity != mc.player && abstractClientPlayerEntity.isAlive() && !AntiBot.isBotStatic(abstractClientPlayerEntity) && ProjectionUtil.isVisible(abstractClientPlayerEntity.getBoundingBox())) {
                    QuickInteract.ScreenBounds screenBounds = this.computeScreenBounds(abstractClientPlayerEntity);
                    if (screenBounds != null && screenBounds.contains(d, d2)) {
                        return screenBounds;
                    }
                }
            }
        }

        return null;
    }

    private QuickInteract.ScreenBounds computeScreenBounds(PlayerEntity playerEntity) {
        Box box = playerEntity.getBoundingBox();
        float f = Float.MAX_VALUE;
        float f2 = Float.MAX_VALUE;
        float f3 = -Float.MAX_VALUE;
        float f4 = -Float.MAX_VALUE;
        boolean flag = false;
        double[] doubles = new double[]{box.minX, box.maxX};
        double[] doubles2 = new double[]{box.minY, box.maxY + 0.2};
        double[] doubles3 = new double[]{box.minZ, box.maxZ};

        for (double d : doubles) {
            for (double d2 : doubles2) {
                for (double d3 : doubles3) {
                    Vector2f vector2f = ProjectionUtil.worldToScreen(d, d2, d3);
                    if (vector2f != null) {
                        flag = true;
                        f = Math.min(f, vector2f.x);
                        f2 = Math.min(f2, vector2f.y);
                        f3 = Math.max(f3, vector2f.x);
                        f4 = Math.max(f4, vector2f.y);
                    }
                }
            }
        }

        if (!flag) {
            return null;
        }

        QuickInteract.ScreenBounds screenBounds = new QuickInteract.ScreenBounds();
        screenBounds.name = playerEntity.getGameProfile().name();
        screenBounds.minX = f;
        screenBounds.minY = f2;
        screenBounds.maxX = f3;
        screenBounds.maxY = f4;
        return screenBounds;
    }

    static final class ClickSegment {
        final ClickEvent clickEvent;
        final StringBuilder text;

        ClickSegment(ClickEvent clickEvent2, String string) {
            this.clickEvent = clickEvent2;
            this.text = new StringBuilder(string);
        }
    }

    public enum QuickAction {
        ACCEPT_TELEPORT(Icon.CIRCLE_CHECK, "quickinteract.accept teleport", "Accept teleport request"),
        DECLINE_TELEPORT(Icon.CIRCLE_XMARK, "quickinteract.decline teleport", "Decline teleport request"),
        ACCEPT(Icon.CIRCLE_CHECK, "quickinteract.accept duel", "Accept duel"),
        DECLINE(Icon.CIRCLE_XMARK, "quickinteract.decline duel", "Decline duel"),
        CANCEL_DUEL(Icon.CLOSE, "quickinteract.cancel duel", "Cancel duel request"),
        CANCEL_TELEPORT(Icon.CLOCK_HOUR_4, "quickinteract.cancel teleport", "Cancel teleport");

        private final Icon icon;
        private final String translationKey;
        private final String fallback;

        public String getDisplayName() {
            return Language.translate(this.translationKey, this.fallback);
        }

        public Icon getIcon() {
            return this.icon;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public String getFallback() {
            return this.fallback;
        }

        QuickAction(Icon icon2, String string, String string2) {
            this.icon = icon2;
            this.translationKey = string;
            this.fallback = string2;
        }
    }

    static final class ScreenBounds {
        String name;
        float minX;
        float minY;
        float maxX;
        float maxY;

        boolean contains(double d, double d2) {
            return d >= this.minX && d <= this.maxX && d2 >= this.minY && d2 <= this.maxY;
        }
    }
}
