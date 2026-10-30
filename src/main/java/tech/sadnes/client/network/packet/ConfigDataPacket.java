package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class ConfigDataPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final int requestId;
    private final boolean found;
    private final String configId;
    private final byte[] data;

    public ConfigDataPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readBoolean(), packetBuffer.readString(32), packetBuffer.readByteArray(1048576));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeBoolean(this.found);
        packetBuffer.writeString(this.configId, 32);
        packetBuffer.writeByteArray(this.data, 1048576);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.configs().onConfigData(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public boolean isFound() {
        return this.found;
    }

    public String getConfigId() {
        return this.configId;
    }

    public byte[] getData() {
        return this.data;
    }

    public ConfigDataPacket(int i, boolean flag, String string, byte[] bytes) {
        this.requestId = i;
        this.found = flag;
        this.configId = string;
        this.data = bytes;
    }
}
