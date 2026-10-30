package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class ConfigResultPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final int requestId;
    private final boolean success;
    private final String message;
    private final String configId;

    public ConfigResultPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readBoolean(), packetBuffer.readString(256), packetBuffer.readString(32));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeBoolean(this.success);
        packetBuffer.writeString(this.message, 256);
        packetBuffer.writeString(this.configId, 32);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.configs().onConfigResult(this);
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

    public String getConfigId() {
        return this.configId;
    }

    public ConfigResultPacket(int i, boolean flag, String string, String string2) {
        this.requestId = i;
        this.success = flag;
        this.message = string;
        this.configId = string2;
    }
}
