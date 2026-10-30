package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class UserSearchResultPacket implements ClientboundPacket<ClientMainPacketListener> {
    public static final int USERS_MAX = 128;
    private final String query;
    private final boolean lookup;
    private final List<UserInfo> users;

    public UserSearchResultPacket(String string, boolean flag, List<UserInfo> userInfos) {
        this.query = string;
        this.lookup = flag;
        this.users = List.copyOf(userInfos);
    }

    public UserSearchResultPacket(PacketBuffer packetBuffer) {
        this.query = packetBuffer.readString(48);
        this.lookup = packetBuffer.readBoolean();
        this.users = packetBuffer.readList(128, UserInfo::read);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.query, 48);
        packetBuffer.writeBoolean(this.lookup);
        packetBuffer.writeList(this.users, 128, (packetBuffer2, userInfo) -> userInfo.write(packetBuffer2));
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.players().onUserSearchResult(this);
    }

    public String getQuery() {
        return this.query;
    }

    public boolean isLookup() {
        return this.lookup;
    }

    public List<UserInfo> getUsers() {
        return this.users;
    }
}
