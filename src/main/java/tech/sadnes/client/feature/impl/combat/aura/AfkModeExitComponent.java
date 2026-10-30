package tech.sadnes.client.feature.impl.combat.aura;

import java.util.Locale;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.util.math.StopWatch;

public final class AfkModeExitComponent extends FeatureComponent {
    private static final long HOLD_DURATION_MS = 250L;
    private final StopWatch holdTimer = new StopWatch();
    private volatile boolean afkDetected;
    private boolean holdingForward;

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
            if (gameMessageS2CPacket.content().getString().toLowerCase(Locale.ROOT).contains("режиме afk")) {
                this.afkDetected = true;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player == null) {
            this.releaseForward();
        } else {
            if (this.afkDetected) {
                this.afkDetected = false;
                AuctionUtil.closeScreen();
                this.holdTimer.reset();
                this.holdingForward = true;
            }

            if (this.holdingForward) {
                if (this.holdTimer.isElapsed(250L)) {
                    this.releaseForward();
                } else {
                    mc.options.forwardKey.setPressed(true);
                }
            }
        }
    }

    private void releaseForward() {
        if (this.holdingForward) {
            this.holdingForward = false;
            mc.options.forwardKey.setPressed(false);
        }
    }
}
