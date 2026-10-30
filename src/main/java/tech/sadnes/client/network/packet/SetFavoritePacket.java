package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class SetFavoritePacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final String name;
    private final boolean favorite;

    public SetFavoritePacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readString(32), packetBuffer.readBoolean());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.name, 32);
        packetBuffer.writeBoolean(this.favorite);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onSetFavorite(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getName() {
        return this.name;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public SetFavoritePacket(int i, String string, boolean flag) {
        this.requestId = i;
        this.name = string;
        this.favorite = flag;
    }
}
