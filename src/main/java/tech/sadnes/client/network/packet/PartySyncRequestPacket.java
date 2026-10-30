package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PartySyncRequestPacket implements ServerboundPacket<ServerMainPacketListener> {
    public PartySyncRequestPacket() {
    }

    public PartySyncRequestPacket(PacketBuffer packetBuffer) {
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.party().onPartySyncRequest(this);
    }
}
