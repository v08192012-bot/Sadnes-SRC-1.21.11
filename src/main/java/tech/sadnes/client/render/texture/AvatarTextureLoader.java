package tech.sadnes.client.render.texture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import local.sadnes.OfflineRouting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;

public final class AvatarTextureLoader {
    private static final ConcurrentMap<String, Identifier> LOADED = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> RETRY_AFTER = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean EMPTY_URL_LOGGED = new AtomicBoolean(false);

    private AvatarTextureLoader() {
    }

    public static Identifier getTexture(String string) {
        if (string != null && !string.isEmpty()) {
            Identifier identifier = LOADED.get(string);
            if (identifier != null) {
                return identifier;
            }

            Long longValue = RETRY_AFTER.get(string);
            if (longValue != null && System.currentTimeMillis() < longValue) {
                return null;
            }

            if (LOADING.add(string)) {
                Sadnes.getLogger().info("[Avatar] старт загрузки: {}", string);
                loadAsync(string);
                return null;
            }
        } else if (EMPTY_URL_LOGGED.compareAndSet(false, true)) {
            Sadnes.getLogger().info("[Avatar] URL пустой — грузить нечего (Profile.avatarUrl не выставлен)");
        }

        return null;
    }

    private static void loadAsync(String string) {
        CompletableFuture.<NativeImage>supplyAsync(() -> {
            byte[] bytes = download(string);
            return bytes == null ? null : decode(string, bytes);
        }).thenAccept(nativeImage -> {
            if (nativeImage == null) {
                markFailed(string);
            } else {
                MinecraftClient.getInstance().execute(() -> register(string, nativeImage));
            }
        }).exceptionally(throwable -> {
            Sadnes.getLogger().warn("[Avatar] ошибка асинхронной загрузки {}: {}", string, throwable.toString());
            markFailed(string);
            return null;
        });
    }

    private static void markFailed(String string) {
        RETRY_AFTER.put(string, System.currentTimeMillis() + 30000L);
        LOADING.remove(string);
    }

    private static void register(String string, NativeImage nativeImage) {
        try {
            String string2 = "res_" + Integer.toHexString(string.hashCode());
            Identifier identifier = Identifier.of(ClientInfo.MOD_ID, "dynamic/resource/" + string2);
            NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_res_" + string2, nativeImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeImageBackedTexture);
            LOADED.put(string, identifier);
            LOADING.remove(string);
            Sadnes.getLogger().info("[Avatar] текстура зарегистрирована: {} -> {}", string, identifier);
        } catch (Exception exception) {
            Sadnes.getLogger().error("Failed to register texture {}", string, exception);
            nativeImage.close();
            markFailed(string);
        }
    }

    private static NativeImage decode(String string, byte[] bytes) {
        try {
            return isPng(bytes) ? NativeImage.read(new ByteArrayInputStream(bytes)) : decodeWithStb(string, bytes);
        } catch (Exception exception) {
            Sadnes.getLogger().warn("[Avatar] не удалось декодировать {}: {}", string, exception.toString());
            return null;
        }
    }

    private static NativeImage decodeWithStb(String string, byte[] bytes) {
        ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);

        NativeImage nativeImage;
        try {
            byteBuffer.put(bytes).flip();

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer intBuffer = memoryStack.mallocInt(1);
                IntBuffer intBuffer2 = memoryStack.mallocInt(1);
                IntBuffer intBuffer3 = memoryStack.mallocInt(1);
                ByteBuffer byteBuffer2 = STBImage.stbi_load_from_memory(byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
                if (byteBuffer2 == null) {
                    Sadnes.getLogger().warn("[Avatar] STB не смог декодировать {}: {}", string, STBImage.stbi_failure_reason());
                    return null;
                }

                try {
                    int i = intBuffer.get(0);
                    int i2 = intBuffer2.get(0);
                    nativeImage = new NativeImage(i, i2, false);

                    for (int i3 = 0; i3 < i2; i3++) {
                        for (int i4 = 0; i4 < i; i4++) {
                            int i5 = (i4 + i3 * i) * 4;
                            int i6 = byteBuffer2.get(i5) & 255;
                            int i7 = byteBuffer2.get(i5 + 1) & 255;
                            int i8 = byteBuffer2.get(i5 + 2) & 255;
                            int i9 = byteBuffer2.get(i5 + 3) & 255;
                            nativeImage.setColorArgb(i4, i3, i9 << 24 | i6 << 16 | i7 << 8 | i8);
                        }
                    }
                } finally {
                    STBImage.stbi_image_free(byteBuffer2);
                }
            }
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }

        return nativeImage;
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
            && (bytes[0] & 255) == 137
            && bytes[1] == 80
            && bytes[2] == 78
            && bytes[3] == 71
            && (bytes[4] & 255) == 13
            && (bytes[5] & 255) == 10
            && (bytes[6] & 255) == 26
            && (bytes[7] & 255) == 10;
    }

    private static byte[] download(String string) {
        try {
            URLConnection urlConnection = OfflineRouting.openConnection(URI.create(string).toURL());
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestProperty("User-Agent", "Sadnes");

            try (
                InputStream inputStream = urlConnection.getInputStream();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ) {
                byte[] bytes = new byte[4096];

                int i;
                while ((i = inputStream.read(bytes)) != -1) {
                    byteArrayOutputStream.write(bytes, 0, i);
                }

                return byteArrayOutputStream.toByteArray();
            }
        } catch (IOException ioException) {
            Sadnes.getLogger().warn("[Avatar] ошибка скачивания {}: {}", string, ioException.toString());
            return null;
        }
    }
}
