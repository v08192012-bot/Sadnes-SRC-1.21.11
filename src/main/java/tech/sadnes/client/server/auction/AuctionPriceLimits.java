package tech.sadnes.client.server.auction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;

public final class AuctionPriceLimits {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionPriceLimits.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type MAP_TYPE = (new TypeToken<Map<String, Map<String, Long>>>() {}).getType();
    private final Map<String, Map<String, Long>> limits = new HashMap<>();
    private boolean loaded;

    public long getLimit(String string) {
        this.ensureLoaded();
        Map<String, Long> map = this.limits.get(AuctionDumper.getServerName());
        return map == null ? 0L : map.getOrDefault(string, 0L);
    }

    public void setLimit(String string, long l) {
        this.ensureLoaded();
        Map map = this.limits.computeIfAbsent(AuctionDumper.getServerName(), string2 -> new HashMap<>());
        if (l > 0L) {
            map.put(string, l);
            this.save();
        } else {
            map.remove(string);
            this.save();
        }
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.loaded = true;
            Path path = this.getFilePath();
            if (Files.exists(path)) {
                try {
                    Map map = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), MAP_TYPE);
                    if (map != null) {
                        this.limits.putAll(map);
                    }
                } catch (Exception exception) {
                    LOGGER.warn("Failed to load auction price limits: {}", exception.getMessage());
                }
            }
        }
    }

    private void save() {
        Path path = this.getFilePath();

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this.limits, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save auction price limits: {}", exception.getMessage());
        }
    }

    private Path getFilePath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("auction-limits.json");
    }
}
