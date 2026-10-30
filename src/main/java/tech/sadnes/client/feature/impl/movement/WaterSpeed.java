package tech.sadnes.client.feature.impl.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.TriggerBot;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "WaterSpeed",
    description = "feature.impl.movement.waterspeed",
    displayName = "feature.impl.movement.waterspeed",
    category = Category.MOVEMENT,
    subcategory = Subcategory.SPEED
)
public final class WaterSpeed extends Feature {
    public static WaterSpeed getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(WaterSpeed.class);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && mc.player.networkHandler != null) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            TagKey tagKey = FluidTags.WATER;
            if (clientPlayerEntity.isSubmergedIn(tagKey) && !mc.player.isSwimming()) {
                boolean flag = MoveUtil.isMoving();
                if ((flag || mc.player.input.playerInput.jump()) && !this.hasCombatTarget()) {
                    if (flag) {
                        mc.player.setSprinting(true);
                        mc.player.setSwimming(true);
                        return;
                    }

                    mc.player.setSwimming(true);
                    return;
                }
            }
        }
    }

    private boolean hasCombatTarget() {
        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura != null && attackAura.isEnabled() && attackAura.target != null) {
            return true;
        } else {
            TriggerBot triggerBot = TriggerBot.getInstance();
            if (triggerBot != null) {
                return triggerBot.isEnabled() ? triggerBot.findTarget() != null : false;
            } else {
                return false;
            }
        }
    }
}
