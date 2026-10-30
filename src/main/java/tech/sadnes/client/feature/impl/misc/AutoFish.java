package tech.sadnes.client.feature.impl.misc;

import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "AutoFish",
    description = "feature.impl.misc.autofish",
    displayName = "feature.impl.misc.autofish",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_ITEMS
)
public final class AutoFish extends Feature {
    private final NumberSetting recastDelay = new NumberSetting("autofish.recastDelay", 167.0F, 42.0F, 670.0F, 50.0F);
    private final ActionSequence scheduler = new ActionSequence();
    private final StopWatch catchTimer = new StopWatch();

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.scheduler.tick();
        if (!ServerManager.isNullWorld()) {
            if (this.scheduler.isFinished()) {
                Hand hand = this.getRodHand();
                if (hand != null) {
                    FishingBobberEntity fishingBobberEntity = mc.player.fishHook;
                    if (fishingBobberEntity != null) {
                        if (fishingBobberEntity.getVelocity().y < -0.15 && this.catchTimer.hasPassed(1488.0) && fishingBobberEntity.isTouchingWater()) {
                            this.catchTimer.reset();
                            int i = this.recastDelay.getValue().intValue();
                            this.scheduler
                                .clear()
                                .addDelayed(0, () -> mc.interactionManager.interactItem(mc.player, hand))
                                .addDelayed(i, () -> mc.interactionManager.interactItem(mc.player, hand));
                        }
                    }
                }
            }
        }
    }

    private Hand getRodHand() {
        if (mc.player.getMainHandStack().isOf(Items.FISHING_ROD)) {
            return Hand.MAIN_HAND;
        } else {
            return mc.player.getOffHandStack().isOf(Items.FISHING_ROD) ? Hand.OFF_HAND : null;
        }
    }
}
