package tech.sadnes.client.network.packet;

import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class ModerationResultPacket implements ClientboundPacket<ClientMainPacketListener> {
    public static final int MAX_MESSAGE_LENGTH = 256;
    private final ModerationAction action;
    private final int targetId;
    private final boolean success;
    private final String message;

    public ModerationResultPacket(ModerationAction moderationAction, int i, boolean flag, String string) {
        this.action = moderationAction;
        this.targetId = i;
        this.success = flag;
        this.message = string;
    }

    public ModerationResultPacket(PacketBuffer packetBuffer) {
        this.action = packetBuffer.readEnum(ModerationAction.class);
        this.targetId = packetBuffer.readVarInt();
        this.success = packetBuffer.readBoolean();
        this.message = packetBuffer.readString(256);
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeEnum(this.action);
        packetBuffer.writeVarInt(this.targetId);
        packetBuffer.writeBoolean(this.success);
        packetBuffer.writeString(this.message, 256);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.moderation().onModerationResult(this);
    }

    public ModerationAction getAction() {
        return this.action;
    }

    public int getTargetId() {
        return this.targetId;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }
}
