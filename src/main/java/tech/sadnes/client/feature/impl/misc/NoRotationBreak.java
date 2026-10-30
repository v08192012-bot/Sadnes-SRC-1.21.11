package tech.sadnes.client.feature.impl.misc;

import java.util.Set;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import tech.sadnes.api.mixin.accessor.PlayerPositionAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;

@FeatureInfo(
    name = "NoRotationBreak",
    description = "feature.impl.misc.norotationbreak",
    displayName = "feature.impl.misc.norotationbreak",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_TWEAKS
)
public final class NoRotationBreak extends Feature {
    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (mc.player != null) {
            if (packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket playerPositionLookS2CPacket) {
                Set set = playerPositionLookS2CPacket.relatives();
                if (set.isEmpty()) {
                    EntityPosition entityPosition = playerPositionLookS2CPacket.change();
                    PlayerPositionAccessor playerPositionAccessor = (PlayerPositionAccessor)(Object)entityPosition;
                    playerPositionAccessor.sadnes$setYaw(mc.player.getYaw());
                    playerPositionAccessor.sadnes$setPitch(mc.player.getPitch());
                }
            }
        }
    }
}
