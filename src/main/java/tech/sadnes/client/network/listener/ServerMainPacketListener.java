package tech.sadnes.client.network.listener;

import tech.sadnes.client.network.packet.ClearPlayerStatePacket;
import tech.sadnes.client.network.packet.DeleteConfigPacket;
import tech.sadnes.client.network.packet.GenerateShareCodePacket;
import tech.sadnes.client.network.packet.ImportSharedConfigPacket;
import tech.sadnes.client.network.packet.KeepAliveResponsePacket;
import tech.sadnes.client.network.packet.ModerationRequestPacket;
import tech.sadnes.client.network.packet.PartyCreatePacket;
import tech.sadnes.client.network.packet.PartyDisbandPacket;
import tech.sadnes.client.network.packet.PartyInvitePacket;
import tech.sadnes.client.network.packet.PartyJoinPacket;
import tech.sadnes.client.network.packet.PartyKickPacket;
import tech.sadnes.client.network.packet.PartyLeavePacket;
import tech.sadnes.client.network.packet.PartySyncRequestPacket;
import tech.sadnes.client.network.packet.PlayerLookUpdatePacket;
import tech.sadnes.client.network.packet.PlayerStateUpdatePacket;
import tech.sadnes.client.network.packet.RequestConfigDataPacket;
import tech.sadnes.client.network.packet.RequestConfigListPacket;
import tech.sadnes.client.network.packet.RequestFavoritesPacket;
import tech.sadnes.client.network.packet.RequestPlayerStatePacket;
import tech.sadnes.client.network.packet.RequestPlayerStatesPacket;
import tech.sadnes.client.network.packet.RequestShareCodesPacket;
import tech.sadnes.client.network.packet.SaveConfigPacket;
import tech.sadnes.client.network.packet.SelectConfigPacket;
import tech.sadnes.client.network.packet.SetFavoritePacket;
import tech.sadnes.client.network.packet.ShareConfigPacket;
import tech.sadnes.client.network.packet.TelemetryReportPacket;
import tech.sadnes.client.network.packet.UserSearchPacket;

public interface ServerMainPacketListener extends ServerPacketListener {
    ServerMainPacketListener.Connection connection();

    ServerMainPacketListener.Moderation moderation();

    ServerMainPacketListener.Players players();

    ServerMainPacketListener.Party party();

    ServerMainPacketListener.Configs configs();

    ServerMainPacketListener.Telemetry telemetry();

    interface Configs {
        void onRequestConfigList(RequestConfigListPacket requestConfigListPacket);

        void onSaveConfig(SaveConfigPacket saveConfigPacket);

        void onRequestConfigData(RequestConfigDataPacket requestConfigDataPacket);

        void onDeleteConfig(DeleteConfigPacket deleteConfigPacket);

        void onSelectConfig(SelectConfigPacket selectConfigPacket);

        void onShareConfig(ShareConfigPacket shareConfigPacket);

        void onImportSharedConfig(ImportSharedConfigPacket importSharedConfigPacket);

        void onRequestShareCodes(RequestShareCodesPacket requestShareCodesPacket);

        void onGenerateShareCode(GenerateShareCodePacket generateShareCodePacket);

        void onRequestFavorites(RequestFavoritesPacket requestFavoritesPacket);

        void onSetFavorite(SetFavoritePacket setFavoritePacket);
    }

    interface Connection {
        void onKeepAliveResponse(KeepAliveResponsePacket keepAliveResponsePacket);
    }

    interface Moderation {
        void onModerationRequest(ModerationRequestPacket moderationRequestPacket);
    }

    interface Party {
        void onPartyCreate(PartyCreatePacket partyCreatePacket);

        void onPartyJoin(PartyJoinPacket partyJoinPacket);

        void onPartyInvite(PartyInvitePacket partyInvitePacket);

        void onPartyLeave(PartyLeavePacket partyLeavePacket);

        void onPartyKick(PartyKickPacket partyKickPacket);

        void onPartyDisband(PartyDisbandPacket partyDisbandPacket);

        void onPartySyncRequest(PartySyncRequestPacket partySyncRequestPacket);

        void onPlayerLook(PlayerLookUpdatePacket playerLookUpdatePacket);
    }

    interface Players {
        void onPlayerStateUpdate(PlayerStateUpdatePacket playerStateUpdatePacket);

        void onPlayerStateClear(ClearPlayerStatePacket clearPlayerStatePacket);

        void onPlayerStateRequest(RequestPlayerStatePacket requestPlayerStatePacket);

        void onPlayerStatesRequest(RequestPlayerStatesPacket requestPlayerStatesPacket);

        void onUserSearch(UserSearchPacket userSearchPacket);
    }

    interface Telemetry {
        void onReport(TelemetryReportPacket telemetryReportPacket);
    }
}
