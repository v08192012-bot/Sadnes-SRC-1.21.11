package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class ConfigListPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final int requestId;
    private final List<CloudConfigEntry> entries;
    private final String activeId;

    public ConfigListPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readVarInt(), packetBuffer.readList(256, CloudConfigEntry::read), packetBuffer.readString(32));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeVarInt(this.requestId);
        packetBuffer.writeList(this.entries, 256, (packetBuffer2, cloudConfigEntry) -> cloudConfigEntry.write(packetBuffer2));
        packetBuffer.writeString(this.activeId, 32);
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.configs().onConfigList(this);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public List<CloudConfigEntry> getEntries() {
        return this.entries;
    }

    public String getActiveId() {
        return this.activeId;
    }

    public ConfigListPacket(int i, List<CloudConfigEntry> cloudConfigEntries, String string) {
        this.requestId = i;
        this.entries = cloudConfigEntries;
        this.activeId = string;
    }
}
