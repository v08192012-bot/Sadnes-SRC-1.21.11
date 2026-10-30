package tech.sadnes.client.render.texture;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.ResourceUtil;

public final class ChamsNoiseTexture {
    private static final int SIZE = 1024;
    private static final float PERIOD = 16.0F;
    private static NativeImageBackedTexture texture;

    private ChamsNoiseTexture() {
    }

    public static GpuTextureView getTexture() {
        if (texture == null) {
            texture = createTexture();
        }

        return ResourceUtil.getView(texture);
    }

    public static void preload() {
        getTexture();
    }

    private static NativeImageBackedTexture createTexture() {
        NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_chams_noise_lut", 1024, 1024, false);
        NativeImage nativeImage = nativeImageBackedTexture.getImage();

        for (int i = 0; i < 1024; i++) {
            for (int i2 = 0; i2 < 1024; i2++) {
                float f = fractalNoise(i2 / 64.0F, i / 64.0F, 16);
                int i3 = Math.min(Math.round(f * 65535.0F), 65535);
                nativeImage.setColorArgb(i2, i, 0xFF000000 | i3 >> 8 << 16 | (i3 & 0xFF) << 8);
            }
        }

        nativeImageBackedTexture.upload();
        return nativeImageBackedTexture;
    }

    private static float fractalNoise(float f, float f2, int i) {
        float f3 = 0.0F;
        float f4 = 0.5F;
        int i2 = i;

        for (int i3 = 0; i3 < 5; i3++) {
            f3 += f4 * valueNoise(f, f2, i2);
            f *= 2.0F;
            f2 *= 2.0F;
            i2 <<= 1;
            f4 *= 0.5F;
        }

        return f3;
    }

    private static float valueNoise(float f, float f2, int i) {
        int i2 = (int)Math.floor(f);
        int i3 = (int)Math.floor(f2);
        float f3 = f - i2;
        float f4 = f2 - i3;
        float f5 = f3 * f3 * (3.0F - 2.0F * f3);
        float f6 = f4 * f4 * (3.0F - 2.0F * f4);
        float f7 = hash(i2, i3, i);
        float f8 = hash(i2 + 1, i3, i);
        float f9 = hash(i2, i3 + 1, i);
        float f10 = hash(i2 + 1, i3 + 1, i);
        float f11 = f7 + (f8 - f7) * f5;
        float f12 = f9 + (f10 - f9) * f5;
        return f11 + (f12 - f11) * f6;
    }

    private static float hash(int i, int i2, int i3) {
        int i4 = Math.floorMod(i, i3) * 374761393 + Math.floorMod(i2, i3) * 668265263;
        int i5 = (i4 ^ i4 >>> 13) * 1274126177;
        return ((i5 ^ i5 >>> 16) & 16777215) / 1.6777216E7F;
    }
}
