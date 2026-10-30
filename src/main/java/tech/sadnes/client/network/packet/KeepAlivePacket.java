package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class KeepAlivePacket implements ClientboundPacket<ClientMainPacketListener> {
    private final long id;
    private final long serverTime;

    public KeepAlivePacket(long l, long l2) {
        this.id = l;
        this.serverTime = l2;
    }

    public KeepAlivePacket(PacketBuffer packetBuffer) {
        this.id = packetBuffer.readLong();
        this.serverTime = packetBuffer.readLong();
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeLong(this.id);
        packetBuffer.writeLong(this.serverTime);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.connection().onKeepAlive(this);
    }

    public long getId() {
        return this.id;
    }

    public long getServerTime() {
        return this.serverTime;
    }
}
