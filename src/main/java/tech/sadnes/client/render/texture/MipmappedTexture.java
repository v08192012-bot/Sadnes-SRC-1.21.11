package tech.sadnes.client.render.texture;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.function.Supplier;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;

public final class MipmappedTexture extends AbstractTexture {
    public MipmappedTexture(Supplier<String> supplier, NativeImage nativeImage) {
        this(supplier, nativeImage, Integer.MAX_VALUE);
    }

    public MipmappedTexture(Supplier<String> supplier, NativeImage[] nativeImages) {
        NativeImage nativeImage = nativeImages[0];
        int i = nativeImage.getWidth();
        int i2 = nativeImage.getHeight();
        int i3 = nativeImages.length;
        GpuDevice gpuDevice = RenderSystem.getDevice();
        this.glTexture = gpuDevice.createTexture(supplier, 5, TextureFormat.RGBA8, i, i2, 1, i3);
        this.glTextureView = gpuDevice.createTextureView(this.glTexture, 0, i3);
        CommandEncoder commandEncoder = gpuDevice.createCommandEncoder();
        commandEncoder.writeToTexture(this.glTexture, nativeImage);

        for (int i4 = 1; i4 < i3; i4++) {
            NativeImage nativeImage2 = nativeImages[i4];
            commandEncoder.writeToTexture(this.glTexture, nativeImage2, i4, 0, 0, 0, nativeImage2.getWidth(), nativeImage2.getHeight(), 0, 0);
        }
    }

    public static NativeImage[] generateMipmaps(NativeImage nativeImage, int i) {
        int i2 = nativeImage.getWidth();
        int i3 = nativeImage.getHeight();
        int i4 = Math.max(1, Math.min(getMaxMipLevels(i2, i3), i));
        NativeImage[] nativeImages = new NativeImage[i4];
        nativeImages[0] = nativeImage;

        try {
            for (int i5 = 1; i5 < i4; i5++) {
                int i6 = Math.max(1, i2 >> i5);
                int i7 = Math.max(1, i3 >> i5);
                NativeImage nativeImage2 = new NativeImage(nativeImage.getFormat(), i6, i7, false);
                NativeImage nativeImage3 = nativeImages[i5 - 1];
                nativeImage3.resizeSubRectTo(0, 0, nativeImage3.getWidth(), nativeImage3.getHeight(), nativeImage2);
                nativeImages[i5] = nativeImage2;
            }

            return nativeImages;
        } catch (RuntimeException | Error exception) {
            for (int i8 = 1; i8 < i4; i8++) {
                if (nativeImages[i8] != null) {
                    nativeImages[i8].close();
                }
            }

            throw exception;
        }
    }

    public MipmappedTexture(Supplier<String> supplier, NativeImage nativeImage, int i) {
        int i2 = nativeImage.getWidth();
        int i3 = nativeImage.getHeight();
        int i4 = Math.max(1, Math.min(getMaxMipLevels(i2, i3), i));
        GpuDevice gpuDevice = RenderSystem.getDevice();
        this.glTexture = gpuDevice.createTexture(supplier, 5, TextureFormat.RGBA8, i2, i3, 1, i4);
        this.glTextureView = gpuDevice.createTextureView(this.glTexture, 0, i4);
        CommandEncoder commandEncoder = gpuDevice.createCommandEncoder();
        commandEncoder.writeToTexture(this.glTexture, nativeImage);
        if (i4 != 1) {
            NativeImage nativeImage2 = nativeImage;
            boolean flag = false;

            try {
                for (int i5 = 1; i5 < i4; i5++) {
                    int i6 = Math.max(1, i2 >> i5);
                    int i7 = Math.max(1, i3 >> i5);
                    NativeImage nativeImage3 = new NativeImage(nativeImage.getFormat(), i6, i7, false);
                    nativeImage2.resizeSubRectTo(0, 0, nativeImage2.getWidth(), nativeImage2.getHeight(), nativeImage3);
                    commandEncoder.writeToTexture(this.glTexture, nativeImage3, i5, 0, 0, 0, i6, i7, 0, 0);
                    if (flag) {
                        nativeImage2.close();
                    }

                    nativeImage2 = nativeImage3;
                    flag = true;
                }
            } finally {
                if (flag) {
                    nativeImage2.close();
                }
            }
        }
    }

    private static int getMaxMipLevels(int i, int i2) {
        int i3 = Math.max(1, Math.min(i, i2));
        return 1 + (int)(Math.log(i3) / Math.log(2.0));
    }
}
