package tech.sadnes.client.feature.impl.player;

import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;

@FeatureInfo(
    name = "Immortality",
    description = "feature.impl.player.immortality",
    displayName = "feature.impl.player.immortality",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_TWEAKS
)
public final class Immortality extends Feature {
    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isSend() && packetEvent.getPacket() instanceof TeleportConfirmC2SPacket) {
            packetEvent.cancel();
        }
    }
}
