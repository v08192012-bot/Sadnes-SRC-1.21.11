package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PartyKickPacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int uid;

    public PartyKickPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.uid);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.party().onPartyKick(this);
    }

    public int getUid() {
        return this.uid;
    }

    public PartyKickPacket(int i) {
        this.uid = i;
    }
}
