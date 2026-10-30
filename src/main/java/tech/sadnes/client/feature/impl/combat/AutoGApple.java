package tech.sadnes.client.feature.impl.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "AutoGApple",
    description = "feature.impl.combat.autogapple",
    displayName = "feature.impl.combat.autogapple",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_INVENTORY
)
public final class AutoGApple extends Feature {
    private final NumberSetting health = new NumberSetting("autogapple.health", 15.0F, 1.0F, 36.0F, 1.0F);
    private final NumberSetting delay = new NumberSetting("autogapple.delay", 0.0F, 0.0F, 2000.0F, 50.0F);
    private final BooleanSetting absorption = new BooleanSetting("autogapple.absorption", false);
    private final StopWatch timer = new StopWatch();
    private boolean eating;

    @Override
    public void onEnable() {
        super.onEnable();
        this.stopEating();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (ServerManager.isNullWorld()) {
            this.stopEating();
        } else {
            if (this.hasGoldenApple()) {
                float f = mc.player.getHealth();
                boolean flag = this.absorption.getValue();
                NumberSetting numberSetting = this.health;
                float f2;
                if (flag) {
                    f2 = mc.player.getAbsorptionAmount();
                } else {
                    f2 = 0.0F;
                }

                float f3 = numberSetting.getValue();
                StopWatch stopWatch = this.timer;
                NumberSetting numberSetting2 = this.delay;
                boolean flag2 = this.eating;
                float f4 = ExactMath.add(f2, f);
                if (!(f3 >= f4)) {
                    this.eating = flag2;
                    this.stopEating();
                    return;
                }

                this.eating = flag2;
                double d = numberSetting2.getValue().longValue();
                if (stopWatch.hasPassed(d)) {
                    this.eating = true;
                    if (!mc.player.isUsingItem()) {
                        ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                        ClientPlayerEntity clientPlayerEntity = mc.player;
                        Hand hand = Hand.OFF_HAND;
                        clientPlayerInteractionManager.interactItem(clientPlayerEntity, hand);
                    }

                    KeyBinding keyBinding = mc.options.useKey;
                    boolean flag3 = mc.player.isUsingItem();
                    keyBinding.setPressed(flag3);
                    return;
                }
            }

            this.stopEating();
        }
    }

    private boolean hasGoldenApple() {
        ItemStack itemStack = mc.player.getOffHandStack();
        Item item = Items.GOLDEN_APPLE;
        if (!itemStack.isOf(item)) {
            Item item2 = Items.ENCHANTED_GOLDEN_APPLE;
            return itemStack.isOf(item2);
        } else {
            return true;
        }
    }

    private void stopEating() {
        if (this.eating) {
            this.eating = false;
            mc.options.useKey.setPressed(false);
            this.timer.reset();
        }
    }
}
