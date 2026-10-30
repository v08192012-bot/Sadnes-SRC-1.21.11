package tech.sadnes.client.friend;

import java.util.HashSet;
import java.util.Set;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyMemberView;
import ua.mintantileak.profile.Profile;

public final class PartyFriendSync {
    private final BackendService partyService;
    private final FriendManager friendManager;

    public PartyFriendSync(BackendService backendService, FriendManager friendManager2) {
        this.partyService = backendService;
        this.friendManager = friendManager2;
    }

    public void register() {
        Sadnes.getEventBus().subscribe(this);
    }

    @EventHandler
    public void onUpdate(PlayerTickEvent playerTickEvent) {
        PartyManager partyManager = this.partyService != null ? this.partyService.getPartyManager() : null;
        if (partyManager != null && partyManager.inParty()) {
            int i = Profile.getUid();
            HashSet hashSet = new HashSet();

            for (PartyMemberView partyMemberView : partyManager.getMemberViews()) {
                if (partyMemberView.uid() != i) {
                    PlayerState playerState = partyMemberView.state();
                    if (playerState != null && !playerState.getUsername().isBlank()) {
                        hashSet.add(playerState.getUsername());
                    }
                }
            }

            this.friendManager.setSharedFriends(hashSet);
        } else {
            this.friendManager.setSharedFriends(Set.of());
        }
    }
}
