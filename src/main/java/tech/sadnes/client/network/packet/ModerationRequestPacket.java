package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.ServerboundPacket;
import tech.sadnes.client.network.listener.ServerMainPacketListener;

public final class ModerationRequestPacket implements ServerboundPacket<ServerMainPacketListener> {
    public static final int MAX_REASON_LENGTH = 256;
    private final ModerationAction action;
    private final int targetId;
    private final String reason;
    private final long duration;

    public ModerationRequestPacket(ModerationAction moderationAction, int i, String string, long l) {
        this.action = moderationAction;
        this.targetId = i;
        this.reason = string;
        this.duration = l;
    }

    public ModerationRequestPacket(PacketBuffer packetBuffer) {
        this.action = packetBuffer.readEnum(ModerationAction.class);
        this.targetId = packetBuffer.readVarInt();
        this.reason = packetBuffer.readString(256);
        this.duration = packetBuffer.readVarLong();
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeEnum(this.action);
        packetBuffer.writeVarInt(this.targetId);
        packetBuffer.writeString(this.reason, 256);
        packetBuffer.writeVarLong(this.duration);
    }

    public void handle(ServerMainPacketListener serverMainPacketListener) {
        serverMainPacketListener.moderation().onModerationRequest(this);
    }

    public ModerationAction getAction() {
        return this.action;
    }

    public int getTargetId() {
        return this.targetId;
    }

    public String getReason() {
        return this.reason;
    }

    public long getDuration() {
        return this.duration;
    }
}
