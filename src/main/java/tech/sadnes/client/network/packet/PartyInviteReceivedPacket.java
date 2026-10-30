package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class PartyInviteReceivedPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final String inviter;
    private final String code;

    public PartyInviteReceivedPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readString(48), packetBuffer.readString(16));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.inviter, 48);
        packetBuffer.writeString(this.code, 16);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.party().onPartyInvite(this);
    }

    public String getInviter() {
        return this.inviter;
    }

    public String getCode() {
        return this.code;
    }

    public PartyInviteReceivedPacket(String string, String string2) {
        this.inviter = string;
        this.code = string2;
    }
}
