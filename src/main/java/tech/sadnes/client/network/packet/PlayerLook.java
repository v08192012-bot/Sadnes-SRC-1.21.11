package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.PacketBuffer;

public record PlayerLook(String model, List<String> cosmetics) {
    public static final int MODEL_MAX = 64;
    public static final int COSMETICS_MAX = 16;
    public static final int COSMETIC_MAX = 64;
    public static final PlayerLook NONE = new PlayerLook("", List.of());

    public PlayerLook(String model, List<String> cosmetics) {
        cosmetics = List.copyOf(cosmetics);
        this.model = model;
        this.cosmetics = cosmetics;
    }

    public static PlayerLook read(PacketBuffer packetBuffer) {
        return new PlayerLook(packetBuffer.readString(64), packetBuffer.readList(16, packetBuffer2 -> packetBuffer2.readString(64)));
    }

    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.model, 64);
        packetBuffer.writeList(this.cosmetics, 16, (packetBuffer2, string) -> packetBuffer2.writeString(string, 64));
    }
}
