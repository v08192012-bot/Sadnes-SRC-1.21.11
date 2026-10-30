package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class RequestConfigListPacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final ConfigKind kind;

    public RequestConfigListPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), ConfigKind.byId(packetBuffer.readVarInt()));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeVarInt(this.kind.getId());
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onRequestConfigList(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public ConfigKind getKind() {
        return this.kind;
    }

    public RequestConfigListPacket(int i, ConfigKind configKind) {
        this.requestId = i;
        this.kind = configKind;
    }
}
