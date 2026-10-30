package tech.sadnes.client.feature.impl.player;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "AutoRelease",
    description = "feature.impl.player.autorelease",
    displayName = "feature.impl.player.autorelease",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_ITEMS
)
public final class AutoRelease extends Feature {
    private final MultiSetting items = new MultiSetting(
        "autorelease.items", BooleanSetting.of("bow", true), BooleanSetting.of("crossbow", true), BooleanSetting.of("trident", true)
    );
    private final ModeSetting bowMode = new ModeSetting("autorelease.bowMode", new String[]{"spam", "max"}).setValue("max").setVisibleWhen(() -> this.items.has("bow"));

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        label38: {
            if (mc.player != null && mc.interactionManager != null && mc.player.isUsingItem()) {
                ItemStack itemStack = mc.player.getActiveItem();
                Item item = itemStack.getItem();
                int i = mc.player.getItemUseTime();
                boolean flag = item instanceof BowItem;
                MultiSetting multiSetting = this.items;
                if (flag) {
                    boolean flag2 = multiSetting.has("bow");
                    ModeSetting modeSetting = this.bowMode;
                    if (flag2 && (modeSetting.is("spam") ? 3 <= i : 20 <= i)) {
                        break label38;
                    }
                }

                if (item instanceof CrossbowItem && multiSetting.has("crossbow")) {
                    int i2 = this.getQuickChargeLevel(itemStack);
                    int i3 = (int)(Integer.toUnsignedLong(i2) * 4L);
                    int i4 = i2 + i3;
                    if (25 - i4 <= i) {
                        break label38;
                    }
                }

                if (item instanceof TridentItem && multiSetting.has("trident") && i >= 10) {
                    break label38;
                }
            }

            return;
        }

        ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
        ClientPlayerEntity clientPlayerEntity = mc.player;
        clientPlayerInteractionManager.stopUsingItem(clientPlayerEntity);
    }

    private int getQuickChargeLevel(ItemStack itemStack) {
        if (mc.world != null) {
            if (itemStack.hasEnchantments()) {
                DynamicRegistryManager dynamicRegistryManager = mc.world.getRegistryManager();
                RegistryKey registryKey = RegistryKeys.ENCHANTMENT;
                Registry registry = dynamicRegistryManager.getOrThrow(registryKey);
                RegistryKey registryKey2 = Enchantments.QUICK_CHARGE;
                return EnchantmentHelper.getLevel(registry.getOrThrow(registryKey2), itemStack);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }
}
