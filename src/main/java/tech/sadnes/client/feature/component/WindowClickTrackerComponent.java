package tech.sadnes.client.feature.component;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.math.TimerUtil;

public final class WindowClickTrackerComponent extends FeatureComponent {
    private final TimerUtil clickTimer = new TimerUtil();
    private volatile boolean windowClosed = false;

    public static WindowClickTrackerComponent getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(WindowClickTrackerComponent.class);
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (mc.player != null) {
            if (packetEvent.isGhost()) {
                this.handlePacket(packetEvent.getPacket());
            }
        }
    }

    public void handlePacket(Packet<?> packet) {
        if (packet instanceof ClickSlotC2SPacket) {
            this.clickTimer.reset();
            this.windowClosed = false;
        } else {
            if (packet instanceof CloseHandledScreenC2SPacket || packet instanceof CloseScreenS2CPacket) {
                this.windowClosed = true;
            }
        }
    }

    public boolean isInteracting() {
        if (mc == null) {
            return false;
        } else {
            return !ServerManager.isFunTime() && !ServerManager.isHolyWorld() ? false : !this.clickTimer.hasPassed(250L) && !this.windowClosed;
        }
    }

    public TimerUtil getClickTimer() {
        return this.clickTimer;
    }

    public boolean isWindowClosed() {
        return this.windowClosed;
    }
}
