package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;

public record PartyMember(int uid, String username, boolean online) {
    public static final int USERNAME_MAX = 48;

    public static PartyMember read(PacketBuffer packetBuffer) {
        return new PartyMember(packetBuffer.readVarInt(), packetBuffer.readString(48), packetBuffer.readBoolean());
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.uid);
        packetBuffer.writeString(this.username, 48);
        packetBuffer.writeBoolean(this.online);
    }
}
