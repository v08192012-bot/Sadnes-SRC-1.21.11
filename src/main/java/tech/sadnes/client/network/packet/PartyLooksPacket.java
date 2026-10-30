package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class PartyLooksPacket implements ClientboundPacket<ClientMainPacketListener> {
    private final List<PartyLooksPacket.Entry> looks;

    public PartyLooksPacket(List<PartyLooksPacket.Entry> entries) {
        this.looks = List.copyOf(entries);
    }

    public PartyLooksPacket(PacketBuffer packetBuffer) {
        this.looks = packetBuffer.readList(32, packetBuffer2 -> new PartyLooksPacket.Entry(packetBuffer2.readVarInt(), PlayerLook.read(packetBuffer2)));
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeList(this.looks, 32, (packetBuffer2, entry) -> {
            packetBuffer2.writeVarInt(entry.uid());
            entry.look().write(packetBuffer2);
        });
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.party().onPartyLooks(this);
    }

    public List<PartyLooksPacket.Entry> getLooks() {
        return this.looks;
    }

    public record Entry(int uid, PlayerLook look) {
    }
}
