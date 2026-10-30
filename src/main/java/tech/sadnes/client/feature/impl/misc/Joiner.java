package tech.sadnes.client.feature.impl.misc;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "Joiner",
    description = "feature.impl.misc.joiner",
    displayName = "feature.impl.misc.joiner",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public class Joiner extends Feature {
    private final ModeSetting type = new ModeSetting("joiner.type", new String[]{"st_duels", "anarchy"});
    private final TextSetting anarchyNumber = new TextSetting("joiner.anarchyNumber", "102", "напр. 102").setVisibleWhen(() -> this.type.is("anarchy"));
    private final StopWatch actionTimer = new StopWatch();
    private final StopWatch joinTimer = new StopWatch();

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            if (this.joinTimer.hasPassed(1000.0)) {
                if (this.type.is("st_duels")) {
                    this.joinDuels();
                } else {
                    if (this.type.is("anarchy")) {
                        this.joinAnarchy();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.joinTimer.reset();
        this.actionTimer.reset();
    }

    private void joinDuels() {
        if (this.isInLobby()) {
            Slot slot = InventoryUtil.findSlotReverse(Items.COMPASS);
            if (slot != null) {
                if (this.actionTimer.hasPassed(200.0)) {
                    if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                        if (genericContainerScreen.getTitle().getString().toLowerCase().contains("выберите режим")) {
                            GenericContainerScreenHandler genericContainerScreenHandler = genericContainerScreen.getScreenHandler();

                            for (int i = 0; i < genericContainerScreenHandler.slots.size(); i++) {
                                if (genericContainerScreenHandler.slots.get(i).getStack().isOf(Items.NETHERITE_SWORD)) {
                                    mc.interactionManager.clickSlot(genericContainerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                                    this.actionTimer.reset();
                                    return;
                                }
                            }
                        }
                    } else {
                        InventoryUtil.swapToHand(slot, Hand.MAIN_HAND);
                        this.actionTimer.reset();
                    }
                }
            }
        }
    }

    private void joinAnarchy() {
        if (ServerManager.isFunTime() || ServerManager.isSpookyTime()) {
            if (ServerManager.getAnarchyNumber() == -1) {
                if (this.actionTimer.hasPassed(200.0)) {
                    String string = this.anarchyNumber.getValue().trim();
                    if (!string.isEmpty()) {
                        mc.getNetworkHandler().sendChatCommand("an" + string);
                        this.actionTimer.reset();
                    }
                }
            }
        }
    }

    private boolean isInLobby() {
        return !ServerManager.isFunTime() && !ServerManager.isSpookyTime() && !ServerManager.isHolyWorld() && !ServerManager.isReallyWorld()
            ? false
            : ServerManager.getAnarchyNumber() == -1;
    }

    public static Joiner getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Joiner.class);
    }

    public ModeSetting getType() {
        return this.type;
    }

    public TextSetting getAnarchyNumber() {
        return this.anarchyNumber;
    }

    public StopWatch getActionTimer() {
        return this.actionTimer;
    }

    public StopWatch getJoinTimer() {
        return this.joinTimer;
    }
}
