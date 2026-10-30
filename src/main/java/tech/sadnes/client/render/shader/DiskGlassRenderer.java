package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BuiltBuffer;
import tech.sadnes.client.render.MeshDrawUtil;

public enum DiskGlassRenderer {
    INSTANCE;

    private SimpleFramebuffer captureTarget;
    private final PingPongBlurTarget blur = new PingPongBlurTarget("sadnes_disk_glass_blur");

    public void render(float f, BuiltBuffer builtBuffer) {
        this.renderWithPipeline(Pipelines.DISK_GLASS, f, builtBuffer);
    }

    public void renderWithPipeline(RenderPipeline renderPipeline, float f, BuiltBuffer builtBuffer) {
        if (builtBuffer != null) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            Framebuffer framebuffer = minecraftClient.getFramebuffer();
            if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
                int i = Math.max(1, framebuffer.textureWidth);
                int i2 = Math.max(1, framebuffer.textureHeight);
                this.captureTarget = ensureTarget(this.captureTarget, "sadnes_disk_glass_capture", i, i2);
                this.blur.resize(i, i2);
                GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

                try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                    GpuBufferSlice gpuBufferSlice = BlurRectUniformStorage.write(new BlurRectUniforms(0.0F, 0.0F, 1.0F, 1.0F, 0.0F));
                    FullscreenPass.blitWithUniforms(Pipelines.CLAMP_COPY, this.captureTarget, framebuffer.getColorAttachmentView(), gpuSampler, gpuBufferSlice);
                    this.blur.blur(this.captureTarget.getColorAttachmentView(), f, f, gpuSampler);
                }

                MeshDrawUtil.drawWithTexture(renderPipeline, builtBuffer, this.blur.getColorView(), gpuSampler);
            } else {
                builtBuffer.close();
            }
        }
    }

    private static SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, String string, int i, int i2) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, i, i2, false);
        }

        if (simpleFramebuffer.textureWidth != i || simpleFramebuffer.textureHeight != i2) {
            simpleFramebuffer.resize(i, i2);
        }

        return simpleFramebuffer;
    }
}
