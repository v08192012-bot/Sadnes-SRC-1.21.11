package tech.sadnes.client.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import javax.net.ssl.SSLContext;
import local.sadnes.OfflineRouting;
import tech.sadnes.client.network.packet.AuthRequestPacket;
import tech.sadnes.client.network.packet.AuthResultPacket;
import tech.sadnes.client.network.packet.ClearPlayerStatePacket;
import tech.sadnes.client.network.packet.ConfigDataPacket;
import tech.sadnes.client.network.packet.ConfigKind;
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
import tech.sadnes.client.network.packet.ModerationAction;
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
import tech.sadnes.client.network.packet.PlayerLook;
import tech.sadnes.client.network.packet.PlayerLookUpdatePacket;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.network.packet.PlayerStateEntry;
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

public final class SadnesSocketClient implements Listener {
    private final UnaryOperator<String> encoder;
    private final UnaryOperator<String> decoder;
    private final UserProfile profile;
    private final ExecutorService executor;
    private final HttpClient httpClient;
    private HttpClient sslHttpClient;
    private final AtomicBoolean connectionNotified = new AtomicBoolean(false);
    private final StringBuilder textBuffer = new StringBuilder();
    private final Queue<CompletableFuture<String>> pendingCommands = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<PlayerStateEntry>> pendingEntryRequests = new ConcurrentHashMap<>();
    private final AtomicInteger entryRequestCounter = new AtomicInteger();
    private final ConcurrentHashMap<Integer, CompletableFuture<ConfigListPacket>> pendingUploads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<byte[]>> pendingDownloads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<ConfigResultPacket>> pendingActions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<ShareResultPacket>> pendingLookups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<ShareCodesPacket>> pendingInfoRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CompletableFuture<List<String>>> pendingListRequests = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger();
    private volatile Consumer<Boolean> connectionListener;
    private volatile Consumer<List<PlayerStateEntry>> entriesListener;
    private volatile Consumer<PartyStatePacket> listener1381;
    private volatile Consumer<PartyResultPacket> listener1380;
    private volatile Consumer<PartyInviteReceivedPacket> listener1371;
    private volatile Consumer<PartyLooksPacket> listener1377;
    private volatile Consumer<UserSearchResultPacket> listener1367;
    private volatile CompletableFuture<Void> connectFuture = new CompletableFuture<>();
    private volatile ProtocolState state = ProtocolState.HANDSHAKE;
    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile boolean shutDown;
    private volatile long serverTime = -1L;
    private volatile String lastError = "";
    private CompletableFuture<WebSocket> sendChain;

    public SadnesSocketClient(UnaryOperator<String> unaryOperator, UnaryOperator<String> unaryOperator2, UserProfile userProfile) {
        Packets.ensureInitialized();
        this.encoder = unaryOperator;
        this.decoder = unaryOperator2;
        this.profile = userProfile;
        this.executor = Executors.newFixedThreadPool(2, threadFactory());
        this.httpClient = HttpClient.newBuilder().executor(this.executor).build();
    }

    public void setConnectionListener(Consumer<Boolean> consumer) {
        this.connectionListener = consumer;
    }

    public CompletableFuture<Void> connect(String string, SSLContext sslContext) {
        if (this.shutDown) {
            return CompletableFuture.failedFuture(new IllegalStateException("client shut down"));
        }

        CompletableFuture completableFuture = this.resetForConnect();
        OfflineRouting.connect(this.getHttpClient(sslContext).newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10L)), URI.create(string), this)
            .whenComplete((webSocket2, throwable) -> {
                if (throwable != null) {
                    this.fail(throwable);
                } else {
                    synchronized (this) {
                        this.webSocket = webSocket2;
                        this.sendChain = CompletableFuture.completedFuture(webSocket2);
                    }

                    this.sendPacket(new HandshakePacket(12, 12, 1, "sadnes-client"));
                }
            });
        return completableFuture;
    }

    public boolean isConnected() {
        return !this.shutDown && this.connected;
    }

    public long getServerTime() {
        return this.serverTime;
    }

    public String getLastError() {
        return this.lastError;
    }

    public void abortStalled() {
        WebSocket webSocket2 = this.webSocket;
        if (webSocket2 != null) {
            webSocket2.abort();
        }

        this.fail(new IllegalStateException("connect stalled"));
    }

    public void shutdown() {
        this.shutDown = true;
        WebSocket webSocket2 = this.webSocket;
        if (webSocket2 != null) {
            webSocket2.sendClose(1000, "bye");
        }

        this.failPending(new IllegalStateException("client shut down"));
        this.markDisconnected();
        this.executor.shutdown();
    }

    public CompletableFuture<String> sendCommand(ModerationAction moderationAction, int i, String string, long l) {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingCommands.add(completableFuture);
        this.sendPacket(new ModerationRequestPacket(moderationAction, i, string, l));
        return completableFuture;
    }

    public void setEntriesListener(Consumer<List<PlayerStateEntry>> consumer) {
        this.entriesListener = consumer;
    }

    public void publishData(PlayerState playerState) {
        if (this.isConnected()) {
            this.sendPacket(new PlayerStateUpdatePacket(playerState));
        }
    }

    public void sendNamedList(String string, List<String> strings) {
        if (this.isConnected()) {
            this.sendPacket(new TelemetryReportPacket(string, strings));
        }
    }

    public void sendRefresh() {
        if (this.isConnected()) {
            this.sendPacket(new ClearPlayerStatePacket());
        }
    }

    public CompletableFuture<PlayerState> fetchData(String string) {
        return this.requestEntry(string, false).thenApply(PlayerStateEntry::data);
    }

    public CompletableFuture<PlayerState> fetchDataFresh(String string) {
        return this.requestEntry(string, true).thenApply(PlayerStateEntry::data);
    }

    public CompletableFuture<PlayerStateEntry> fetchEntry(String string) {
        return this.requestEntry(string, true);
    }

    private CompletableFuture<PlayerStateEntry> requestEntry(String string, boolean flag) {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        int i = this.entryRequestCounter.incrementAndGet();
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingEntryRequests.put(i, completableFuture);
        this.sendPacket(new RequestPlayerStatePacket(i, string, flag));
        return completableFuture;
    }

    public void sendNames(List<String> strings) {
        if (this.isConnected()) {
            this.sendPacket(new RequestPlayerStatesPacket(strings));
        }
    }

    public void setListener1381(Consumer<PartyStatePacket> consumer) {
        this.listener1381 = consumer;
    }

    public void setListener1380(Consumer<PartyResultPacket> consumer) {
        this.listener1380 = consumer;
    }

    public void setListener1371(Consumer<PartyInviteReceivedPacket> consumer) {
        this.listener1371 = consumer;
    }

    public void setListener1367(Consumer<UserSearchResultPacket> consumer) {
        this.listener1367 = consumer;
    }

    public void sendText(String string) {
        if (this.isConnected()) {
            this.sendPacket(new UserSearchPacket(string == null ? "" : string, List.of()));
        }
    }

    public void sendTargets(List<String> strings) {
        if (this.isConnected() && !strings.isEmpty()) {
            this.sendPacket(new UserSearchPacket("", strings));
        }
    }

    public void sendPacket1368() {
        if (this.isConnected()) {
            this.sendPacket(new PartyCreatePacket());
        }
    }

    public void sendPacket1370(String string) {
        if (this.isConnected()) {
            this.sendPacket(new PartyInvitePacket(string));
        }
    }

    public void sendPacket1372(String string) {
        if (this.isConnected()) {
            this.sendPacket(new PartyJoinPacket(string));
        }
    }

    public void sendPacket1374() {
        if (this.isConnected()) {
            this.sendPacket(new PartyLeavePacket());
        }
    }

    public void sendPacket1373(int i) {
        if (this.isConnected()) {
            this.sendPacket(new PartyKickPacket(i));
        }
    }

    public void sendPacket1369() {
        if (this.isConnected()) {
            this.sendPacket(new PartyDisbandPacket());
        }
    }

    public void sendPacket1382() {
        if (this.isConnected()) {
            this.sendPacket(new PartySyncRequestPacket());
        }
    }

    public void setListener1377(Consumer<PartyLooksPacket> consumer) {
        this.listener1377 = consumer;
    }

    public void sendPacket1376(PlayerLook playerLook) {
        if (this.isConnected()) {
            this.sendPacket(new PlayerLookUpdatePacket(playerLook));
        }
    }

    public CompletableFuture<ConfigListPacket> upload(ConfigKind configKind) {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        int i = this.requestCounter.incrementAndGet();
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingUploads.put(i, completableFuture);
        this.sendPacket(new RequestConfigListPacket(i, configKind));
        return completableFuture;
    }

    public CompletableFuture<byte[]> download(String string) {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        int i = this.requestCounter.incrementAndGet();
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingDownloads.put(i, completableFuture);
        this.sendPacket(new RequestConfigDataPacket(i, string));
        return completableFuture;
    }

    public CompletableFuture<String> uploadFile(String string, ConfigKind configKind, String string2, byte[] bytes, byte[] bytes2) {
        return this.sendActionRequest(i -> new SaveConfigPacket(i, string, configKind, string2, bytes, bytes2)).thenApply(ConfigResultPacket::getConfigId);
    }

    public CompletableFuture<Void> requestAction1342(String string) {
        return this.sendActionRequest(i -> new DeleteConfigPacket(i, string)).thenAccept(configResultPacket -> {});
    }

    public CompletableFuture<Void> requestAction1354(String string) {
        return this.sendActionRequest(i -> new SelectConfigPacket(i, string)).thenAccept(configResultPacket -> {});
    }

    public CompletableFuture<ShareResultPacket> lookup(String string, boolean flag) {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        int i = this.requestCounter.incrementAndGet();
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingLookups.put(i, completableFuture);
        this.sendPacket(new ShareConfigPacket(i, string, flag));
        return completableFuture;
    }

    public CompletableFuture<List<String>> requestList() {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        int i = this.requestCounter.incrementAndGet();
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingListRequests.put(i, completableFuture);
        this.sendPacket(new RequestFavoritesPacket(i));
        return completableFuture;
    }

    public CompletableFuture<Void> requestAction1345(String string, boolean flag) {
        return this.sendActionRequest(i -> new SetFavoritePacket(i, string, flag)).thenAccept(configResultPacket -> {});
    }

    public CompletableFuture<ShareCodesPacket> requestInfo(String string) {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        int i = this.requestCounter.incrementAndGet();
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingInfoRequests.put(i, completableFuture);
        this.sendPacket(new RequestShareCodesPacket(i, string));
        return completableFuture;
    }

    public CompletableFuture<Void> requestAction1349(String string) {
        return this.sendActionRequest(i -> new GenerateShareCodePacket(i, string)).thenAccept(configResultPacket -> {});
    }

    public CompletableFuture<String> requestAction1340(String string) {
        return this.sendActionRequest(i -> new ImportSharedConfigPacket(i, string)).thenApply(ConfigResultPacket::getConfigId);
    }

    private CompletableFuture<ConfigResultPacket> sendActionRequest(IntFunction<Packet<?>> intFunction) {
        if (!this.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        }

        int i = this.requestCounter.incrementAndGet();
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingActions.put(i, completableFuture);
        this.sendPacket((Packet<?>)intFunction.apply(i));
        return completableFuture;
    }

    private synchronized void sendPacket(Packet<?> packet) {
        if (this.sendChain != null && !this.shutDown) {
            this.sendChain = this.sendChain.thenComposeAsync(webSocket2 -> webSocket2.sendText(this.encoder.apply(PacketCodec.encode(packet)), true), this.executor);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket2) {
        webSocket2.request(1L);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket2, CharSequence charSequence, boolean flag) {
        this.textBuffer.append(charSequence);
        if (flag) {
            String string = this.textBuffer.toString();
            this.textBuffer.setLength(0);

            try {
                this.handlePacket(PacketCodec.decode(this.state, PacketDirection.CLIENTBOUND, this.decoder.apply(string)));
            } catch (RuntimeException exception) {
                this.fail(exception);
            }
        }

        webSocket2.request(1L);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket2, int i, String string) {
        this.fail(new IllegalStateException("closed: " + i + " " + string));
        return null;
    }

    @Override
    public void onError(WebSocket webSocket2, Throwable throwable) {
        this.fail(throwable);
    }

    private void handlePacket(Packet<?> packet) {
        switch (this.state) {
            case HANDSHAKE:
                if (packet instanceof HandshakeResultPacket handshakeResultPacket) {
                    if (!handshakeResultPacket.isAccepted()) {
                        this.fail(new IllegalStateException("handshake rejected: " + handshakeResultPacket.getMessage()));
                        return;
                    }

                    this.state = ProtocolState.AUTH;
                    this.sendPacket(
                        new AuthRequestPacket(
                            this.profile.uid(),
                            this.profile.username(),
                            this.profile.role(),
                            this.profile.hwid(),
                            this.profile.subscriptionEndDate(),
                            this.profile.avatarUrl(),
                            "sadnes-client"
                        )
                    );
                    return;
                }

                if (packet instanceof DisconnectPacket disconnectPacket) {
                    this.fail(new IllegalStateException("disconnected: " + disconnectPacket.getReason()));
                }

                return;
            case AUTH:
                if (packet instanceof AuthResultPacket authResultPacket) {
                    if (!authResultPacket.isSuccess()) {
                        this.fail(new IllegalStateException("auth rejected: " + authResultPacket.getMessage()));
                        return;
                    }

                    this.state = ProtocolState.MAIN;
                    this.connected = true;
                    this.connectFuture.complete(null);
                    this.onAuthenticated();
                    return;
                }

                if (packet instanceof DisconnectPacket disconnectPacket2) {
                    this.fail(new IllegalStateException("disconnected: " + disconnectPacket2.getReason()));
                }

                return;
            case MAIN:
                this.handleMainPacket(packet);
        }
    }

    private void handleMainPacket(Packet<?> packet) {
        if (packet instanceof KeepAlivePacket keepAlivePacket) {
            this.serverTime = keepAlivePacket.getServerTime();
            this.sendPacket(new KeepAliveResponsePacket(keepAlivePacket.getId()));
        } else if (packet instanceof ModerationResultPacket moderationResultPacket) {
            CompletableFuture completableFuture = this.pendingCommands.poll();
            if (completableFuture != null) {
                if (moderationResultPacket.isSuccess()) {
                    completableFuture.complete(moderationResultPacket.getMessage());
                } else {
                    completableFuture.completeExceptionally(new IllegalStateException(moderationResultPacket.getMessage()));
                }
            }
        } else {
            CompletableFuture completableFuture2;
            label101: {
                if (packet instanceof PlayerStateResponsePacket playerStateResponsePacket) {
                    CompletableFuture completableFuture3 = this.pendingEntryRequests.remove(playerStateResponsePacket.getRequestId());
                    if (completableFuture3 != null) {
                        completableFuture3.complete(new PlayerStateEntry(playerStateResponsePacket.getLogin(), playerStateResponsePacket.getState()));
                        return;
                    }
                } else if (packet instanceof PlayerStatesPacket playerStatesPacket) {
                    Consumer consumer = this.entriesListener;
                    if (consumer != null) {
                        consumer.accept(playerStatesPacket.getEntries());
                        return;
                    }
                } else if (packet instanceof PartyStatePacket partyStatePacket) {
                    Consumer consumer2 = this.listener1381;
                    if (consumer2 != null) {
                        consumer2.accept(partyStatePacket);
                        return;
                    }
                } else if (packet instanceof PartyResultPacket partyResultPacket) {
                    Consumer consumer3 = this.listener1380;
                    if (consumer3 != null) {
                        consumer3.accept(partyResultPacket);
                        return;
                    }
                } else if (packet instanceof PartyInviteReceivedPacket partyInviteReceivedPacket) {
                    Consumer consumer4 = this.listener1371;
                    if (consumer4 != null) {
                        consumer4.accept(partyInviteReceivedPacket);
                        return;
                    }
                } else if (packet instanceof PartyLooksPacket partyLooksPacket) {
                    Consumer consumer5 = this.listener1377;
                    if (consumer5 != null) {
                        consumer5.accept(partyLooksPacket);
                        return;
                    }
                } else if (packet instanceof UserSearchResultPacket userSearchResultPacket) {
                    Consumer consumer6 = this.listener1367;
                    if (consumer6 != null) {
                        consumer6.accept(userSearchResultPacket);
                        return;
                    }
                } else if (packet instanceof ConfigListPacket configListPacket) {
                    CompletableFuture completableFuture4 = this.pendingUploads.remove(configListPacket.getRequestId());
                    if (completableFuture4 != null) {
                        completableFuture4.complete(configListPacket);
                        return;
                    }
                } else if (packet instanceof ConfigDataPacket configDataPacket) {
                    completableFuture2 = this.pendingDownloads.remove(configDataPacket.getRequestId());
                    if (completableFuture2 != null) {
                        if (configDataPacket.isFound()) {
                            completableFuture2.complete(configDataPacket.getData());
                            return;
                        }
                        break label101;
                    }
                } else if (packet instanceof ConfigResultPacket configResultPacket) {
                    CompletableFuture completableFuture5 = this.pendingActions.remove(configResultPacket.getRequestId());
                    if (completableFuture5 != null) {
                        if (configResultPacket.isSuccess()) {
                            completableFuture5.complete(configResultPacket);
                            return;
                        }

                        completableFuture5.completeExceptionally(new IllegalStateException(configResultPacket.getMessage()));
                        return;
                    }
                } else if (packet instanceof ShareResultPacket shareResultPacket) {
                    CompletableFuture completableFuture6 = this.pendingLookups.remove(shareResultPacket.getRequestId());
                    if (completableFuture6 != null) {
                        if (shareResultPacket.isSuccess()) {
                            completableFuture6.complete(shareResultPacket);
                            return;
                        }

                        completableFuture6.completeExceptionally(new IllegalStateException(shareResultPacket.getMessage()));
                        return;
                    }
                } else if (packet instanceof FavoritesPacket favoritesPacket) {
                    CompletableFuture completableFuture7 = this.pendingListRequests.remove(favoritesPacket.getRequestId());
                    if (completableFuture7 != null) {
                        completableFuture7.complete(favoritesPacket.getFavorites());
                        return;
                    }
                } else if (packet instanceof ShareCodesPacket shareCodesPacket) {
                    completableFuture2 = this.pendingInfoRequests.remove(shareCodesPacket.getRequestId());
                    if (completableFuture2 != null) {
                        if (shareCodesPacket.isFound()) {
                            completableFuture2.complete(shareCodesPacket);
                            return;
                        }
                        break label101;
                    }
                } else if (packet instanceof DisconnectPacket disconnectPacket) {
                    this.fail(new IllegalStateException("disconnected: " + disconnectPacket.getReason()));
                }

                return;
            }

            completableFuture2.completeExceptionally(new IllegalStateException("not_found"));
        }
    }

    private synchronized CompletableFuture<Void> resetForConnect() {
        this.connectFuture = new CompletableFuture<>();
        this.state = ProtocolState.HANDSHAKE;
        this.textBuffer.setLength(0);
        this.webSocket = null;
        this.sendChain = null;
        this.connected = false;
        this.serverTime = -1L;
        this.lastError = "";
        this.connectionNotified.set(true);
        return this.connectFuture;
    }

    private void onAuthenticated() {
        this.connected = true;
        this.notifyConnection(Boolean.TRUE);
    }

    private void notifyConnection(Boolean flag) {
        Consumer consumer = this.connectionListener;
        if (consumer != null) {
            try {
                consumer.accept(flag);
            } catch (RuntimeException exception) {
                System.err.println("[sadnes-net] connection listener failed: " + exception);
            }
        }
    }

    private void fail(Throwable throwable) {
        this.connected = false;
        this.lastError = throwable == null ? "" : String.valueOf(throwable.getMessage());
        this.markDisconnected();
        this.failPending(throwable);
        if (!this.connectFuture.isDone()) {
            this.connectFuture.completeExceptionally(throwable);
        }
    }

    private void failPending(Throwable throwable) {
        CompletableFuture completableFuture;
        while ((completableFuture = this.pendingCommands.poll()) != null) {
            completableFuture.completeExceptionally(throwable);
        }

        for (Integer integer : this.pendingEntryRequests.keySet()) {
            CompletableFuture completableFuture2 = this.pendingEntryRequests.remove(integer);
            if (completableFuture2 != null) {
                completableFuture2.completeExceptionally(throwable);
            }
        }

        failAll(this.pendingUploads, throwable);
        failAll(this.pendingDownloads, throwable);
        failAll(this.pendingActions, throwable);
        failAll(this.pendingLookups, throwable);
        failAll(this.pendingInfoRequests, throwable);
        failAll(this.pendingListRequests, throwable);
    }

    private static <T> void failAll(ConcurrentHashMap<Integer, CompletableFuture<T>> concurrentHashMap, Throwable throwable) {
        for (Integer integer : concurrentHashMap.keySet()) {
            CompletableFuture completableFuture = (CompletableFuture)concurrentHashMap.remove(integer);
            if (completableFuture != null) {
                completableFuture.completeExceptionally(throwable);
            }
        }
    }

    private void markDisconnected() {
        this.connected = false;
        if (this.connectionNotified.compareAndSet(true, false)) {
            this.notifyConnection(Boolean.FALSE);
        }
    }

    private HttpClient getHttpClient(SSLContext sslContext2) {
        if (sslContext2 == null) {
            return this.httpClient;
        }

        if (this.sslHttpClient == null) {
            this.sslHttpClient = HttpClient.newBuilder().executor(this.executor).sslContext(sslContext2).build();
        }

        return this.sslHttpClient;
    }

    private static ThreadFactory threadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "sadnes-ws");
            thread.setDaemon(true);
            return thread;
        };
    }
}
