package tech.sadnes.client.feature.impl.misc;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "ItemScroller",
    description = "feature.impl.misc.itemscroller",
    displayName = "feature.impl.misc.itemscroller",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_INVENTORY
)
public final class ItemScroller extends Feature {
    public final NumberSetting delay = new NumberSetting("itemscroller.delay", 100.0F, 0.0F, 200.0F, 5.0F);
    public final StopWatch stopWatch = new StopWatch();

    public static ItemScroller getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(ItemScroller.class);
    }

    public boolean canQuickMove(ScreenHandler screenHandler, Slot slot) {
        boolean flag = slot.id >= 36 && slot.id <= 44;
        if ((slot.id < 9 || slot.id > 35) && !flag) {
            return true;
        }

        int i = flag ? 9 : 36;
        int i2 = flag ? 35 : 44;
        ItemStack itemStack = slot.getStack();

        for (int i3 = i; i3 <= i2; i3++) {
            ItemStack itemStack2 = screenHandler.getSlot(i3).getStack();
            boolean flag2 = ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack) && itemStack2.getCount() < itemStack2.getMaxCount();
            if (itemStack2.isEmpty() || flag2) {
                return true;
            }
        }

        return false;
    }
}
