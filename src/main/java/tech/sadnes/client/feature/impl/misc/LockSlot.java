package tech.sadnes.client.feature.impl.misc;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "LockSlot",
    description = "feature.impl.misc.lockslot",
    displayName = "feature.impl.misc.lockslot",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_INVENTORY
)
public final class LockSlot extends Feature {
    public final MultiSetting slots = new MultiSetting(
        "lockslot.slots",
        BooleanSetting.of("1", false),
        BooleanSetting.of("2", false),
        BooleanSetting.of("3", false),
        BooleanSetting.of("4", false),
        BooleanSetting.of("5", false),
        BooleanSetting.of("6", false),
        BooleanSetting.of("7", false),
        BooleanSetting.of("8", false),
        BooleanSetting.of("9", false)
    );
    public final BooleanSetting onlyPvP = new BooleanSetting("lockslot.onlyPvP", false);
    public final BooleanSetting includeInventory = new BooleanSetting("lockslot.includeInventory", false);

    public static LockSlot getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(LockSlot.class);
    }

    public boolean isSelectedSlotLocked() {
        return this.isActive() && mc.player != null ? this.isSlotLocked(mc.player.getInventory().getSelectedSlot()) : false;
    }

    @EventHandler
    public void onSlotClick(SlotClickEvent slotClickEvent) {
        if (this.isActive() && this.includeInventory.getValue()) {
            SlotActionType slotActionType = slotClickEvent.getActionType();
            if (slotActionType == SlotActionType.PICKUP || slotActionType == SlotActionType.THROW) {
                int i = this.toHotbarIndex(slotClickEvent.getSlotId());
                if (i != -1 && this.isSlotLocked(i)) {
                    slotClickEvent.cancel();
                }
            }
        }
    }

    private boolean isActive() {
        return !this.isEnabled() ? false : !this.onlyPvP.getValue() || ServerManager.isInPvp();
    }

    private boolean isSlotLocked(int i) {
        return i >= 0 && i <= 8 && this.slots.has(String.valueOf(i + 1));
    }

    private int toHotbarIndex(int i) {
        if (mc.player == null) {
            return -1;
        }

        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (screenHandler != null && i >= 0 && i < screenHandler.slots.size()) {
            Slot slot = screenHandler.getSlot(i);
            if (slot.inventory instanceof PlayerInventory) {
                if (slot.inventory != mc.player.getInventory()) {
                    return -1;
                }

                int i2 = slot.getIndex();
                if (i2 >= 0 && i2 <= 8) {
                    return i2;
                }

                return -1;
            }
        }

        return -1;
    }
}
