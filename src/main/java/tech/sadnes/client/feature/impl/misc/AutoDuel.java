package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(
    name = "AutoDuel",
    description = "feature.impl.misc.autoduel",
    displayName = "feature.impl.misc.autoduel",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SOCIAL
)
public final class AutoDuel extends Feature {
    private static final Map<String, String> KITS = createKits();
    private final MultiSetting kits = new MultiSetting("autoduel.kits", KITS.keySet().toArray(new String[0]));
    private final NumberSetting delay = new NumberSetting("autoduel.delay", 200.0F, 0.0F, 2000.0F, 10.0F, Unit.MILLISECOND);
    private final Random random = new Random();
    private final TimerUtil requestTimer = new TimerUtil();
    private final TimerUtil clickTimer = new TimerUtil();
    private boolean requestSent;

    private static Map<String, String> createKits() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        linkedHashMap.put("netherite", "незер");
        linkedHashMap.put("shield", "щит");
        linkedHashMap.put("spikes", "шип");
        linkedHashMap.put("bow", "лук");
        linkedHashMap.put("totems", "тотем");
        linkedHashMap.put("nodebuff", "нодеб");
        linkedHashMap.put("orbs", "шар");
        linkedHashMap.put("classic", "класс");
        linkedHashMap.put("cheater", "читер");
        return linkedHashMap;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.requestSent = false;
        this.requestTimer.setTime(0L);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && mc.getNetworkHandler() != null && mc.interactionManager != null) {
            if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                String string = genericContainerScreen.getTitle().getString().toLowerCase(Locale.ROOT);
                ScreenHandler screenHandler = genericContainerScreen.getScreenHandler();
                if (string.contains("выбор набора")) {
                    if (this.clickTimer.hasPassed(150L)) {
                        this.selectKit(screenHandler);
                        return;
                    }
                } else if (string.contains("настройка поединка") && this.clickTimer.hasPassed(150L)) {
                    this.confirmDuel(screenHandler);
                    return;
                }
            } else {
                if (!this.requestSent) {
                    if (!this.requestTimer.hasPassed(this.delay.getValue().longValue())) {
                        return;
                    }

                    String string2 = this.pickRandomPlayer();
                    if (string2 == null) {
                        return;
                    }

                    mc.getNetworkHandler().sendChatCommand("duel " + string2);
                    this.requestSent = true;
                    this.requestTimer.reset();
                    return;
                }

                if (this.requestTimer.hasPassed(2000L)) {
                    this.requestSent = false;
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()
            && mc.player != null
            && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket
            && gameMessageS2CPacket.content().getString().toLowerCase(Locale.ROOT).contains("принял ваш запрос")) {
            this.setEnabled(false);
        }
    }

    private void selectKit(ScreenHandler screenHandler) {
        List list = this.kits.getSelected();
        ArrayList arrayList = new ArrayList();
        int i = screenHandler.slots.size();

        for (int i2 = 0; i2 < i; i2++) {
            Slot slot = screenHandler.slots.get(i2);
            if (slot != null
                && slot.hasStack()
                && slot.inventory != mc.player.getInventory()
                && this.matchesKit(slot.getStack().getName().getString().toLowerCase(Locale.ROOT), list)) {
                arrayList.add(i2);
            }
        }

        if (!arrayList.isEmpty()) {
            int i3 = (Integer)arrayList.get(this.random.nextInt(arrayList.size()));
            mc.interactionManager.clickSlot(screenHandler.syncId, i3, 0, SlotActionType.PICKUP, mc.player);
            this.clickTimer.reset();
            this.requestSent = true;
            this.requestTimer.reset();
        }
    }

    private void confirmDuel(ScreenHandler screenHandler) {
        int i = screenHandler.slots.size();

        for (int i2 = 0; i2 < i; i2++) {
            Slot slot = screenHandler.slots.get(i2);
            if (slot != null
                && slot.hasStack()
                && (slot.getStack().isOf(Items.LIME_STAINED_GLASS_PANE) || slot.getStack().getName().getString().toLowerCase(Locale.ROOT).contains("отправить"))
                )
             {
                mc.interactionManager.clickSlot(screenHandler.syncId, i2, 0, SlotActionType.PICKUP, mc.player);
                this.clickTimer.reset();
                this.requestSent = false;
                this.requestTimer.reset();
                return;
            }
        }
    }

    private boolean matchesKit(String string, List<String> strings) {
        if (strings.isEmpty()) {
            for (String string2 : KITS.values()) {
                if (string.contains(string2)) {
                    return true;
                }
            }
        } else {
            for (String string3 : strings) {
                String string4 = KITS.get(string3.toLowerCase(Locale.ROOT));
                if (string4 != null && string.contains(string4)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String pickRandomPlayer() {
        String string = mc.player.getGameProfile().name();
        ArrayList arrayList = new ArrayList();

        for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
            String string2 = playerListEntry.getProfile().name();
            if (string2 != null && !string2.isBlank() && !string2.equalsIgnoreCase(string)) {
                arrayList.add(string2);
            }
        }

        return arrayList.isEmpty() ? null : (String)arrayList.get(this.random.nextInt(arrayList.size()));
    }
}
