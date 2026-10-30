package tech.sadnes.client.feature.impl.player;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;

@FeatureInfo(
    name = "Luckboost",
    description = "feature.impl.player.luckboost",
    displayName = "feature.impl.player.luckboost",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_TWEAKS
)
public final class Luckboost extends Feature {
    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            RegistryEntry registryEntry = StatusEffects.LUCK;
            StatusEffectInstance statusEffectInstance = clientPlayerEntity.getStatusEffect(registryEntry);
            if (statusEffectInstance == null || statusEffectInstance.getDuration() <= 0) {
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                RegistryEntry registryEntry2 = StatusEffects.LUCK;
                StatusEffectInstance statusEffectInstance2 = new StatusEffectInstance(registryEntry2, 200, 0);
                clientPlayerEntity2.addStatusEffect(statusEffectInstance2);
                return;
            }
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            RegistryEntry registryEntry = StatusEffects.LUCK;
            clientPlayerEntity.removeStatusEffect(registryEntry);
        }

        super.onEnable();
    }
}
