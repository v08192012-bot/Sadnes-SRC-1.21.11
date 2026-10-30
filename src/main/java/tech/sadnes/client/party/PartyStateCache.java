package tech.sadnes.client.party;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.network.packet.PlayerStateEntry;

public final class PartyStateCache {
    private static final long STATE_TTL_MS = 8000L;
    private final SadnesSocketClient connection;
    private final ConcurrentHashMap<String, PartyStateCache.CachedState> byLogin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PartyStateCache.CachedState> byName = new ConcurrentHashMap<>();
    private volatile Set<String> subscribed = Set.of();

    public PlayerState getByLogin(String string) {
        return unwrapFresh(this.byLogin.get(string));
    }

    public PlayerState getByName(String string) {
        return unwrapFresh(this.byName.get(string));
    }

    public int size() {
        return this.byLogin.size();
    }

    public CompletableFuture<PlayerState> requestByLogin(String string) {
        return this.connection.fetchData(string).whenComplete((playerState, throwable) -> {
            if (throwable == null) {
                if (playerState == null) {
                    this.evict(string);
                } else {
                    this.store(string, playerState);
                }
            }
        });
    }

    public CompletableFuture<PlayerState> requestByName(String string) {
        return this.connection.fetchDataFresh(string).whenComplete((playerState, throwable) -> {
            if (throwable == null) {
                if (playerState == null) {
                    this.byName.remove(string);
                } else {
                    this.byName.put(playerState.getUsername(), new PartyStateCache.CachedState(playerState, System.currentTimeMillis()));
                }
            }
        });
    }

    public void setSubscribed(Collection<String> strings) {
        this.subscribed = Set.copyOf(strings);
        this.sendSubscription();
    }

    public void resubscribe() {
        this.sendSubscription();
    }

    public void applyUpdates(List<PlayerStateEntry> playerStateEntries) {
        for (PlayerStateEntry playerStateEntry : playerStateEntries) {
            this.store(playerStateEntry.login(), playerStateEntry.data());
        }
    }

    private static PlayerState unwrapFresh(PartyStateCache.CachedState cachedState) {
        return cachedState != null && System.currentTimeMillis() - cachedState.atMs() <= 8000L ? cachedState.data() : null;
    }

    private void store(String string, PlayerState playerState) {
        PartyStateCache.CachedState cachedState = new PartyStateCache.CachedState(playerState, System.currentTimeMillis());
        PartyStateCache.CachedState cachedState2 = this.byLogin.put(string, cachedState);
        if (cachedState2 != null && !cachedState2.data().getUsername().equals(playerState.getUsername())) {
            this.byName.remove(cachedState2.data().getUsername(), cachedState2);
        }

        this.byName.put(playerState.getUsername(), cachedState);
    }

    private void evict(String string) {
        PartyStateCache.CachedState cachedState = this.byLogin.remove(string);
        if (cachedState != null) {
            this.byName.remove(cachedState.data().getUsername(), cachedState);
        }
    }

    private void sendSubscription() {
        Set set = this.subscribed;
        if (!set.isEmpty()) {
            this.connection.sendNames(List.copyOf(set));
        }
    }

    public PartyStateCache(SadnesSocketClient sadnesSocketClient) {
        this.connection = sadnesSocketClient;
    }

    record CachedState(PlayerState data, long atMs) {
    }
}
