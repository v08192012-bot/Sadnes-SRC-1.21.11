package tech.sadnes.client.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import tech.sadnes.client.ClientInfo;

public final class LayoutStorage {
    private static final LocalConfigStorage STORAGE = new LocalConfigStorage(
        MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("layouts")
    );
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = (new TypeToken<LinkedHashMap<Integer, String>>() {}).getType();

    private LayoutStorage() {
    }

    public static boolean save(String string, Map<Integer, String> map) {
        return STORAGE.write(string, GSON.toJson(map));
    }

    public static Map<Integer, String> load(String string) {
        String string2 = STORAGE.read(string);
        if (string2 == null) {
            return null;
        }

        try {
            return GSON.fromJson(string2, MAP_TYPE);
        } catch (Exception exception) {
            return null;
        }
    }

    public static boolean delete(String string) {
        return STORAGE.delete(string);
    }

    public static void deleteAll() {
        for (String string : list()) {
            STORAGE.delete(string);
        }
    }

    public static List<String> list() {
        return STORAGE.list();
    }

    public static boolean exists(String string) {
        return STORAGE.exists(string);
    }
}
