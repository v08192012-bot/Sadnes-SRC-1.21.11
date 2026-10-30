package tech.sadnes.client.feature.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import tech.sadnes.api.mixin.accessor.PlayerInteractEntityC2SPacketAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.EntityRaycastEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;

@FeatureInfo(
    name = "NoFriendDamage",
    description = "feature.impl.misc.nofrienddamage",
    displayName = "feature.impl.misc.nofrienddamage",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SAFETY
)
public final class NoFriendDamage extends Feature {
    @EventHandler
    public void onAttack(EntityRaycastEvent entityRaycastEvent) {
        if (Sadnes.getInstance().getFriendManager().isFriend(entityRaycastEvent.getEntity().getName().getString())) {
            entityRaycastEvent.cancel();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isSend() && packetEvent.getPacket() instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
            PlayerInteractEntityC2SPacketAccessor playerInteractEntityC2SPacketAccessor = (PlayerInteractEntityC2SPacketAccessor)playerInteractEntityC2SPacket;
            Entity entity = mc.world.getEntityById(playerInteractEntityC2SPacketAccessor.sadnes$getEntityId());
            if (entity == null) {
                return;
            }

            if (Sadnes.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
                packetEvent.cancel();
            }
        }
    }
}
