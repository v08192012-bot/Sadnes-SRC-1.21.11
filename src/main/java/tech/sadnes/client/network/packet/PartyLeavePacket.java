package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PartyLeavePacket implements ServerboundPacket<ServerMainPacketListener> {
    public PartyLeavePacket() {
    }

    public PartyLeavePacket(PacketBuffer packetBuffer) {
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.party().onPartyLeave(this);
    }
}
