package tech.sadnes.client.network;

import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

public final class PacketCodec {
    private static final Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Decoder DECODER = Base64.getUrlDecoder();

    private PacketCodec() {
    }

    public static String encode(Packet<?> packet) {
        return encodeWithId(PacketRegistry.getId(packet), packet);
    }

    public static String encode(PacketType<?> packetType, Packet<?> packet) {
        return encodeWithId(packetType.id(), packet);
    }

    private static String encodeWithId(int i, Packet<?> packet) {
        PacketBuffer packetBuffer = PacketBuffer.create();
        packetBuffer.writeVarInt(i);
        packet.write(packetBuffer);
        return ENCODER.encodeToString(Arrays.copyOf(packetBuffer.getBuffer(), packetBuffer.getWriterIndex()));
    }

    public static Packet<?> decode(ProtocolState protocolState, PacketDirection packetDirection, String string) {
        byte[] bytes;
        try {
            bytes = DECODER.decode(string);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ProtocolException("bad base64");
        }

        PacketBuffer packetBuffer = PacketBuffer.wrap(bytes);
        int i = packetBuffer.readVarInt();
        PacketType packetType = PacketRegistry.getType(protocolState, packetDirection, i);
        Packet packet = packetType.decode(packetBuffer);
        if (packetBuffer.readableBytes() != 0) {
            throw new ProtocolException("trailing bytes after " + packetType.clazz().getSimpleName());
        } else {
            return packet;
        }
    }
}
