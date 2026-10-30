package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class UserSearchPacket implements ServerboundPacket<ServerMainPacketListener> {
    public static final int LOGINS_MAX = 32;
    private final String query;
    private final List<String> logins;

    public UserSearchPacket(String string, List<String> strings) {
        this.query = string;
        this.logins = List.copyOf(strings);
    }

    public UserSearchPacket(PacketBuffer packetBuffer) {
        this.query = packetBuffer.readString(48);
        this.logins = packetBuffer.readList(32, packetBuffer2 -> packetBuffer2.readString(48));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.query, 48);
        packetBuffer.writeList(this.logins, 32, (packetBuffer2, string) -> packetBuffer2.writeString(string, 48));
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.players().onUserSearch(this);
    }

    public String getQuery() {
        return this.query;
    }

    public List<String> getLogins() {
        return this.logins;
    }
}
