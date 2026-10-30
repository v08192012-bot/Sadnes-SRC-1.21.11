package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class PlayerStatesPacket implements ClientboundPacket<ClientMainPacketListener> {
    public static final int MAX_ENTRIES = 64;
    private final List<PlayerStateEntry> entries;

    public PlayerStatesPacket(List<PlayerStateEntry> playerStateEntries) {
        this.entries = List.copyOf(playerStateEntries);
    }

    public PlayerStatesPacket(PacketBuffer packetBuffer) {
        this(packetBuffer.readList(64, PlayerStateEntry::read));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeList(this.entries, 64, (packetBuffer2, playerStateEntry) -> playerStateEntry.write(packetBuffer2));
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.players().onPlayerStates(this);
    }

    public List<PlayerStateEntry> getEntries() {
        return this.entries;
    }
}
