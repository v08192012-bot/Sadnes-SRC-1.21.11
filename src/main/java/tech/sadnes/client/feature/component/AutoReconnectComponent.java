package tech.sadnes.client.feature.component;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.MenuTickEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.AutoReconnector;
import tech.sadnes.client.util.player.InventoryUtil;

public final class AutoReconnectComponent extends FeatureComponent {
    private final StopWatch timer = new StopWatch();
    private final AutoReconnector statusOverlay = new AutoReconnector();
    private int targetAnarchy = -1;
    private int lastClickedSyncId = -1;
    private boolean active;
    private String holyMode;

    public static AutoReconnectComponent getInstance() {
        return Sadnes.getInstance().getComponentManager().getComponent(AutoReconnectComponent.class);
    }

    public boolean start(int i) {
        return this.start(i, null);
    }

    public boolean start(int i, String string) {
        if (i == -1) {
            return false;
        }

        if (ServerManager.isHolyWorld()) {
            String string2 = string != null ? string : ServerManager.getHolyMode();
            if (string2 == null) {
                return false;
            }

            this.holyMode = parseModeType(string2) == AutoReconnectComponent.HolyMode.LIGHT_116 ? ServerManager.getLiteModeName(i) : string2;
        } else {
            this.holyMode = null;
        }

        this.targetAnarchy = i;
        this.lastClickedSyncId = -1;
        this.active = true;
        this.timer.reset();
        return true;
    }

    public void reconnect() {
        if (!this.start(ServerManager.getAnarchyNumber())) {
            this.cancel();
        }

        this.statusOverlay.disconnect(Language.translate("screen.pause.reconnecting", "Переподключение..."));
    }

    public String getHolyMode() {
        return this.holyMode;
    }

    public void cancel() {
        this.active = false;
        this.targetAnarchy = -1;
        this.lastClickedSyncId = -1;
        this.holyMode = null;
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.statusOverlay.cancel();
        this.timer.reset();
    }

    @EventHandler
    public void onMenuTick(MenuTickEvent menuTickEvent) {
        this.statusOverlay.tick();
        boolean flag;
        if (!(mc.currentScreen instanceof TitleScreen)) {
            flag = mc.currentScreen instanceof MultiplayerScreen;
        } else {
            flag = true;
        }

        if (flag) {
            this.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.active && mc.player != null && mc.getNetworkHandler() != null) {
            if (ServerManager.isFunTime() || ServerManager.isSpookyTime()) {
                this.tickFunTime();
            } else if (ServerManager.isReallyWorld()) {
                this.tickReallyWorld();
            } else if (ServerManager.isHolyWorld()) {
                this.tickHolyWorld();
            } else {
                this.cancel();
            }
        }
    }

    private void tickFunTime() {
        if (ServerManager.getAnarchyNumber() != -1) {
            mc.getNetworkHandler().sendChatCommand("hub");
            this.timer.reset();
        } else if (this.timer.hasPassed(1000.0)) {
            mc.getNetworkHandler().sendChatCommand("an" + this.targetAnarchy);
            this.cancel();
        }
    }

    private void tickReallyWorld() {
        if (this.timer.hasPassed(120.0)) {
            this.timer.reset();
            if (ServerManager.getAnarchyNumber() != -1) {
                mc.getNetworkHandler().sendChatCommand("hub");
            } else if (mc.currentScreen == null) {
                this.useCompass();
            } else if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                ScreenHandler screenHandler = genericContainerScreen.getScreenHandler();
                String string = genericContainerScreen.getTitle().getString().toLowerCase();
                if (string.contains("выбор сервера")) {
                    this.clickSlot(screenHandler, 21);
                } else if (string.contains("выбор мира грифа")) {
                    Slot slot = this.findAnarchySlot(screenHandler, "гриф");
                    if (slot == null) {
                        if (screenHandler.slots.size() > 44 && screenHandler.getSlot(44).getStack().isOf(Items.ARROW)) {
                            this.clickSlot(screenHandler, 44);
                        } else {
                            this.notifyNotFound();
                        }
                    } else {
                        this.clickAndClose(screenHandler, slot);
                    }
                }
            }
        }
    }

    private void tickHolyWorld() {
        if (this.timer.hasPassed(250.0)) {
            this.timer.reset();
            if (ServerManager.getHolyMode() != null) {
                mc.getNetworkHandler().sendChatCommand("hub");
            } else if (mc.currentScreen == null) {
                this.useCompass();
            } else if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                ScreenHandler screenHandler = genericContainerScreen.getScreenHandler();
                String string = genericContainerScreen.getTitle().getString().toLowerCase();
                AutoReconnectComponent.HolyMode holyMode2 = this.getModeType();
                if (string.contains("выберите режим")) {
                    Slot slot = this.findModeSlot(screenHandler, holyMode2);
                    if (slot != null) {
                        this.clickSlot(screenHandler, slot.id);
                    } else {
                        if (hasItems(screenHandler)) {
                            this.fail(String.format(Language.translate("command.rct.mode_not_found", "Режим %s не найден, реконнект отменён."), this.holyMode));
                        }
                    }
                } else {
                    switch (holyMode2) {
                        case CLASSIC:
                            if (string.contains("классич")) {
                                this.selectAnarchy(screenHandler, "классик");
                                return;
                            }
                            break;
                        case PRIME:
                            if (string.contains("прайм анарх")) {
                                this.selectAnarchy(screenHandler, "прайм");
                                return;
                            }
                            break;
                        case LIGHT_120:
                            if (string.contains("сервер лайт")) {
                                this.selectAnarchy(screenHandler, "лайт");
                                return;
                            }
                            break;
                        case LIGHT_116:
                            if (string.contains("выбор лайт анархии")) {
                                this.selectLiteAnarchy(screenHandler);
                            }
                    }
                }
            }
        }
    }

    private AutoReconnectComponent.HolyMode getModeType() {
        return parseModeType(this.holyMode);
    }

    private static AutoReconnectComponent.HolyMode parseModeType(String string) {
        String string2 = string == null ? "" : string.toLowerCase();
        if (string2.contains("классик")) {
            return AutoReconnectComponent.HolyMode.CLASSIC;
        } else if (string2.contains("прайм")) {
            return AutoReconnectComponent.HolyMode.PRIME;
        } else {
            return string2.contains("1.20") ? AutoReconnectComponent.HolyMode.LIGHT_120 : AutoReconnectComponent.HolyMode.LIGHT_116;
        }
    }

    private Slot findModeSlot(ScreenHandler screenHandler, AutoReconnectComponent.HolyMode holyMode2) {
        for (Slot slot : screenHandler.slots) {
            ItemStack itemStack = slot.getStack();
            if (!itemStack.isEmpty()) {
                boolean flag;
                label39: {
                    String string = getItemText(itemStack);
                    boolean flag2;
                    switch (holyMode2) {
                        case CLASSIC:
                            flag2 = string.contains("классическ");
                            break;
                        case PRIME:
                            flag2 = string.contains("прайм");
                            break;
                        case LIGHT_120:
                            flag2 = string.contains("1.20");
                            break;
                        case LIGHT_116:
                            if (string.contains("1.16") && string.contains("лайт")) {
                                flag = !string.contains("классическ");
                                break label39;
                            }

                            flag2 = false;
                            break;
                        default:
                            throw new MatchException(null, null);
                    }

                    flag = flag2;
                }

                if (flag) {
                    return slot;
                }
            }
        }

        return null;
    }

    private void selectLiteAnarchy(ScreenHandler screenHandler) {
        String string = this.getTeamModeKeyword();
        Slot slot = this.findAnarchySlot(screenHandler, string);
        if (slot != null) {
            this.clickAndClose(screenHandler, slot);
        } else if (this.lastClickedSyncId != screenHandler.syncId && hasItems(screenHandler)) {
            Slot slot2 = findSlotContaining(screenHandler, string, "анархии");
            if (this.lastClickedSyncId != -1) {
                this.notifyNotFound();
            } else if (slot2 != null) {
                this.lastClickedSyncId = screenHandler.syncId;
                this.clickSlot(screenHandler, slot2.id);
            } else {
                this.notifyNotFound();
            }
        }
    }

    private String getTeamModeKeyword() {
        String string = this.holyMode == null ? "" : this.holyMode.toLowerCase();
        if (string.contains("соло")) {
            return "соло";
        } else if (string.contains("дуо")) {
            return "дуо";
        } else {
            return string.contains("трио") ? "трио" : "клан";
        }
    }

    private void useCompass() {
        if (mc.player.getMainHandStack().isOf(Items.COMPASS)) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } else {
            int i = InventoryUtil.findHotbarIndex(Items.COMPASS);
            if (i != -1) {
                InventoryUtil.setSelectedSlot(i);
            } else {
                InventoryUtil.setSelectedSlot(0);
            }
        }
    }

    private void selectAnarchy(ScreenHandler screenHandler, String string) {
        Slot slot = this.findAnarchySlot(screenHandler, string);
        if (slot == null) {
            if (hasItems(screenHandler)) {
                this.notifyNotFound();
            }
        } else {
            this.clickAndClose(screenHandler, slot);
        }
    }

    private void clickAndClose(ScreenHandler screenHandler, Slot slot) {
        this.clickSlot(screenHandler, slot.id);
        mc.player.closeHandledScreen();
        this.cancel();
    }

    private void notifyNotFound() {
        String string = this.holyMode == null ? "" : this.holyMode + " ";
        this.fail(String.format(Language.translate("command.rct.not_found", "Анархия %s#%s не найдена, реконнект отменён."), string, this.targetAnarchy));
    }

    private void fail(String string) {
        NotificationUtil.error(NotificationSources.reconnect(), string);
        if (mc.player != null) {
            mc.player.closeHandledScreen();
        }

        this.cancel();
    }

    private static boolean hasItems(ScreenHandler screenHandler) {
        int i = screenHandler.slots.size() - 36;

        for (int i2 = 0; i2 < i; i2++) {
            if (!screenHandler.getSlot(i2).getStack().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void clickSlot(ScreenHandler screenHandler, int i) {
        mc.interactionManager.clickSlot(screenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
    }

    private Slot findAnarchySlot(ScreenHandler screenHandler, String string) {
        for (Slot slot : screenHandler.slots) {
            ItemStack itemStack = slot.getStack();
            if (!itemStack.isEmpty() && matchesAnarchyNumber(getItemText(itemStack), string, this.targetAnarchy)) {
                return slot;
            }
        }

        return null;
    }

    private static Slot findSlotContaining(ScreenHandler screenHandler, String string, String string2) {
        for (Slot slot : screenHandler.slots) {
            ItemStack itemStack = slot.getStack();
            if (!itemStack.isEmpty()) {
                String string3 = getItemText(itemStack);
                if (string3.contains(string) && string3.contains(string2)) {
                    return slot;
                }
            }
        }

        return null;
    }

    private static boolean matchesAnarchyNumber(String string, String string2, int i) {
        String string3 = "#" + i;
        int i2 = string.indexOf(string2);

        while (i2 != -1) {
            int i3 = i2 + string2.length();
            int i4 = Math.min(string.length(), i3 + 16);
            int i5 = string.indexOf(string3, i3);
            if (i5 != -1 && i5 < i4) {
                int i6 = i5 + string3.length();
                if (i6 >= string.length() || !Character.isDigit(string.charAt(i6))) {
                    return true;
                }
            }

            i2 = string.indexOf(string2, i3);
        }

        return false;
    }

    private static String getItemText(ItemStack itemStack) {
        StringBuilder builder = new StringBuilder(itemStack.getName().getString().toLowerCase());
        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            for (Text text : loreComponent.lines()) {
                builder.append(' ').append(text.getString().toLowerCase());
            }
        }

        return builder.toString();
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (this.active && packetEvent.isReceive() && mc.player != null) {
            if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                String string = gameMessageS2CPacket.content().getString().toLowerCase();
                if (string.contains("премиальные анархии")) {
                    NotificationUtil.error(
                        NotificationSources.reconnect(), Language.translate("command.rct.premium_denied", "Нет доступа к премиальным анархиям!")
                    );
                    this.cancel();
                }
            }
        }
    }

    enum HolyMode {
        CLASSIC,
        PRIME,
        LIGHT_120,
        LIGHT_116;
    }
}
