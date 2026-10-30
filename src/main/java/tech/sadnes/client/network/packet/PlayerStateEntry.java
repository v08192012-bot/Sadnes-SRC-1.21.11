package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;

public record PlayerStateEntry(String login, PlayerState data) {
    public static final int LOGIN_MAX = 48;

    public static PlayerStateEntry read(PacketBuffer packetBuffer) {
        return new PlayerStateEntry(packetBuffer.readString(48), PlayerState.read(packetBuffer));
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.login, 48);
        this.data.write(packetBuffer);
    }
}
