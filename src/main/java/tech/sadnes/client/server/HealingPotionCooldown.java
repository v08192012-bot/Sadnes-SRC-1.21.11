package tech.sadnes.client.server;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.component.ItemCooldownTrackerComponent;

public final class HealingPotionCooldown extends FeatureComponent {
    public static final Identifier COOLDOWN_ID = Identifier.of("sadnes", "healing_potion");
    private static final int COOLDOWN_TICKS = 400;
    private long lastUseCount;
    private boolean cooldownActive;

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            long l = ItemCooldownTrackerComponent.getInstance().getRemainingMs(mc.player, CooldownItem.HEALING_POTION);
            boolean flag = l > this.lastUseCount;
            this.lastUseCount = l;
            ItemCooldownManager itemCooldownManager = mc.player.getItemCooldownManager();
            boolean flag2;
            if (!ServerManager.isFunTime()) {
                flag2 = ServerManager.isSpookyTime();
            } else {
                flag2 = true;
            }

            if (flag2 && ServerManager.isInPvp()) {
                if (flag) {
                    itemCooldownManager.set(COOLDOWN_ID, 400);
                    this.cooldownActive = true;
                }
            } else {
                if (this.cooldownActive) {
                    itemCooldownManager.remove(COOLDOWN_ID);
                    this.cooldownActive = false;
                }
            }
        }
    }
}
