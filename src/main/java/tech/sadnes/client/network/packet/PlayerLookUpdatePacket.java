package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PlayerLookUpdatePacket implements ServerboundPacket<ServerMainPacketListener> {
    private final PlayerLook look;

    public PlayerLookUpdatePacket(PacketBuffer packetBuffer) {
        this(PlayerLook.read(packetBuffer));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        this.look.write(packetBuffer);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.party().onPlayerLook(this);
    }

    public PlayerLook getLook() {
        return this.look;
    }

    public PlayerLookUpdatePacket(PlayerLook playerLook) {
        this.look = playerLook;
    }
}
