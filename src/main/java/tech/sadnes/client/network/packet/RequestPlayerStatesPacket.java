package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class RequestPlayerStatesPacket implements ServerboundPacket<ServerMainPacketListener> {
    public static final int MAX_LOGINS = 64;
    private final List<String> logins;

    public RequestPlayerStatesPacket(List<String> strings) {
        this.logins = List.copyOf(strings);
    }

    public RequestPlayerStatesPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readList(64, packetBuffer2 -> packetBuffer2.readString(48)));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeList(this.logins, 64, (packetBuffer2, string) -> packetBuffer2.writeString(string, 48));
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.players().onPlayerStatesRequest(this);
    }

    public List<String> getLogins() {
        return this.logins;
    }
}
