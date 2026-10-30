package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class GenerateShareCodePacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final String configId;

    public GenerateShareCodePacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readString(32));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.configId, 32);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onGenerateShareCode(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getConfigId() {
        return this.configId;
    }

    public GenerateShareCodePacket(int i, String string) {
        this.requestId = i;
        this.configId = string;
    }
}
