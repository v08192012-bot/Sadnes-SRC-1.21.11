package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;

public record CloudConfigEntry(
    String id, ConfigKind kind, String name, int size, long modifiedAtMs, byte[] meta, boolean shared, String ownerName, String shareId, boolean isPublic
) {
    public static final int ID_MAX = 32;
    public static final int NAME_MAX = 64;
    public static final int OWNER_MAX = 48;
    public static final int SHARE_ID_MAX = 32;
    public static final int META_MAX = 8192;
    public static final int JSON_MAX = 1048576;
    public static final int MESSAGE_MAX = 256;
    public static final int LIST_MAX = 256;

    public static CloudConfigEntry read(PacketBuffer packetBuffer) {
        return new CloudConfigEntry(
            packetBuffer.readString(32),
            ConfigKind.byId(packetBuffer.readVarInt()),
            packetBuffer.readString(64),
            packetBuffer.readVarInt(),
            packetBuffer.readVarLong(),
            packetBuffer.readByteArray(8192),
            packetBuffer.readBoolean(),
            packetBuffer.readString(48),
            packetBuffer.readString(32),
            packetBuffer.readBoolean()
        );
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.id, 32);
        packetBuffer.writeVarInt(this.kind.getId());
        packetBuffer.writeString(this.name, 64);
        packetBuffer.writeVarInt(this.size);
        packetBuffer.writeVarLong(this.modifiedAtMs);
        packetBuffer.writeByteArray(this.meta, 8192);
        packetBuffer.writeBoolean(this.shared);
        packetBuffer.writeString(this.ownerName, 48);
        packetBuffer.writeString(this.shareId, 32);
        packetBuffer.writeBoolean(this.isPublic);
    }
}
