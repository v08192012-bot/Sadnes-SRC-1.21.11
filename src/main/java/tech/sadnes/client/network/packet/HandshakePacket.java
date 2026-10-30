package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerHandshakePacketListener;

public final class HandshakePacket implements ServerboundPacket<ServerHandshakePacketListener> {
    public static final int CLIENT_NAME_MAX = 64;
    private final int protocolVersion;
    private final int minProtocolVersion;
    private final int clientVersion;
    private final String clientName;

    public HandshakePacket(int i, int i2, int i3, String string) {
        this.protocolVersion = i;
        this.minProtocolVersion = i2;
        this.clientVersion = i3;
        this.clientName = string;
    }

    public HandshakePacket(PacketBuffer packetBuffer) {
        this.protocolVersion = packetBuffer.readVarInt();
        this.minProtocolVersion = packetBuffer.readVarInt();
        this.clientVersion = packetBuffer.readVarInt();
        this.clientName = packetBuffer.readString(64);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.protocolVersion);
        packetBuffer.writeVarInt(this.minProtocolVersion);
        packetBuffer.writeVarInt(this.clientVersion);
        packetBuffer.writeString(this.clientName, 64);
    }

    public void handle(ServerHandshakePacketListener serverHandshakePacketListener) {
        serverHandshakePacketListener.onHandshake(this);
    }

    public int getProtocolVersion() {
        return this.protocolVersion;
    }

    public int getMinProtocolVersion() {
        return this.minProtocolVersion;
    }

    public int getClientVersion() {
        return this.clientVersion;
    }

    public String getClientName() {
        return this.clientName;
    }
}
