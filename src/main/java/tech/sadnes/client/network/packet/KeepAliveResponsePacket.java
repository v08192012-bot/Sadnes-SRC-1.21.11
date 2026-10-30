package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class KeepAliveResponsePacket implements ServerboundPacket<ServerMainPacketListener> {
    private final long id;

    public KeepAliveResponsePacket(long l) {
        this.id = l;
    }

    public KeepAliveResponsePacket(PacketBuffer packetBuffer) {
        this.id = packetBuffer.readLong();
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeLong(this.id);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.connection().onKeepAliveResponse(this);
    }

    public long getId() {
        return this.id;
    }
}
