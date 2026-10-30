package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;

public record ShareActivation(int uid, String username, String code, long activatedMs) {
    public static final int NAME_MAX = 48;
    public static final int LIST_MAX = 256;

    public static ShareActivation read(PacketBuffer packetBuffer) {
        return new ShareActivation(packetBuffer.readVarInt(), packetBuffer.readString(48), packetBuffer.readString(32), packetBuffer.readVarLong());
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.uid);
        packetBuffer.writeString(this.username, 48);
        packetBuffer.writeString(this.code, 32);
        packetBuffer.writeVarLong(this.activatedMs);
    }
}
