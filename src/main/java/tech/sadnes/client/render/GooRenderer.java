package tech.sadnes.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.util.Window;
import tech.sadnes.client.render.shader.FullscreenPass;
import tech.sadnes.client.render.shader.Pipelines;

public enum GooRenderer {
    INSTANCE;

    private static final int MIN_PASSES = 2;
    private static final float BLUR_OFFSET = 1.2F;
    private static final int DOWNSCALE = 2;
    private static final int MAX_PASSES = 5;
    private SimpleFramebuffer captureTarget;
    private SimpleFramebuffer resultTarget;
    private SimpleFramebuffer[] mipTargets = new SimpleFramebuffer[0];

    public void render(int i, float f, Runnable runnable) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            this.ensureTargets();
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.captureTarget.getColorAttachment(), 0);
            GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride;
            RenderSystem.outputColorTextureOverride = this.captureTarget.getColorAttachmentView();

            try {
                runnable.run();
            } finally {
                RenderSystem.outputColorTextureOverride = gpuTextureView;
            }

            int i2 = 2 + Math.round(Math.max(0.0F, Math.min(1.0F, f)) * 3.0F);
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

            try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                this.ensureMips(i2);
                FullscreenPass.blitUnflipped(Pipelines.AMBIENCE_BLUR_DOWN, this.mipTargets[0], this.captureTarget.getColorAttachmentView(), gpuSampler, 1.2F);

                for (int i3 = 1; i3 < i2; i3++) {
                    FullscreenPass.blit(Pipelines.AMBIENCE_BLUR_DOWN, this.mipTargets[i3], this.mipTargets[i3 - 1].getColorAttachmentView(), gpuSampler, 1.2F);
                }

                for (int i4 = i2 - 2; i4 >= 0; i4--) {
                    FullscreenPass.blit(Pipelines.AMBIENCE_BLUR_UP, this.mipTargets[i4], this.mipTargets[i4 + 1].getColorAttachmentView(), gpuSampler, 1.2F);
                }

                FullscreenPass.blit(Pipelines.AMBIENCE_BLUR_UP, this.resultTarget, this.mipTargets[0].getColorAttachmentView(), gpuSampler, 1.2F);
            }

            FullscreenPass.compositeUnflipped(Pipelines.GOO_COMPOSITE, framebuffer.getColorAttachmentView(), this.resultTarget.getColorAttachmentView(), gpuSampler, i);
        }
    }

    public void reset() {
        if (this.captureTarget != null) {
            this.captureTarget.delete();
        }

        if (this.resultTarget != null) {
            this.resultTarget.delete();
        }

        for (SimpleFramebuffer simpleFramebuffer : this.mipTargets) {
            if (simpleFramebuffer != null) {
                simpleFramebuffer.delete();
            }
        }

        this.captureTarget = null;
        this.resultTarget = null;
        this.mipTargets = new SimpleFramebuffer[0];
    }

    private void ensureTargets() {
        Window window = MinecraftClient.getInstance().getWindow();
        int i = Math.max(1, window.getFramebufferWidth() / 2);
        int i2 = Math.max(1, window.getFramebufferHeight() / 2);
        this.captureTarget = ensureTarget(this.captureTarget, "sadnes_goo_capture", i, i2);
        this.resultTarget = ensureTarget(this.resultTarget, "sadnes_goo_result", i, i2);
    }

    private void ensureMips(int i) {
        int i2 = this.resultTarget.textureWidth;
        int i3 = this.resultTarget.textureHeight;
        if (this.mipTargets.length < i) {
            this.mipTargets = Arrays.copyOf(this.mipTargets, i);
        }

        for (int i4 = 0; i4 < i; i4++) {
            this.mipTargets[i4] = ensureTarget(this.mipTargets[i4], "sadnes_goo_mip" + i4, Math.max(1, i2 >> i4 + 1), Math.max(1, i3 >> i4 + 1));
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
