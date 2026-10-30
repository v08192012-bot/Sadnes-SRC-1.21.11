package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class ShareConfigPacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final String configId;
    private final boolean makePublic;

    public ShareConfigPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readString(32), packetBuffer.readBoolean());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.configId, 32);
        packetBuffer.writeBoolean(this.makePublic);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onShareConfig(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getConfigId() {
        return this.configId;
    }

    public boolean isMakePublic() {
        return this.makePublic;
    }

    public ShareConfigPacket(int i, String string, boolean flag) {
        this.requestId = i;
        this.configId = string;
        this.makePublic = flag;
    }
}
