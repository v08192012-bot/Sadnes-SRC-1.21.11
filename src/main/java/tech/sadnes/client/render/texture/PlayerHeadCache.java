package tech.sadnes.client.render.texture;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import local.sadnes.OfflineRouting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;

public final class PlayerHeadCache {
    private static final String AVATAR_URL = "https://mc-heads.net/avatar/%s/64";
    private static final ConcurrentMap<String, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static Identifier steveHead;

    private PlayerHeadCache() {
    }

    public static Identifier getHead(String string) {
        if (string != null && !string.isBlank()) {
            String string2 = string.toLowerCase(Locale.ROOT);
            Identifier identifier = TEXTURES.get(string2);
            if (identifier != null) {
                return identifier;
            }

            if (FAILED.contains(string2)) {
                return null;
            }

            if (LOADING.add(string2)) {
                download(string2, string);
                return null;
            }
        }

        return null;
    }

    public static Identifier getSteveHead() {
        if (steveHead != null) {
            return steveHead;
        }

        try {
            Identifier identifier = Identifier.ofVanilla("textures/entity/player/wide/steve.png");
            Optional optional = MinecraftClient.getInstance().getResourceManager().getResource(identifier);
            if (optional.isEmpty()) {
                return null;
            }

            NativeImage nativeImage;
            try (InputStream inputStream = ((Resource)optional.get()).getInputStream()) {
                NativeImage nativeImage2 = NativeImage.read(inputStream);
                nativeImage = new NativeImage(64, 64, false);

                for (int i = 0; i < 8; i++) {
                    for (int i2 = 0; i2 < 8; i2++) {
                        int i3 = nativeImage2.getColorArgb(8 + i2, 8 + i);
                        int i4 = nativeImage2.getColorArgb(40 + i2, 8 + i);
                        int i5 = i4 >>> 24 != 0 ? i4 : i3;

                        for (int i6 = 0; i6 < 8; i6++) {
                            for (int i7 = 0; i7 < 8; i7++) {
                                nativeImage.setColorArgb(i2 * 8 + i7, i * 8 + i6, i5);
                            }
                        }
                    }
                }

                nativeImage2.close();
            }

            Identifier identifier2 = Identifier.of(ClientInfo.MOD_ID, "dynamic/steve_head");
            MipmappedTexture mipmappedTexture = new MipmappedTexture(() -> ClientInfo.MOD_ID + "_steve_head", nativeImage, 1);
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier2, mipmappedTexture);
            nativeImage.close();
            steveHead = identifier2;
            return identifier2;
        } catch (Exception exception) {
            Sadnes.getLogger().warn("[PlayerHeads] дефолтная голова Стива: {}", exception.toString());
            return null;
        }
    }

    private static void download(String string, String string2) {
        CompletableFuture.<byte[]>supplyAsync(() -> fetchBytes(string2)).thenAccept(bytes -> {
            if (bytes == null) {
                FAILED.add(string);
                LOADING.remove(string);
            } else {
                MinecraftClient.getInstance().execute(() -> register(string, bytes));
            }
        }).exceptionally(throwable -> {
            FAILED.add(string);
            LOADING.remove(string);
            return null;
        });
    }

    private static boolean isBlank(NativeImage nativeImage) {
        int i = nativeImage.getWidth();
        int i2 = nativeImage.getHeight();
        if (i > 0 && i2 > 0) {
            int i3 = 0;

            for (int i4 = 0; i4 < 8; i4++) {
                for (int i5 = 0; i5 < 8; i5++) {
                    int i6 = nativeImage.getColorArgb(i5 * i / 8, i4 * i2 / 8);
                    if (i6 >>> 24 >= 16) {
                        int i7 = Math.max(i6 >> 16 & 0xFF, Math.max(i6 >> 8 & 0xFF, i6 & 0xFF));
                        if (i7 > 24) {
                            i3++;
                        }
                    }
                }
            }

            return i3 < 4;
        } else {
            return true;
        }
    }

    private static byte[] fetchBytes(String string) {
        try {
            String string2 = URLEncoder.encode(string, StandardCharsets.UTF_8);
            URI uri = URI.create(String.format("https://mc-heads.net/avatar/%s/64", string2));
            HttpURLConnection httpURLConnection = (HttpURLConnection)OfflineRouting.openConnection(uri.toURL());
            httpURLConnection.setConnectTimeout(6000);
            httpURLConnection.setReadTimeout(6000);
            httpURLConnection.setRequestProperty("User-Agent", "Sadnes-client");
            if (httpURLConnection.getResponseCode() != 200) {
                return null;
            }

            try (InputStream inputStream = httpURLConnection.getInputStream()) {
                return inputStream.readAllBytes();
            }
        } catch (Exception exception) {
            return null;
        }
    }

    private static void register(String string, byte[] bytes) {
        try {
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
            if (!isBlank(nativeImage)) {
                String string2 = "head_" + Integer.toHexString(string.hashCode());
                Identifier identifier = Identifier.of(ClientInfo.MOD_ID, "dynamic/" + string2);
                MipmappedTexture mipmappedTexture = new MipmappedTexture(() -> ClientInfo.MOD_ID + "_" + string2, nativeImage, 1);
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, mipmappedTexture);
                nativeImage.close();
                TEXTURES.put(string, identifier);
                return;
            }

            nativeImage.close();
            FAILED.add(string);
        } catch (Exception exception) {
            Sadnes.getLogger().warn("[PlayerHeads] не удалось загрузить голову {}: {}", string, exception.toString());
            FAILED.add(string);
            return;
        } finally {
            LOADING.remove(string);
        }
    }
}
