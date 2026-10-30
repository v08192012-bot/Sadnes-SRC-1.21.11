package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class PartyResultPacket implements ClientboundPacket<ClientMainPacketListener> {
    public static final int MESSAGE_MAX = 256;
    private final boolean success;
    private final String message;

    public PartyResultPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readBoolean(), packetBuffer.readString(256));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBoolean(this.success);
        packetBuffer.writeString(this.message, 256);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.party().onPartyResult(this);
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public PartyResultPacket(boolean flag, String string) {
        this.success = flag;
        this.message = string;
    }
}
