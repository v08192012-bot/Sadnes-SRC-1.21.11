package tech.sadnes.client.render.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.util.ResourceUtil;

public final class EasingLutTexture {
    private static final int LUT_SIZE = 256;
    private static final Map<Easing, NativeImageBackedTexture> CACHE = new IdentityHashMap<>();

    private EasingLutTexture() {
    }

    public static TextureSetup getTextureSetup(Easing easing) {
        if (easing == null) {
            return null;
        }

        NativeImageBackedTexture nativeImageBackedTexture = CACHE.computeIfAbsent(easing, EasingLutTexture::createLutTexture);
        GpuTextureView gpuTextureView = ResourceUtil.getView(nativeImageBackedTexture);
        return gpuTextureView == null ? null : TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
    }

    private static NativeImageBackedTexture createLutTexture(Easing easing) {
        NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_easing_lut", 256, 1, false);
        NativeImage nativeImage = nativeImageBackedTexture.getImage();

        for (int i = 0; i < 256; i++) {
            float f = i / 255.0F;
            int i2 = MathHelper.clamp(Math.round((float)easing.ease(f) * 255.0F), 0, 255);
            nativeImage.setColorArgb(i, 0, 0xFF000000 | i2 << 16 | i2 << 8 | i2);
        }

        nativeImageBackedTexture.upload();
        return nativeImageBackedTexture;
    }
}
