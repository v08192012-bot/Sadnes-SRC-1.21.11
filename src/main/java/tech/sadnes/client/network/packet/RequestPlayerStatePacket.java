package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class RequestPlayerStatePacket implements ServerboundPacket<ServerMainPacketListener> {
    private final int requestId;
    private final String login;
    private final boolean subscribe;

    public RequestPlayerStatePacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readString(48), packetBuffer.readBoolean());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeString(this.login, 48);
        packetBuffer.writeBoolean(this.subscribe);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.players().onPlayerStateRequest(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public String getLogin() {
        return this.login;
    }

    public boolean isSubscribe() {
        return this.subscribe;
    }

    public RequestPlayerStatePacket(int i, String string, boolean flag) {
        this.requestId = i;
        this.login = string;
        this.subscribe = flag;
    }
}
