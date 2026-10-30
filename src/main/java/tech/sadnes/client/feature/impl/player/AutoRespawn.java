package tech.sadnes.client.feature.impl.player;

import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.DeathScreenEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;

@FeatureInfo(
    name = "AutoRespawn",
    description = "feature.impl.player.autorespawn",
    displayName = "feature.impl.player.autorespawn",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_TWEAKS
)
public final class AutoRespawn extends Feature {
    @EventHandler
    public void onDeath(DeathScreenEvent deathScreenEvent) {
        if (mc.player != null) {
            mc.player.requestRespawn();
        }
    }
}
