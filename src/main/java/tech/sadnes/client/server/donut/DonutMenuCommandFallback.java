package tech.sadnes.client.server.donut;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.chat.ChatMessageUtil;

public final class DonutMenuCommandFallback extends FeatureComponent {
    private volatile QuickAction pendingCommand;
    private volatile long commandSentTime;
    private long lastFallbackTime;

    @EventHandler(priority = 500)
    public void onPacket(PacketEvent packetEvent) {
        Packet packet = packetEvent.getPacket();
        if (packetEvent.isReceive()) {
            if (this.pendingCommand != null && this.isServerResponse(packet)) {
                this.pendingCommand = null;
            }
        } else if (packet instanceof CommandExecutionC2SPacket commandExecutionC2SPacket && mc.isOnThread() && mc.player != null && ServerManager.isDonut()) {
            QuickAction quickAction = MovementPauseComponent.isPaused() ? null : QuickAction.byId(commandExecutionC2SPacket.command());
            long l = System.currentTimeMillis();
            if (quickAction != null && quickAction != this.pendingCommand && l - this.lastFallbackTime >= 5000L) {
                this.pendingCommand = quickAction;
                this.commandSentTime = l;
            }
        }
    }

    private boolean isServerResponse(Packet<?> packet) {
        return packet instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay() && ChatMessageUtil.isErrorMessage(ChatMessageUtil.getPlainText(gameMessageS2CPacket))
            ? true
            : packet instanceof OpenScreenS2CPacket || packet instanceof CloseScreenS2CPacket;
    }

    @EventHandler
    public void onTick(WorldTickEvent worldTickEvent) {
        QuickAction quickAction = this.pendingCommand;
        if (quickAction != null && System.currentTimeMillis() - this.commandSentTime >= Math.max(1500, ServerManager.getPing() * 3)) {
            this.pendingCommand = null;
            if (quickAction.run()) {
                this.lastFallbackTime = System.currentTimeMillis();
            }
        }
    }

    @EventHandler
    public void onWorldJoin(GameJoinEvent gameJoinEvent) {
        this.pendingCommand = null;
    }
}
