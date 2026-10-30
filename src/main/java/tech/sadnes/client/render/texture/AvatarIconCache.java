package tech.sadnes.client.render.texture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import local.sadnes.OfflineRouting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;

public final class AvatarIconCache {
    private static final ConcurrentMap<String, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private AvatarIconCache() {
    }

    public static Identifier getFromFile(Path path) {
        if (path == null) {
            return null;
        }

        String string = path.toAbsolutePath().toString();
        Identifier identifier = TEXTURES.get(string);
        if (identifier != null) {
            return identifier;
        }

        if (FAILED.contains(string)) {
            return null;
        }

        if (LOADING.add(string)) {
            loadFromFile(string, path);
        }

        return null;
    }

    public static Identifier getFromUrl(String string, String string2) {
        Identifier identifier = TEXTURES.get(string);
        if (identifier != null) {
            return identifier;
        }

        if (FAILED.contains(string)) {
            return null;
        }

        if (LOADING.add(string)) {
            loadFromUrl(string, string2);
        }

        return null;
    }

    public static Identifier getFromResource(String string, String string2) {
        Identifier identifier = TEXTURES.get(string);
        if (identifier != null) {
            return identifier;
        }

        if (FAILED.contains(string)) {
            return null;
        }

        if (LOADING.add(string)) {
            loadFromResource(string, string2);
        }

        return null;
    }

    private static void loadFromResource(String string, String string2) {
        CompletableFuture.<byte[]>supplyAsync(() -> {
            try (InputStream inputStream = AvatarIconCache.class.getResourceAsStream(string2)) {
                return inputStream == null ? null : inputStream.readAllBytes();
            } catch (Exception exception) {
                return null;
            }
        }).thenAccept(bytes -> {
            if (bytes == null) {
                FAILED.add(string);
                LOADING.remove(string);
            } else {
                MinecraftClient.getInstance().execute(() -> registerTexture(string, bytes));
            }
        }).exceptionally(throwable -> {
            FAILED.add(string);
            LOADING.remove(string);
            return null;
        });
    }

    private static void loadFromUrl(String string, String string2) {
        CompletableFuture.<byte[]>supplyAsync(() -> {
            try (InputStream inputStream = OfflineRouting.openStream(URI.create(string2).toURL())) {
                return inputStream.readAllBytes();
            } catch (Exception exception) {
                return null;
            }
        }).thenAccept(bytes -> {
            if (bytes == null) {
                FAILED.add(string);
                LOADING.remove(string);
            } else {
                MinecraftClient.getInstance().execute(() -> registerTexture(string, bytes));
            }
        }).exceptionally(throwable -> {
            FAILED.add(string);
            LOADING.remove(string);
            return null;
        });
    }

    private static void loadFromFile(String string, Path path) {
        CompletableFuture.<byte[]>supplyAsync(() -> {
            try {
                return Files.exists(path) ? Files.readAllBytes(path) : null;
            } catch (IOException ioException) {
                return null;
            }
        }).thenAccept(bytes -> {
            if (bytes == null) {
                Sadnes.getLogger().warn("[AvatarIcons] файл не прочитан: {}", string);
                FAILED.add(string);
                LOADING.remove(string);
            } else {
                MinecraftClient.getInstance().execute(() -> registerTexture(string, bytes));
            }
        }).exceptionally(throwable -> {
            Sadnes.getLogger().warn("[AvatarIcons] ошибка чтения {}: {}", string, throwable.toString());
            FAILED.add(string);
            LOADING.remove(string);
            return null;
        });
    }

    private static void registerTexture(String string, byte[] bytes) {
        try {
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
            String string2 = "avatar_" + Integer.toHexString(string.hashCode());
            Identifier identifier = Identifier.of(ClientInfo.MOD_ID, "dynamic/" + string2);
            MipmappedTexture mipmappedTexture = new MipmappedTexture(() -> ClientInfo.MOD_ID + "_" + string2, nativeImage, 1);
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, mipmappedTexture);
            nativeImage.close();
            TEXTURES.put(string, identifier);
            Sadnes.getLogger().info("[AvatarIcons] загружено: {}", identifier);
        } catch (Exception exception) {
            Sadnes.getLogger().warn("[AvatarIcons] не удалось загрузить {}: {}", string, exception.toString());
            FAILED.add(string);
        } finally {
            LOADING.remove(string);
        }
    }
}
