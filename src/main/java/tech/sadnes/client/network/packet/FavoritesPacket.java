package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class FavoritesPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final int requestId;
    private final List<String> favorites;

    public FavoritesPacket(int i, List<String> strings) {
        this.requestId = i;
        this.favorites = List.copyOf(strings);
    }

    public FavoritesPacket(PacketBuffer packetBuffer) {
        this.requestId = packetBuffer.readVarInt();
        this.favorites = packetBuffer.readList(256, packetBuffer2 -> packetBuffer2.readString(32));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeList(this.favorites, 256, (packetBuffer2, string) -> packetBuffer2.writeString(string, 32));
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.configs().onFavorites(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public List<String> getFavorites() {
        return this.favorites;
    }
}
