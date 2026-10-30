package tech.sadnes.client.party;

import tech.sadnes.client.network.packet.PartyMember;
import tech.sadnes.client.network.packet.PlayerState;

public record PartyMemberView(PartyMember member, PlayerState state) {
    public int uid() {
        return this.member.uid();
    }

    public String username() {
        return this.member.username();
    }

    public boolean online() {
        return this.member.online();
    }

    public boolean hasState() {
        return this.state != null;
    }
}
