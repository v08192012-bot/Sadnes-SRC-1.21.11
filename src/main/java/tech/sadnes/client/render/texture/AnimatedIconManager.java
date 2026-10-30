package tech.sadnes.client.render.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.util.ResourceUtil;
import tech.sadnes.client.util.crypto.AssetCrypto;

public final class AnimatedIconManager {
    private static final int MIPMAP_LEVELS = 3;
    private static final Map<AnimatedIcon, Identifier> TEXTURES = new EnumMap<>(AnimatedIcon.class);
    private static final long START_TIME = System.nanoTime();

    private AnimatedIconManager() {
    }

    public static TextureSetup getTextureSetup(AnimatedIcon animatedIcon) {
        Identifier identifier = getTexture(animatedIcon);
        if (identifier == null) {
            return null;
        }

        GpuTextureView gpuTextureView = ResourceUtil.getTextureView(identifier);
        return gpuTextureView == null ? null : TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true));
    }

    public static Identifier getTexture(AnimatedIcon animatedIcon) {
        if (animatedIcon == null) {
            return null;
        }

        Identifier identifier = TEXTURES.get(animatedIcon);
        if (identifier == null) {
            identifier = loadTexture(animatedIcon);
            if (identifier == null) {
                return null;
            }

            TEXTURES.put(animatedIcon, identifier);
        }

        return identifier;
    }

    public static int getCurrentFrame(AnimatedIcon animatedIcon) {
        if (animatedIcon != null && animatedIcon.getFrameCount() > 0) {
            double d = (System.nanoTime() - START_TIME) / 1.0E9;
            return Math.floorMod((int)(d * animatedIcon.getFps()), animatedIcon.getFrameCount());
        } else {
            return 0;
        }
    }

    public static float getFrameProgress(AnimatedIcon animatedIcon) {
        if (animatedIcon != null && animatedIcon.getFrameCount() > 0) {
            double d = (System.nanoTime() - START_TIME) / 1.0E9;
            double d2 = d * animatedIcon.getFps();
            return (float)(d2 - Math.floor(d2));
        } else {
            return 0.0F;
        }
    }

    public static int getFrameAt(AnimatedIcon animatedIcon, float f) {
        if (animatedIcon != null && animatedIcon.getFrameCount() > 0) {
            int i = animatedIcon.getFrameCount() - 1;
            int i2 = Math.round(f * i);
            if (i2 < 0) {
                return 0;
            } else {
                return i2 > i ? i : i2;
            }
        } else {
            return 0;
        }
    }

    public static void clear() {
        if (!TEXTURES.isEmpty()) {
            for (Identifier identifier : TEXTURES.values()) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(identifier);
            }

            TEXTURES.clear();
        }
    }

    private static Identifier loadTexture(AnimatedIcon animatedIcon) {
        Identifier identifier = animatedIcon.getLocation();

        try {
            InputStream inputStream = AssetCrypto.wrap(AnimatedIconManager.class.getResourceAsStream("/assets/" + identifier.getNamespace() + "/" + identifier.getPath()));

            Identifier identifier2;
            try {
                if (inputStream == null) {
                    Sadnes.getLogger().warn("Animated icon resource not found: {}", identifier);
                    return null;
                }

                NativeImage nativeImage = NativeImage.read(inputStream);
                String string = animatedIcon.name().toLowerCase() + "_anim";
                identifier2 = Identifier.of(ClientInfo.MOD_ID, "dynamic/icon/" + string);
                MipmappedTexture mipmappedTexture = new MipmappedTexture(() -> ClientInfo.MOD_ID + "_anim_icon_" + string, nativeImage, 3);
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier2, mipmappedTexture);
                nativeImage.close();
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
            return identifier2;
        } catch (Exception exception) {
            Sadnes.getLogger().error("Failed to load animated icon {}", identifier, exception);
            return null;
        }
    }
}
