package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class PlayerStateResponsePacket implements ClientboundPacket<ClientMainPacketListener> {
    private final int requestId;
    private final String login;
    private final PlayerState state;

    public PlayerStateResponsePacket(PacketBuffer packetBuffer) {
        this.requestId = packetBuffer.readVarInt();
        this.login = packetBuffer.readString(48);
        this.state = packetBuffer.readBoolean() ? PlayerState.read(packetBuffer) : null;
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.login, 48);
        packetBuffer.writeBoolean(this.state != null);
        if (this.state != null) {
            this.state.write(packetBuffer);
        }
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.players().onPlayerStateResponse(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getLogin() {
        return this.login;
    }

    public PlayerState getState() {
        return this.state;
    }

    public PlayerStateResponsePacket(int i, String string, PlayerState playerState) {
        this.requestId = i;
        this.login = string;
        this.state = playerState;
    }
}
