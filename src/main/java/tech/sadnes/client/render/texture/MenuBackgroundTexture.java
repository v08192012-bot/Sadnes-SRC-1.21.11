package tech.sadnes.client.render.texture;

import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.util.crypto.AssetCrypto;

public final class MenuBackgroundTexture {
    private static final String RESOURCE_PATH = "/assets/" + ClientInfo.MOD_ID + "/texture/other/menu_bg.png";
    private static final Identifier TEXTURE_ID = Identifier.of(ClientInfo.MOD_ID, "dynamic/boot_menu_bg");
    private static Identifier texture;
    private static int width;
    private static int height;

    private MenuBackgroundTexture() {
    }

    public static Identifier getTexture() {
        if (texture == null) {
            texture = load();
        }

        return texture;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static void unload() {
        if (texture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(texture);
            texture = null;
        }
    }

    private static Identifier load() {
        try {
            InputStream inputStream = AssetCrypto.wrap(MenuBackgroundTexture.class.getResourceAsStream(RESOURCE_PATH));

            Identifier identifier;
            try {
                if (inputStream == null) {
                    Sadnes.getLogger().warn("Boot menu background resource not found: {}", RESOURCE_PATH);
                    return null;
                }

                NativeImage nativeImage = NativeImage.read(inputStream);
                width = nativeImage.getWidth();
                height = nativeImage.getHeight();
                NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_boot_menu_bg", nativeImage);
                MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, nativeImageBackedTexture);
                identifier = TEXTURE_ID;
            } catch (Throwable throwable) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }

                throw throwable;
            }

            inputStream.close();
            return identifier;
        } catch (Exception exception) {
            Sadnes.getLogger().error("Failed to load boot menu background", exception);
            return null;
        }
    }
}
