package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class ImportSharedConfigPacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final String shareCode;

    public ImportSharedConfigPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readString(32));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.shareCode, 32);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onImportSharedConfig(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getShareCode() {
        return this.shareCode;
    }

    public ImportSharedConfigPacket(int i, String string) {
        this.requestId = i;
        this.shareCode = string;
    }
}
