package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerAuthPacketListener;

public final class AuthRequestPacket implements ServerboundPacket<ServerAuthPacketListener> {
    public static final int USERNAME_MAX = 48;
    public static final int HWID_MAX = 4096;
    public static final int SUBSCRIPTION_MAX = 32;
    public static final int AVATAR_MAX = 4096;
    public static final int CLIENT_MAX = 64;
    private final int uid;
    private final String username;
    private final UserRole role;
    private final String hwid;
    private final String subscriptionEndDate;
    private final String avatarUrl;
    private final String clientName;

    public AuthRequestPacket(int i, String string, UserRole userRole, String string2, String string3, String string4, String string5) {
        this.uid = i;
        this.username = string;
        this.role = userRole;
        this.hwid = string2;
        this.subscriptionEndDate = string3;
        this.avatarUrl = string4;
        this.clientName = string5;
    }

    public AuthRequestPacket(PacketBuffer packetBuffer) {
        this.uid = packetBuffer.readVarInt();
        this.username = packetBuffer.readString(48);
        this.role = UserRole.read(packetBuffer);
        this.hwid = packetBuffer.readString(4096);
        this.subscriptionEndDate = packetBuffer.readString(32);
        this.avatarUrl = packetBuffer.readString(4096);
        this.clientName = packetBuffer.readString(64);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.uid);
        packetBuffer.writeString(this.username, 48);
        UserRole.write(packetBuffer, this.role);
        packetBuffer.writeString(this.hwid, 4096);
        packetBuffer.writeString(this.subscriptionEndDate, 32);
        packetBuffer.writeString(this.avatarUrl, 4096);
        packetBuffer.writeString(this.clientName, 64);
    }

    public void handle(ServerAuthPacketListener serverAuthPacketListener) {
        serverAuthPacketListener.onAuthRequest(this);
    }

    public int getUid() {
        return this.uid;
    }

    public String getUsername() {
        return this.username;
    }

    public UserRole getRole() {
        return this.role;
    }

    public String getHwid() {
        return this.hwid;
    }

    public String getSubscriptionEndDate() {
        return this.subscriptionEndDate;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    public String getClientName() {
        return this.clientName;
    }
}
