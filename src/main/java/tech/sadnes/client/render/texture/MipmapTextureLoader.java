package tech.sadnes.client.render.texture;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.util.ResourceUtil;

public final class MipmapTextureLoader {
    private static final String PREFIX = "mip/";
    private static final Map<Identifier, Identifier> LOADED = new HashMap<>();

    private MipmapTextureLoader() {
    }

    public static AbstractTexture getTexture(Identifier identifier) {
        Identifier identifier2 = load(identifier);
        if (identifier2 == null) {
            return null;
        }

        AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier2);
        if (isValid(abstractTexture)) {
            return abstractTexture;
        }

        LOADED.remove(identifier);
        MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier2);
        Identifier identifier3 = load(identifier);
        return identifier3 == null ? null : MinecraftClient.getInstance().getTextureManager().getTexture(identifier3);
    }

    public static void clear() {
        if (!LOADED.isEmpty()) {
            TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

            for (Identifier identifier : LOADED.values()) {
                textureManager.destroyTexture(identifier);
            }

            LOADED.clear();
        }
    }

    private static Identifier load(Identifier identifier) {
        Identifier identifier2 = LOADED.get(identifier);
        if (identifier2 != null) {
            return identifier2;
        }

        Identifier identifier3 = Identifier.of(ClientInfo.MOD_ID, "mip/" + identifier.getNamespace() + "/" + identifier.getPath());
        Optional optional = MinecraftClient.getInstance().getResourceManager().getResource(identifier);
        if (optional.isEmpty()) {
            Sadnes.getLogger().warn("Mipmap loader: resource not found {}", identifier);
            return null;
        }

        try (
            InputStream inputStream = ((Resource)optional.get()).getInputStream();
            NativeImage nativeImage = NativeImage.read(inputStream);
        ) {
            MipmappedTexture mipmappedTexture = new MipmappedTexture(() -> "mip_" + identifier, nativeImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier3, mipmappedTexture);
            LOADED.put(identifier, identifier3);
            return identifier3;
        } catch (Exception exception) {
            Sadnes.getLogger().error("Mipmap loader: failed to build {}", identifier, exception);
            MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier3);
            return null;
        }
    }

    private static boolean isValid(AbstractTexture abstractTexture) {
        return ResourceUtil.getView(abstractTexture) != null;
    }
}
