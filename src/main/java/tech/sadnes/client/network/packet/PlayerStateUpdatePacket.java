package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PlayerStateUpdatePacket implements ServerboundPacket<ServerMainPacketListener> {
    private final PlayerState state;

    public PlayerStateUpdatePacket(PacketBuffer packetBuffer) {
        this(PlayerState.read(packetBuffer));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        this.state.write(packetBuffer);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.players().onPlayerStateUpdate(this);
    }

    public PlayerState getState() {
        return this.state;
    }

    public PlayerStateUpdatePacket(PlayerState playerState) {
        this.state = playerState;
    }
}
