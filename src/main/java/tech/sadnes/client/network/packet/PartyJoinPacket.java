package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PartyJoinPacket implements ServerboundPacket<ServerMainPacketListener> {
    public static final int CODE_MAX = 16;
    private final String code;

    public PartyJoinPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readString(16));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.code, 16);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.party().onPartyJoin(this);
    }

    public String getCode() {
        return this.code;
    }

    public PartyJoinPacket(String string) {
        this.code = string;
    }
}
