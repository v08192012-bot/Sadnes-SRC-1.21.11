package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class RequestFavoritesPacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;

    public RequestFavoritesPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onRequestFavorites(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public RequestFavoritesPacket(int i) {
        this.requestId = i;
    }
}
