package tech.sadnes.client.config;

import com.google.gson.Gson;
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

public final class KdStats {
    private static final Logger LOGGER = LoggerFactory.getLogger("KdStats");
    private static final Gson GSON = new Gson();
    private static final Type STATS_TYPE = (new TypeToken<Map<String, int[]>>() {}).getType();
    private static final Map<String, int[]> STATS = new HashMap<>();
    private static boolean loaded;

    private KdStats() {
    }

    public static void addKill(String string) {
        getStats(string)[0]++;
        save();
    }

    public static void addDeath(String string) {
        getStats(string)[1]++;
        save();
    }

    public static int getKills(String string) {
        return getStats(string)[0];
    }

    public static int getDeaths(String string) {
        return getStats(string)[1];
    }

    public static double getKdRatio(String string) {
        int[] ints = getStats(string);
        return ints[1] == 0 ? ints[0] : (double)ints[0] / ints[1];
    }

    private static int[] getStats(String string) {
        load();
        return STATS.computeIfAbsent(string, string2 -> new int[]{0, 0});
    }

    private static void load() {
        if (!loaded) {
            loaded = true;

            try {
                Path path = getPath();
                if (!Files.exists(path)) {
                    return;
                }

                Map map = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), STATS_TYPE);
                if (map != null) {
                    STATS.putAll(map);
                }
            } catch (Exception exception) {
                LOGGER.warn("Failed to load kd stats: {}", exception.getMessage());
            }
        }
    }

    private static void save() {
        try {
            Path path = getPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(STATS, STATS_TYPE), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save kd stats: {}", exception.getMessage());
        }
    }

    private static Path getPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("kdstats.json");
    }
}
