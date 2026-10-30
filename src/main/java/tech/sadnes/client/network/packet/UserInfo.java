package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;

public record UserInfo(String login, String avatarUrl) {
    public static final int AVATAR_MAX = 256;

    public static UserInfo read(PacketBuffer packetBuffer) {
        return new UserInfo(packetBuffer.readString(48), packetBuffer.readString(256));
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.login, 48);
        packetBuffer.writeString(this.avatarUrl, 256);
    }
}
