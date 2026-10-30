package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PartyDisbandPacket implements ServerboundPacket<ServerMainPacketListener> {
    public PartyDisbandPacket() {
    }

    public PartyDisbandPacket(PacketBuffer packetBuffer) {
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.party().onPartyDisband(this);
    }
}
