package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientAuthPacketListener;

public final class AuthResultPacket implements ClientboundPacket<ClientAuthPacketListener> {
    public static final int MESSAGE_MAX = 256;
    private final boolean success;
    private final long sessionId;
    private final String message;

    public AuthResultPacket(boolean flag, long l, String string) {
        this.success = flag;
        this.sessionId = l;
        this.message = string;
    }

    public AuthResultPacket(PacketBuffer packetBuffer) {
        this.success = packetBuffer.readBoolean();
        this.sessionId = packetBuffer.readLong();
        this.message = packetBuffer.readString(256);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBoolean(this.success);
        packetBuffer.writeLong(this.sessionId);
        packetBuffer.writeString(this.message, 256);
    }

    public void handle(ClientAuthPacketListener clientAuthPacketListener) {
        clientAuthPacketListener.onAuthResult(this);
    }

    public boolean isSuccess() {
        return this.success;
    }

    public long getSessionId() {
        return this.sessionId;
    }

    public String getMessage() {
        return this.message;
    }
}
