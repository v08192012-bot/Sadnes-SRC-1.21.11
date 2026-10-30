package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientHandshakePacketListener;

public final class HandshakeResultPacket implements ClientboundPacket<ClientHandshakePacketListener> {
    public static final int MESSAGE_MAX = 256;
    private final boolean accepted;
    private final int protocolVersion;
    private final String message;

    public HandshakeResultPacket(boolean flag, int i, String string) {
        this.accepted = flag;
        this.protocolVersion = i;
        this.message = string;
    }

    public HandshakeResultPacket(PacketBuffer packetBuffer) {
        this.accepted = packetBuffer.readBoolean();
        this.protocolVersion = packetBuffer.readVarInt();
        this.message = packetBuffer.readString(256);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBoolean(this.accepted);
        packetBuffer.writeVarInt(this.protocolVersion);
        packetBuffer.writeString(this.message, 256);
    }

    public void handle(ClientHandshakePacketListener clientHandshakePacketListener) {
        clientHandshakePacketListener.onHandshakeResult(this);
    }

    public boolean isAccepted() {
        return this.accepted;
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public String getMessage() {
        return this.message;
    }
}
