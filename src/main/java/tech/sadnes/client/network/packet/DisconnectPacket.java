package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class DisconnectPacket implements ClientboundPacket<ClientMainPacketListener> {
    public static final int REASON_MAX = 256;
    private final String reason;

    public DisconnectPacket(String string) {
        this.reason = string;
    }

    public DisconnectPacket(PacketBuffer packetBuffer) {
        this.reason = packetBuffer.readString(256);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.reason, 256);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.onDisconnect(this);
    }

    public String getReason() {
        return this.reason;
    }
}
