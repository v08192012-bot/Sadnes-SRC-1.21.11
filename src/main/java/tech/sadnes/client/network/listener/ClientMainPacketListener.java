package tech.sadnes.client.network.listener;

import tech.sadnes.client.network.packet.ConfigDataPacket;
import tech.sadnes.client.network.packet.ConfigListPacket;
import tech.sadnes.client.network.packet.ConfigResultPacket;
import tech.sadnes.client.network.packet.DisconnectPacket;
import tech.sadnes.client.network.packet.FavoritesPacket;
import tech.sadnes.client.network.packet.KeepAlivePacket;
import tech.sadnes.client.network.packet.ModerationResultPacket;
import tech.sadnes.client.network.packet.PartyInviteReceivedPacket;
import tech.sadnes.client.network.packet.PartyLooksPacket;
import tech.sadnes.client.network.packet.PartyResultPacket;
import tech.sadnes.client.network.packet.PartyStatePacket;
import tech.sadnes.client.network.packet.PlayerStateResponsePacket;
import tech.sadnes.client.network.packet.PlayerStatesPacket;
import tech.sadnes.client.network.packet.ShareCodesPacket;
import tech.sadnes.client.network.packet.ShareResultPacket;
import tech.sadnes.client.network.packet.UserSearchResultPacket;

public interface ClientMainPacketListener extends ClientPacketListener {
    ClientMainPacketListener.Connection connection();

    ClientMainPacketListener.Moderation moderation();

    ClientMainPacketListener.Players players();

    ClientMainPacketListener.Party party();

    ClientMainPacketListener.Configs configs();

    void onDisconnect(DisconnectPacket disconnectPacket);

    interface Configs {
        void onConfigList(ConfigListPacket configListPacket);

        void onConfigData(ConfigDataPacket configDataPacket);

        void onConfigResult(ConfigResultPacket configResultPacket);

        void onShareResult(ShareResultPacket shareResultPacket);

        void onShareCodes(ShareCodesPacket shareCodesPacket);

        void onFavorites(FavoritesPacket favoritesPacket);
    }

    interface Connection {
        void onKeepAlive(KeepAlivePacket keepAlivePacket);
    }

    interface Moderation {
        void onModerationResult(ModerationResultPacket moderationResultPacket);
    }

    interface Party {
        void onPartyState(PartyStatePacket partyStatePacket);

        void onPartyResult(PartyResultPacket partyResultPacket);

        void onPartyInvite(PartyInviteReceivedPacket partyInviteReceivedPacket);

        void onPartyLooks(PartyLooksPacket partyLooksPacket);
    }

    interface Players {
        void onPlayerStateResponse(PlayerStateResponsePacket playerStateResponsePacket);

        void onPlayerStates(PlayerStatesPacket playerStatesPacket);

        void onUserSearchResult(UserSearchResultPacket userSearchResultPacket);
    }
}
