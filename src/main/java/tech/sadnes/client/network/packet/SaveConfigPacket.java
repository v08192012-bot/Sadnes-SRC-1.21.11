package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class SaveConfigPacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final String configId;
    private final ConfigKind kind;
    private final String name;
    private final byte[] meta;
    private final byte[] data;

    public SaveConfigPacket(PacketBuffer packetBuffer) {
        this(
            packetBuffer.readVarInt(),
            packetBuffer.readString(32),
            ConfigKind.byId(packetBuffer.readVarInt()),
            packetBuffer.readString(64),
            packetBuffer.readByteArray(8192),
            packetBuffer.readByteArray(1048576)
        );
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.configId, 32);
        packetBuffer.writeVarInt(this.kind.getId());
        packetBuffer.writeString(this.name, 64);
        packetBuffer.writeByteArray(this.meta, 8192);
        packetBuffer.writeByteArray(this.data, 1048576);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.configs().onSaveConfig(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getConfigId() {
        return this.configId;
    }

    public ConfigKind getKind() {
        return this.kind;
    }

    public String getName() {
        return this.name;
    }

    public byte[] getMeta() {
        return this.meta;
    }

    public byte[] getData() {
        return this.data;
    }

    public SaveConfigPacket(int i, String string, ConfigKind configKind, String string2, byte[] bytes, byte[] bytes2) {
        this.requestId = i;
        this.configId = string;
        this.kind = configKind;
        this.name = string2;
        this.meta = bytes;
        this.data = bytes2;
    }
}
