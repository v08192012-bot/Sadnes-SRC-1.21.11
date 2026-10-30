package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;

public record ShareCode(String code, long createdMs, boolean spent, int usedByUid, String usedByName, long usedMs) {
    public static final int CODE_MAX = 32;
    public static final int NAME_MAX = 48;
    public static final int LIST_MAX = 128;

    public static ShareCode unspent(String string, long l) {
        return new ShareCode(string, l, false, 0, "", 0L);
    }

    public static ShareCode read(PacketBuffer packetBuffer) {
        return new ShareCode(packetBuffer.readString(32), packetBuffer.readVarLong(), packetBuffer.readBoolean(), packetBuffer.readVarInt(), packetBuffer.readString(48), packetBuffer.readVarLong());
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.code, 32);
        packetBuffer.writeVarLong(this.createdMs);
        packetBuffer.writeBoolean(this.spent);
        packetBuffer.writeVarInt(this.usedByUid);
        packetBuffer.writeString(this.usedByName, 48);
        packetBuffer.writeVarLong(this.usedMs);
    }
}
