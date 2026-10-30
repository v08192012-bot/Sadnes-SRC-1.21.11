package tech.sadnes.client.network;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.MinecraftClient;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.event.impl.BackendConnectionEvent;
import tech.sadnes.client.network.packet.ModerationAction;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.network.packet.UserRole;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyPresenceReporter;
import tech.sadnes.client.party.PartyStateCache;
import tech.sadnes.client.party.PartyStateReporter;
import tech.sadnes.client.party.PlayerLoginResolver;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.autobuy.CollectorManager;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

public final class BackendService {
    private static final String SECRET_KEY = decodeSecretKey();
    private static final long BASE_RECONNECT_DELAY_MS = 15000L;
    private static final long RECONNECT_JITTER_MS = 10000L;
    private static final long MAX_RECONNECT_DELAY_MS = 240000L;
    private static final long CONNECT_STALL_TIMEOUT_MS = 20000L;
    private final String serverUrl;
    private final SadnesSocketClient socketClient;
    private final PartyStateReporter stateReporter;
    private final PartyPresenceReporter presenceReporter;
    private final PartyStateCache stateCache;
    private final PlayerLoginResolver loginResolver;
    private final PartyManager partyManager;
    private final ScheduledExecutorService supervisor;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private volatile boolean shutdown;
    private volatile long connectStartedAt;
    private volatile int failedAttempts;
    private volatile boolean fatalError;
    private volatile long connectedAt;
    private volatile int connectCount;
    private volatile int disconnectCount;
    private volatile String lastError = "";

    private static String decodeSecretKey() {
        String string = "ih72PmYNnZ3mmi5x/z1i9f4mVTc+99Yh7QMSzylyxUU=";
        return new String(string);
    }

    public BackendService() {
        this.serverUrl = BackendUrls.getWebSocketUrl();
        this.socketClient = new SadnesSocketClient(
            new SocketCipherProvider(SECRET_KEY).encryptor(), new SocketCipherProvider(SECRET_KEY).decryptor(), createUserProfile()
        );
        this.socketClient.setConnectionListener(this::onConnectionChanged);
        this.stateReporter = new PartyStateReporter(this.socketClient);
        this.presenceReporter = new PartyPresenceReporter(this.socketClient);
        this.stateCache = new PartyStateCache(this.socketClient);
        this.socketClient.setEntriesListener(this.stateCache::applyUpdates);
        this.loginResolver = new PlayerLoginResolver(this.socketClient);
        this.partyManager = new PartyManager(this.socketClient, this.stateCache);
        this.supervisor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sadnes-net-supervisor");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        this.connect();
        this.stateReporter.start();
        this.presenceReporter.start();
        this.scheduleSupervisor();
    }

    public boolean isConnected() {
        return this.socketClient.isConnected();
    }

    public long getServerTime() {
        return this.socketClient.getServerTime();
    }

    public boolean isConnecting() {
        return this.connecting.get();
    }

    public long getConnectedAt() {
        return this.connectedAt;
    }

    public int getConnectCount() {
        return this.connectCount;
    }

    public int getDisconnectCount() {
        return this.disconnectCount;
    }

    public String getLastError() {
        return this.lastError;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    public int getTrackedStateCount() {
        return this.stateCache.size();
    }

    public CompletableFuture<String> sendCommand(ModerationAction moderationAction, int i, String string, long l) {
        return this.socketClient.sendCommand(moderationAction, i, string, l);
    }

    public PlayerState getStateByLogin(String string) {
        return this.stateCache.getByLogin(string);
    }

    public PlayerState getStateByName(String string) {
        return this.stateCache.getByName(string);
    }

    public CompletableFuture<PlayerState> requestStateByLogin(String string) {
        return this.stateCache.requestByLogin(string);
    }

    public CompletableFuture<PlayerState> requestStateByName(String string) {
        return this.stateCache.requestByName(string);
    }

    public String getAvatarUrlForPlayer(String string) {
        String string2 = this.loginResolver.resolveLogin(string);
        return string2 == null ? null : this.partyManager.getAvatarUrl(string2);
    }

    public void setSubscribedStates(Collection<String> strings) {
        this.stateCache.setSubscribed(strings);
    }

    public PartyManager getPartyManager() {
        return this.partyManager;
    }

    public SadnesSocketClient getSocketClient() {
        return this.socketClient;
    }

    public void setPing(int i, double d, double d2, double d3, long l) {
        this.stateReporter.setPing(i, d, d2, d3, l);
    }

    public void setEntityPing(int i, UUID uuid, double d, double d2, double d3, long l) {
        this.stateReporter.setEntityPing(i, uuid, d, d2, d3, l);
    }

    public void clearPing() {
        this.stateReporter.clearPing();
    }

    public PartyStateReporter.ActivePing getActivePing() {
        return this.stateReporter.getActivePing();
    }

    public void stop() {
        this.shutdown = true;
        this.stateReporter.stop();
        this.presenceReporter.stop();
        this.supervisor.shutdownNow();
        this.socketClient.shutdown();
    }

    private void scheduleSupervisor() {
        if (!this.shutdown) {
            long l = this.fatalError ? 240000L : Math.min(15000L << Math.min(this.failedAttempts, 4), 240000L);
            long l2 = l + ThreadLocalRandom.current().nextLong(10000L);

            try {
                this.supervisor.schedule(this::superviseConnection, l2, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException rejectedExecutionException) {
            }
        }
    }

    private void superviseConnection() {
        try {
            if (this.shutdown) {
                return;
            }

            if (!this.connecting.get()) {
                if (!this.socketClient.isConnected()) {
                    this.connect();
                }

                return;
            }

            long l = System.currentTimeMillis() - this.connectStartedAt;
            if (l > 20000L) {
                Sadnes.getLogger().warn("[network] connect stalled for {} ms — aborting attempt", l);
                this.socketClient.abortStalled();
            }
        } catch (RuntimeException exception) {
            return;
        } finally {
            this.scheduleSupervisor();
        }
    }

    private void connect() {
        if (!this.shutdown && !this.socketClient.isConnected()) {
            if (this.connecting.compareAndSet(false, true)) {
                this.connectStartedAt = System.currentTimeMillis();
                Sadnes.getLogger().info("[network] connecting to {}", this.serverUrl);
                this.socketClient.connect(this.serverUrl, null).whenComplete((void2, throwable) -> {
                    this.connecting.set(false);
                    if (throwable != null) {
                        Sadnes.getLogger().warn("[network] connect failed: {}", describeError(throwable));
                    }
                });
            }
        }
    }

    private static void runRestore(String string, Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            Sadnes.getLogger().warn("[network] {} restore failed after connect: {}", string, describeError(exception));
        }
    }

    private static String describeError(Throwable throwable) {
        Throwable throwable2 = throwable;

        while (throwable2.getCause() != null && throwable2.getCause() != throwable2) {
            throwable2 = throwable2.getCause();
        }

        return throwable2.getClass().getSimpleName() + ": " + throwable2.getMessage();
    }

    private void onConnectionChanged(boolean flag) {
        if (flag) {
            this.failedAttempts = 0;
            runRestore("states", this.stateCache::resubscribe);
            runRestore("party", this.partyManager::resync);
            ConfigManager configManager = Sadnes.getInstance().getConfigManager();
            if (configManager != null) {
                runRestore("configs", configManager.getCloudSync()::refresh);
                runRestore("favorites", configManager::syncFavorites);
            }

            AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
            if (autoBuyManager != null && autoBuyManager.getCloudSync() != null) {
                runRestore("autobuy configs", autoBuyManager.getCloudSync()::refresh);
            }

            CollectorManager collectorManager = Sadnes.getInstance().getCollectorManager();
            if (collectorManager != null && collectorManager.getCloudConfig() != null) {
                runRestore("collector configs", collectorManager.getCloudConfig()::refresh);
            }
        }

        String string = flag ? "" : this.socketClient.getLastError();
        if (!flag) {
            this.failedAttempts++;
            if (isFatalError(string)) {
                this.fatalError = true;
            }
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null) {
            minecraftClient.execute(() -> this.handleConnectionChange(flag, string));
        }
    }

    private static boolean isFatalError(String string) {
        return string == null
            ? false
            : string.contains("banned")
                || string.contains("client integrity")
                || string.contains("bound to another computer")
                || string.contains("does not match the website")
                || string.contains("account not found")
                || string.contains("subscription expired");
    }

    private void handleConnectionChange(boolean flag, String string) {
        if (flag) {
            this.connectedAt = System.currentTimeMillis();
            this.connectCount++;
        } else {
            this.connectedAt = 0L;
            this.disconnectCount++;
            if (string != null && !string.isBlank()) {
                this.lastError = string;
            }
        }

        Sadnes.getEventBus().post(new BackendConnectionEvent(flag, string));
        if (flag) {
            Sadnes.getLogger().info("[network] connected to {}", this.serverUrl);
        } else if (string != null && !string.isBlank()) {
            if (string.contains("banned")) {
                NotificationUtil.error(NotificationSources.network(), string);
            } else {
                if (string.contains("kicked") || isFatalError(string)) {
                    NotificationUtil.warning(NotificationSources.network(), string);
                }
            }
        }
    }

    private static UserProfile createUserProfile() {
        String string = Profile.getHwid() == null ? "" : Profile.getHwid();
        return new UserProfile(
            Profile.getUid(), Profile.getUsername(), toRole(Profile.getRole()), string, Profile.getSubscriptionEndDate(), Profile.getAvatarUrl()
        );
    }

    private static UserRole toRole(Role role) {
        if (role == null) {
            return UserRole.DEFAULT;
        }

        try {
            return UserRole.valueOf(role.name());
        } catch (IllegalArgumentException illegalArgumentException) {
            return UserRole.DEFAULT;
        }
    }
}
