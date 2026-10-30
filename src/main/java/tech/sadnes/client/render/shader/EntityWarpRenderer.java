package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import tech.sadnes.client.render.EntityMaskRenderer;
import tech.sadnes.client.util.color.ColorUtil;

public enum EntityWarpRenderer {
    INSTANCE;

    private SimpleFramebuffer maskTarget;
    private SimpleFramebuffer captureTarget;
    private final PingPongBlurTarget blurTarget = new PingPongBlurTarget("sadnes_entity_warp_blur");

    public void renderEntities(MatrixStack matrixStack, Camera camera, float f, List<Entity> entities, float f2, float f3, int i) {
        if (!entities.isEmpty() && !(f2 <= 0.002F) && this.ensureTargets()) {
            EntityMaskRenderer.renderEntities(this.maskTarget, matrixStack, camera, f, entities);
            this.composite(Pipelines.ENTITY_WARP, f2, f3, i, null);
        }
    }

    public void render(float f, float f2, int i, Runnable runnable) {
        this.renderWithPipeline(Pipelines.ENTITY_WARP, f, f2, i, runnable);
    }

    public void renderWithPipeline(RenderPipeline renderPipeline, float f, float f2, int i, Runnable runnable) {
        this.renderWithPipeline(renderPipeline, f, f2, i, null, runnable);
    }

    public void renderWithPipeline(RenderPipeline renderPipeline, float f, float f2, int i, Float floatValue, Runnable runnable) {
        if (!(f <= 0.002F) && this.ensureTargets()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.maskTarget.getColorAttachment(), 0);
            GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride;
            RenderSystem.outputColorTextureOverride = this.maskTarget.getColorAttachmentView();

            try {
                runnable.run();
            } finally {
                RenderSystem.outputColorTextureOverride = gpuTextureView;
            }

            this.composite(renderPipeline, f, f2, i, floatValue);
        }
    }

    private boolean ensureTargets() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            Window window = minecraftClient.getWindow();
            int i = Math.max(1, window.getFramebufferWidth());
            int i2 = Math.max(1, window.getFramebufferHeight());
            this.maskTarget = ensureFramebuffer(this.maskTarget, "sadnes_entity_warp_mask", i, i2, true);
            this.captureTarget = ensureFramebuffer(this.captureTarget, "sadnes_entity_warp_capture", i, i2, false);
            this.blurTarget.resize(i, i2);
            return true;
        } else {
            return false;
        }
    }

    private void composite(RenderPipeline renderPipeline, float f, float f2, int i, Float floatValue) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
            GpuBufferSlice gpuBufferSlice = BlurRectUniformStorage.write(new BlurRectUniforms(0.0F, 0.0F, 1.0F, 1.0F, 0.0F));
            FullscreenPass.blitWithUniformsUnflipped(Pipelines.CLAMP_COPY, this.captureTarget, framebuffer.getColorAttachmentView(), gpuSampler, gpuBufferSlice);
            this.blurTarget.blurFirstPassAlt(this.maskTarget.getColorAttachmentView(), f2, f2, gpuSampler);
        }

        FullscreenPass.drawGlowCompositeWithFlame(
            renderPipeline,
            framebuffer.getColorAttachmentView(),
            this.captureTarget.getColorAttachmentView(),
            this.blurTarget.getColorView(),
            gpuSampler,
            ColorUtil.setAlpha(i, Math.round(Math.min(f, 1.0F) * 255.0F)),
            floatValue
        );
    }

    private static SimpleFramebuffer ensureFramebuffer(SimpleFramebuffer simpleFramebuffer, String string, int i, int i2, boolean flag) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, i, i2, flag);
        }

        if (simpleFramebuffer.textureWidth != i || simpleFramebuffer.textureHeight != i2) {
            simpleFramebuffer.resize(i, i2);
        }

        return simpleFramebuffer;
    }
}
