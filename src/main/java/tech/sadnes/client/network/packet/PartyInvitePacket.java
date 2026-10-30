package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class PartyInvitePacket implements ServerboundPacket<ServerMainPacketListener> {
    private final String username;

    public PartyInvitePacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readString(48));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.username, 48);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.party().onPartyInvite(this);
    }

    public String getUsername() {
        return this.username;
    }

    public PartyInvitePacket(String string) {
        this.username = string;
    }
}
