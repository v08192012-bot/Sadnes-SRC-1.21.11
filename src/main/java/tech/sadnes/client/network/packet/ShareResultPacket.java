package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class ShareResultPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final int requestId;
    private final boolean success;
    private final String message;
    private final String shareId;
    private final boolean isPublic;

    public ShareResultPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readBoolean(), packetBuffer.readString(256), packetBuffer.readString(32), packetBuffer.readBoolean());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeBoolean(this.success);
        packetBuffer.writeString(this.message, 256);
        packetBuffer.writeString(this.shareId, 32);
        packetBuffer.writeBoolean(this.isPublic);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.configs().onShareResult(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public String getShareId() {
        return this.shareId;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public ShareResultPacket(int i, boolean flag, String string, String string2, boolean flag2) {
        this.requestId = i;
        this.success = flag;
        this.message = string;
        this.shareId = string2;
        this.isPublic = flag2;
    }
}
