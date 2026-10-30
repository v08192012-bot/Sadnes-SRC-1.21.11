package tech.sadnes.client.party;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import ua.mintantileak.profile.Profile;

public final class PlayerLoginResolver {
    private static final long CACHE_TTL_MS = 30000L;
    private final SadnesSocketClient socketClient;
    private final ConcurrentHashMap<String, PlayerLoginResolver.CacheEntry> cache = new ConcurrentHashMap<>();

    public String resolveLogin(String string) {
        if (IMinecraft.mc.player != null && IMinecraft.mc.player.getGameProfile().name().equals(string)) {
            return Profile.getUsername();
        }

        PlayerLoginResolver.CacheEntry cacheEntry = this.cache.get(string);
        long l = System.currentTimeMillis();
        if (cacheEntry == null || l - cacheEntry.atMs() > 30000L) {
            this.refreshEntry(string, cacheEntry, l);
        }

        return cacheEntry == null ? null : cacheEntry.login();
    }

    private void refreshEntry(String string, PlayerLoginResolver.CacheEntry cacheEntry, long l) {
        if (this.socketClient.isConnected()) {
            if (this.cache.size() > 256) {
                this.cache.values().removeIf(cacheEntry2 -> l - cacheEntry2.atMs() > 30000L);
            }

            this.cache.put(string, new PlayerLoginResolver.CacheEntry(cacheEntry == null ? null : cacheEntry.login(), l));
            String string2 = ServerManager.getServerName();
            this.socketClient.fetchEntry(string).whenComplete((playerStateEntry, throwable) -> {
                if (throwable == null) {
                    boolean flag;
                    if (playerStateEntry.data() != null) {
                        flag = string2.equals(playerStateEntry.data().getServer());
                    } else {
                        flag = false;
                    }

                    this.cache.put(string, new PlayerLoginResolver.CacheEntry(flag ? playerStateEntry.login() : null, System.currentTimeMillis()));
                    if (flag) {
                        this.socketClient.sendTargets(List.of(playerStateEntry.login()));
                    }
                }
            });
        }
    }

    public PlayerLoginResolver(SadnesSocketClient sadnesSocketClient) {
        this.socketClient = sadnesSocketClient;
    }

    record CacheEntry(String login, long atMs) {
    }
}
