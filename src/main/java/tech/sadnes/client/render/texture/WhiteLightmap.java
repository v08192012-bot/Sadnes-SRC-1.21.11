package tech.sadnes.client.render.texture;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

public final class WhiteLightmap {
    private static GpuTexture texture;
    private static GpuTextureView textureView;

    private WhiteLightmap() {
    }

    public static GpuTextureView getTextureView() {
        if (textureView == null) {
            GpuDevice gpuDevice = RenderSystem.getDevice();
            texture = gpuDevice.createTexture("Ambience White Lightmap", 12, TextureFormat.RGBA8, 16, 16, 1, 1);
            textureView = gpuDevice.createTextureView(texture);
            gpuDevice.createCommandEncoder().clearColorTexture(texture, -1);
        }

        return textureView;
    }
}
