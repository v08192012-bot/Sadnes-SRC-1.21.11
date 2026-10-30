package tech.sadnes.client.feature.impl.misc;

import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.player.PlayerEntity;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.TextSetting;

@FeatureInfo(name = "KillTalk", displayName = "feature.impl.misc.killtalk", category = Category.MISC, subcategory = Subcategory.MISC_CHAT)
public class KillTalk extends Feature {
    private final TextSetting message = new TextSetting("killtalk.message", "Игрок {name} был уничтожен!");
    private PlayerEntity lastTarget;

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (attackEntityEvent.getTarget() instanceof PlayerEntity playerEntity) {
            this.lastTarget = playerEntity;
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent entityRemoveEvent) {
        if (mc.player != null && this.lastTarget != null && entityRemoveEvent.getEntity() == this.lastTarget) {
            if (entityRemoveEvent.getReason() == RemovalReason.KILLED || this.lastTarget.getHealth() <= 0.0F || this.lastTarget.isDead()) {
                String string = this.message.getValue().replace("{name}", this.lastTarget.getGameProfile().name());
                if (string.startsWith("/")) {
                    mc.player.networkHandler.sendChatCommand(string.substring(1));
                    this.lastTarget = null;
                } else {
                    mc.player.networkHandler.sendChatMessage(string);
                    this.lastTarget = null;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.lastTarget = null;
    }

    public TextSetting getMessage() {
        return this.message;
    }

    public PlayerEntity getLastTarget() {
        return this.lastTarget;
    }
}
