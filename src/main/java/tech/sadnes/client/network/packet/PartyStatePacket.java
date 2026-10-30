package tech.sadnes.client.network.packet;

import java.util.List;
import tech.sadnes.client.network.ClientboundPacket;
import tech.sadnes.client.network.PacketBuffer;
import tech.sadnes.client.network.listener.ClientMainPacketListener;

public final class PartyStatePacket implements ClientboundPacket<ClientMainPacketListener> {
    public static final int CODE_MAX = 16;
    public static final int MEMBERS_MAX = 32;
    private final boolean inParty;
    private final String code;
    private final int leaderUid;
    private final List<PartyMember> members;

    public PartyStatePacket(boolean flag, String string, int i, List<PartyMember> partyMembers) {
        this.inParty = flag;
        this.code = string;
        this.leaderUid = i;
        this.members = List.copyOf(partyMembers);
    }

    public PartyStatePacket(PacketBuffer packetBuffer) {
        this.inParty = packetBuffer.readBoolean();
        this.code = packetBuffer.readString(16);
        this.leaderUid = packetBuffer.readVarInt();
        this.members = packetBuffer.readList(32, PartyMember::read);
    }

    public static PartyStatePacket empty() {
        return new PartyStatePacket(false, "", 0, List.of());
    }

    @Override
    public void write(PacketBuffer packetBuffer) {
        packetBuffer.writeBoolean(this.inParty);
        packetBuffer.writeString(this.code, 16);
        packetBuffer.writeVarInt(this.leaderUid);
        packetBuffer.writeList(this.members, 32, (packetBuffer2, partyMember) -> partyMember.write(packetBuffer2));
    }

    public void handle(ClientMainPacketListener clientMainPacketListener) {
        clientMainPacketListener.party().onPartyState(this);
    }

    public boolean isInParty() {
        return this.inParty;
    }

    public String getCode() {
        return this.code;
    }

    public int getLeaderUid() {
        return this.leaderUid;
    }

    public List<PartyMember> getMembers() {
        return this.members;
    }
}
