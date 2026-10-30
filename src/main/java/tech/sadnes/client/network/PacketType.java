package tech.sadnes.client.network;

import java.util.function.Function;

public record PacketType<P extends Packet<?>>(ProtocolState state, PacketDirection direction, int id, Class<P> clazz, Function<PacketBuffer, P> decoder) {
    public P decode(PacketBuffer packetBuffer) {
        return this.decoder.apply(packetBuffer);
    }
}
