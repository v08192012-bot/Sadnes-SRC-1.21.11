package tech.sadnes.client.cosmetic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import local.sadnes.LocalResources;
import local.sadnes.OfflineRouting;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.network.BackendUrls;
import tech.sadnes.client.util.IMinecraft;

public final class CosmeticManager implements IMinecraft {
    private static final Logger LOGGER = LoggerFactory.getLogger(CosmeticManager.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).followRedirects(Redirect.NORMAL).build();
    private final Map<CosmeticType, Map<String, Cosmetic>> cosmeticsById = new EnumMap<>(CosmeticType.class);
    private final Map<CosmeticType, List<Cosmetic>> sortedCosmetics = new EnumMap<>(CosmeticType.class);
    private volatile boolean loaded;
    private volatile boolean loading;

    public CosmeticManager() {
        for (CosmeticType cosmeticType : CosmeticType.values()) {
            this.cosmeticsById.put(cosmeticType, new HashMap<>());
            this.sortedCosmetics.put(cosmeticType, List.of());
        }
    }

    public Cosmetic getCosmetic(CosmeticType cosmeticType, String string) {
        return this.cosmeticsById.get(cosmeticType).get(string);
    }

    public List<Cosmetic> getCosmetics(CosmeticType cosmeticType) {
        return this.sortedCosmetics.get(cosmeticType);
    }

    public void loadPack() {
        if (!this.loaded && !this.loading) {
            this.loading = true;
            CompletableFuture.runAsync(() -> {
                try {
                    this.downloadPack();
                    this.loaded = true;
                } catch (Exception exception) {
                    LOGGER.warn("[cosmetics] pack not loaded: {}", exception.toString());
                } finally {
                    this.loading = false;
                }
            });
        }
    }

    public void loadCosmetic(CosmeticType cosmeticType, String string, byte[] bytes, byte[] bytes2, byte[] bytes3) {
        try {
            Cosmetic cosmetic = CosmeticModelLoader.parse(cosmeticType, string, bytes);
            NativeImage nativeImage = NativeImage.read(bytes2);
            NativeImage nativeImage2 = NativeImage.read(bytes3);
            mc.execute(() -> this.registerCosmetic(cosmetic, nativeImage, nativeImage2));
        } catch (Exception exception) {
            LOGGER.warn("[cosmetics] {}/{} rejected: {}", cosmeticType.getId(), string, exception.toString());
        }
    }

    private void downloadPack() throws IOException, InterruptedException {
        InputStream localStream = LocalResources.open("/cosmetics/pack.zip");
        if (localStream != null) {
            try (InputStream inputStream = localStream) {
                this.parsePack(inputStream);
            }

            LOGGER.info("[cosmetics] pack loaded from resources");
            return;
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(OfflineRouting.rewrite(URI.create(BackendUrls.urlNoCache("/cosmetics/pack.zip"))))
            .timeout(Duration.ofMinutes(2L))
            .GET()
            .build();
        HttpResponse httpResponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofInputStream());
        if (httpResponse.statusCode() != 200) {
            throw new IOException("HTTP " + httpResponse.statusCode());
        }

        try (InputStream inputStream = (InputStream)httpResponse.body()) {
            this.parsePack(inputStream);
        }
    }

    private void parsePack(InputStream inputStream) throws IOException {
        LinkedHashMap<String, Map<String, byte[]>> linkedHashMap = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String string = zipEntry.getName();
                int i = string.lastIndexOf(47);
                if (!zipEntry.isDirectory() && i >= 0) {
                    byte[] bytes = zipInputStream.readNBytes(8388609);
                    if (bytes.length > 8388608) {
                        throw new IOException("oversized " + string);
                    }

                    linkedHashMap.computeIfAbsent(string.substring(0, i), string2 -> new HashMap<>()).put(string.substring(i + 1), bytes);
                }
            }
        }

        for (Entry<String, Map<String, byte[]>> entry : linkedHashMap.entrySet()) {
            int i2 = ((String)entry.getKey()).indexOf(47);
            CosmeticType cosmeticType = i2 < 0 ? null : CosmeticType.fromId(((String)entry.getKey()).substring(0, i2));
            Map map = (Map)entry.getValue();
            byte[] bytes2 = (byte[])map.get("model.json");
            byte[] bytes3 = (byte[])map.get("texture.png");
            byte[] bytes4 = (byte[])map.get("preview.png");
            boolean flag = cosmeticType != null && bytes2 != null && bytes3 != null && bytes4 != null;
            if (flag) {
                this.loadCosmetic(cosmeticType, ((String)entry.getKey()).substring(i2 + 1), bytes2, bytes3, bytes4);
            }
        }
    }

    private void registerCosmetic(Cosmetic cosmetic, NativeImage nativeImage, NativeImage nativeImage2) {
        TextureManager textureManager = mc.getTextureManager();
        NativeImageBackedTexture nativeImageBackedTexture = cosmetic.smooth() ? new NativeImageBackedTexture(cosmetic.texture()::toString, nativeImage) {
            {
                this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            }
        } : new NativeImageBackedTexture(cosmetic.texture()::toString, nativeImage);
        textureManager.registerTexture(cosmetic.texture(), nativeImageBackedTexture);
        textureManager.registerTexture(cosmetic.preview(), new NativeImageBackedTexture(cosmetic.preview()::toString, nativeImage2));
        Map map = this.cosmeticsById.get(cosmetic.type());
        map.put(cosmetic.id(), cosmetic);
        ArrayList arrayList = new ArrayList(map.values());
        arrayList.sort(Comparator.comparing(Cosmetic::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Cosmetic::id));
        this.sortedCosmetics.put(cosmetic.type(), Collections.unmodifiableList(arrayList));
    }
}
