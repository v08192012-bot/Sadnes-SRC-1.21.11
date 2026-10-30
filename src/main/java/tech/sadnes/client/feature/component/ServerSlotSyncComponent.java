package tech.sadnes.client.feature.component;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import tech.sadnes.api.mixin.accessor.PlayerInventoryAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.util.math.StopWatch;

public class ServerSlotSyncComponent extends FeatureComponent {
    public static boolean active;
    public boolean serverChanged;
    public int targetSlot = -1;
    public final StopWatch timer = new StopWatch();

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (!packetEvent.isSend()) {
            Packet packet = packetEvent.getPacket();
            if (packet instanceof UpdateSelectedSlotS2CPacket) {
                this.serverChanged = true;
            }
        }
    }

    @EventHandler(priority = 500)
    public void onTick(TickEvent tickEvent) {
        if (this.serverChanged && this.targetSlot != -1) {
            active = true;
            ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(this.targetSlot);
            if (this.timer.hasPassed(200.0)) {
                this.serverChanged = false;
                this.targetSlot = -1;
                active = false;
                return;
            }
        } else {
            reset();
        }
    }

    public static void reset() {
        Sadnes.getInstance().getComponentManager().getComponent(ServerSlotSyncComponent.class).timer.reset();
    }

    public static void setTargetSlot(int i) {
        Sadnes.getInstance().getComponentManager().getComponent(ServerSlotSyncComponent.class).targetSlot = i;
    }
}
