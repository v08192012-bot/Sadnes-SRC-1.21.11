package tech.sadnes.client.network;

import java.util.function.Function;
import tech.sadnes.client.network.packet.AuthRequestPacket;
import tech.sadnes.client.network.packet.AuthResultPacket;
import tech.sadnes.client.network.packet.ClearPlayerStatePacket;
import tech.sadnes.client.network.packet.ConfigDataPacket;
import tech.sadnes.client.network.packet.ConfigListPacket;
import tech.sadnes.client.network.packet.ConfigResultPacket;
import tech.sadnes.client.network.packet.DeleteConfigPacket;
import tech.sadnes.client.network.packet.DisconnectPacket;
import tech.sadnes.client.network.packet.FavoritesPacket;
import tech.sadnes.client.network.packet.GenerateShareCodePacket;
import tech.sadnes.client.network.packet.HandshakePacket;
import tech.sadnes.client.network.packet.HandshakeResultPacket;
import tech.sadnes.client.network.packet.ImportSharedConfigPacket;
import tech.sadnes.client.network.packet.KeepAlivePacket;
import tech.sadnes.client.network.packet.KeepAliveResponsePacket;
import tech.sadnes.client.network.packet.ModerationRequestPacket;
import tech.sadnes.client.network.packet.ModerationResultPacket;
import tech.sadnes.client.network.packet.PartyCreatePacket;
import tech.sadnes.client.network.packet.PartyDisbandPacket;
import tech.sadnes.client.network.packet.PartyInvitePacket;
import tech.sadnes.client.network.packet.PartyInviteReceivedPacket;
import tech.sadnes.client.network.packet.PartyJoinPacket;
import tech.sadnes.client.network.packet.PartyKickPacket;
import tech.sadnes.client.network.packet.PartyLeavePacket;
import tech.sadnes.client.network.packet.PartyLooksPacket;
import tech.sadnes.client.network.packet.PartyResultPacket;
import tech.sadnes.client.network.packet.PartyStatePacket;
import tech.sadnes.client.network.packet.PartySyncRequestPacket;
import tech.sadnes.client.network.packet.PlayerLookUpdatePacket;
import tech.sadnes.client.network.packet.PlayerStateResponsePacket;
import tech.sadnes.client.network.packet.PlayerStateUpdatePacket;
import tech.sadnes.client.network.packet.PlayerStatesPacket;
import tech.sadnes.client.network.packet.RequestConfigDataPacket;
import tech.sadnes.client.network.packet.RequestConfigListPacket;
import tech.sadnes.client.network.packet.RequestFavoritesPacket;
import tech.sadnes.client.network.packet.RequestPlayerStatePacket;
import tech.sadnes.client.network.packet.RequestPlayerStatesPacket;
import tech.sadnes.client.network.packet.RequestShareCodesPacket;
import tech.sadnes.client.network.packet.SaveConfigPacket;
import tech.sadnes.client.network.packet.SelectConfigPacket;
import tech.sadnes.client.network.packet.SetFavoritePacket;
import tech.sadnes.client.network.packet.ShareCodesPacket;
import tech.sadnes.client.network.packet.ShareConfigPacket;
import tech.sadnes.client.network.packet.ShareResultPacket;
import tech.sadnes.client.network.packet.TelemetryReportPacket;
import tech.sadnes.client.network.packet.UserSearchPacket;
import tech.sadnes.client.network.packet.UserSearchResultPacket;

public final class Packets {
    private static volatile boolean initialized = true;
    public static final PacketType<HandshakePacket> HANDSHAKE = register(
        ProtocolState.HANDSHAKE, PacketDirection.SERVERBOUND, 0, HandshakePacket.class, HandshakePacket::new
    );
    public static final PacketType<HandshakeResultPacket> HANDSHAKE_RESULT = register(
        ProtocolState.HANDSHAKE, PacketDirection.CLIENTBOUND, 0, HandshakeResultPacket.class, HandshakeResultPacket::new
    );
    public static final PacketType<DisconnectPacket> HANDSHAKE_DISCONNECT = registerDecodeOnly(
        ProtocolState.HANDSHAKE, PacketDirection.CLIENTBOUND, 1, DisconnectPacket.class, DisconnectPacket::new
    );
    public static final PacketType<AuthRequestPacket> AUTH_REQUEST = register(
        ProtocolState.AUTH, PacketDirection.SERVERBOUND, 0, AuthRequestPacket.class, AuthRequestPacket::new
    );
    public static final PacketType<AuthResultPacket> AUTH_RESULT = register(
        ProtocolState.AUTH, PacketDirection.CLIENTBOUND, 0, AuthResultPacket.class, AuthResultPacket::new
    );
    public static final PacketType<DisconnectPacket> AUTH_DISCONNECT = registerDecodeOnly(
        ProtocolState.AUTH, PacketDirection.CLIENTBOUND, 1, DisconnectPacket.class, DisconnectPacket::new
    );
    public static final PacketType<KeepAliveResponsePacket> KEEP_ALIVE_RESPONSE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 0, KeepAliveResponsePacket.class, KeepAliveResponsePacket::new
    );
    public static final PacketType<ModerationRequestPacket> MODERATION_REQUEST = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 1, ModerationRequestPacket.class, ModerationRequestPacket::new
    );
    public static final PacketType<PlayerStateUpdatePacket> PLAYER_STATE_UPDATE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 2, PlayerStateUpdatePacket.class, PlayerStateUpdatePacket::new
    );
    public static final PacketType<ClearPlayerStatePacket> CLEAR_PLAYER_STATE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 3, ClearPlayerStatePacket.class, ClearPlayerStatePacket::new
    );
    public static final PacketType<RequestPlayerStatePacket> REQUEST_PLAYER_STATE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 4, RequestPlayerStatePacket.class, RequestPlayerStatePacket::new
    );
    public static final PacketType<RequestPlayerStatesPacket> REQUEST_PLAYER_STATES = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 5, RequestPlayerStatesPacket.class, RequestPlayerStatesPacket::new
    );
    public static final PacketType<PartyCreatePacket> PARTY_CREATE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 6, PartyCreatePacket.class, PartyCreatePacket::new
    );
    public static final PacketType<PartyJoinPacket> PARTY_JOIN = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 7, PartyJoinPacket.class, PartyJoinPacket::new
    );
    public static final PacketType<PartyLeavePacket> PARTY_LEAVE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 8, PartyLeavePacket.class, PartyLeavePacket::new
    );
    public static final PacketType<PartyKickPacket> PARTY_KICK = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 9, PartyKickPacket.class, PartyKickPacket::new
    );
    public static final PacketType<PartyDisbandPacket> PARTY_DISBAND = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 10, PartyDisbandPacket.class, PartyDisbandPacket::new
    );
    public static final PacketType<PartySyncRequestPacket> PARTY_SYNC_REQUEST = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 11, PartySyncRequestPacket.class, PartySyncRequestPacket::new
    );
    public static final PacketType<RequestConfigListPacket> REQUEST_CONFIG_LIST = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 12, RequestConfigListPacket.class, RequestConfigListPacket::new
    );
    public static final PacketType<SaveConfigPacket> SAVE_CONFIG = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 13, SaveConfigPacket.class, SaveConfigPacket::new
    );
    public static final PacketType<RequestConfigDataPacket> REQUEST_CONFIG_DATA = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 14, RequestConfigDataPacket.class, RequestConfigDataPacket::new
    );
    public static final PacketType<DeleteConfigPacket> DELETE_CONFIG = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 15, DeleteConfigPacket.class, DeleteConfigPacket::new
    );
    public static final PacketType<SelectConfigPacket> SELECT_CONFIG = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 16, SelectConfigPacket.class, SelectConfigPacket::new
    );
    public static final PacketType<ShareConfigPacket> SHARE_CONFIG = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 17, ShareConfigPacket.class, ShareConfigPacket::new
    );
    public static final PacketType<ImportSharedConfigPacket> IMPORT_SHARED_CONFIG = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 18, ImportSharedConfigPacket.class, ImportSharedConfigPacket::new
    );
    public static final PacketType<RequestShareCodesPacket> REQUEST_SHARE_CODES = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 19, RequestShareCodesPacket.class, RequestShareCodesPacket::new
    );
    public static final PacketType<GenerateShareCodePacket> GENERATE_SHARE_CODE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 20, GenerateShareCodePacket.class, GenerateShareCodePacket::new
    );
    public static final PacketType<RequestFavoritesPacket> REQUEST_FAVORITES = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 21, RequestFavoritesPacket.class, RequestFavoritesPacket::new
    );
    public static final PacketType<SetFavoritePacket> SET_FAVORITE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 22, SetFavoritePacket.class, SetFavoritePacket::new
    );
    public static final PacketType<PartyInvitePacket> PARTY_INVITE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 23, PartyInvitePacket.class, PartyInvitePacket::new
    );
    public static final PacketType<UserSearchPacket> USER_SEARCH = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 24, UserSearchPacket.class, UserSearchPacket::new
    );
    public static final PacketType<TelemetryReportPacket> TELEMETRY_REPORT = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 25, TelemetryReportPacket.class, TelemetryReportPacket::new
    );
    public static final PacketType<PlayerLookUpdatePacket> PLAYER_LOOK_UPDATE = register(
        ProtocolState.MAIN, PacketDirection.SERVERBOUND, 26, PlayerLookUpdatePacket.class, PlayerLookUpdatePacket::new
    );
    public static final PacketType<KeepAlivePacket> KEEP_ALIVE = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 0, KeepAlivePacket.class, KeepAlivePacket::new
    );
    public static final PacketType<DisconnectPacket> MAIN_DISCONNECT = registerDecodeOnly(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 1, DisconnectPacket.class, DisconnectPacket::new
    );
    public static final PacketType<ModerationResultPacket> MODERATION_RESULT = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 2, ModerationResultPacket.class, ModerationResultPacket::new
    );
    public static final PacketType<PlayerStateResponsePacket> PLAYER_STATE_RESPONSE = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 3, PlayerStateResponsePacket.class, PlayerStateResponsePacket::new
    );
    public static final PacketType<PlayerStatesPacket> PLAYER_STATES = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 4, PlayerStatesPacket.class, PlayerStatesPacket::new
    );
    public static final PacketType<PartyStatePacket> PARTY_STATE = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 5, PartyStatePacket.class, PartyStatePacket::new
    );
    public static final PacketType<PartyResultPacket> PARTY_RESULT = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 6, PartyResultPacket.class, PartyResultPacket::new
    );
    public static final PacketType<ConfigListPacket> CONFIG_LIST = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 7, ConfigListPacket.class, ConfigListPacket::new
    );
    public static final PacketType<ConfigDataPacket> CONFIG_DATA = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 8, ConfigDataPacket.class, ConfigDataPacket::new
    );
    public static final PacketType<ConfigResultPacket> CONFIG_RESULT = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 9, ConfigResultPacket.class, ConfigResultPacket::new
    );
    public static final PacketType<ShareResultPacket> SHARE_RESULT = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 10, ShareResultPacket.class, ShareResultPacket::new
    );
    public static final PacketType<ShareCodesPacket> SHARE_CODES = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 11, ShareCodesPacket.class, ShareCodesPacket::new
    );
    public static final PacketType<FavoritesPacket> FAVORITES = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 12, FavoritesPacket.class, FavoritesPacket::new
    );
    public static final PacketType<PartyInviteReceivedPacket> PARTY_INVITE_RECEIVED = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 13, PartyInviteReceivedPacket.class, PartyInviteReceivedPacket::new
    );
    public static final PacketType<UserSearchResultPacket> USER_SEARCH_RESULT = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 14, UserSearchResultPacket.class, UserSearchResultPacket::new
    );
    public static final PacketType<PartyLooksPacket> PARTY_LOOKS = register(
        ProtocolState.MAIN, PacketDirection.CLIENTBOUND, 15, PartyLooksPacket.class, PartyLooksPacket::new
    );

    private Packets() {
    }

    public static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Packets failed to initialize");
        }
    }

    private static <P extends Packet<?>> PacketType<P> register(
        ProtocolState protocolState, PacketDirection packetDirection, int i, Class<P> clazz, Function<PacketBuffer, P> function
    ) {
        return PacketRegistry.register(new PacketType<>(protocolState, packetDirection, i, clazz, function));
    }

    private static <P extends Packet<?>> PacketType<P> registerDecodeOnly(
        ProtocolState protocolState, PacketDirection packetDirection, int i, Class<P> clazz, Function<PacketBuffer, P> function
    ) {
        return PacketRegistry.registerDecodeOnly(new PacketType<>(protocolState, packetDirection, i, clazz, function));
    }

    static {
        PacketRegistry.freeze();
    }
}
