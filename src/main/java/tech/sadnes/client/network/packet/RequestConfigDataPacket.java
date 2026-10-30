package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class RequestConfigDataPacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final String configId;

    public RequestConfigDataPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readString(32));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.configId, 32);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onRequestConfigData(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getConfigId() {
        return this.configId;
    }

    public RequestConfigDataPacket(int i, String string) {
        this.requestId = i;
        this.configId = string;
    }
}
