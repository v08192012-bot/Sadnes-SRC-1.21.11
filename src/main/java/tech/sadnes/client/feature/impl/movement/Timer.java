package tech.sadnes.client.feature.impl.movement;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;

@FeatureInfo(
    name = "Timer",
    description = "feature.impl.movement.timer",
    displayName = "feature.impl.movement.timer",
    category = Category.MOVEMENT,
    subcategory = Subcategory.SPEED
)
public final class Timer extends Feature {
    private final NumberSetting amount = new NumberSetting("timer.amount", 2.0F, 0.1F, 5.0F, 0.01F, Unit.EMPTY);

    public static Timer getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(Timer.class);
    }

    @Override
    public void onEnable() {
        Sadnes.guiScale = 1.0F;
        super.onEnable();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            if (!this.isUsingItem()) {
                Sadnes.guiScale = this.amount.getValue();
            } else {
                Sadnes.guiScale = 1.0F;
            }
        }
    }

    boolean isUsingItem() {
        return mc.player.getItemUseTime() != 0;
    }
}
