package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.render.EntityMaskRenderer;
import tech.sadnes.client.render.HandCaptureRenderer;
import tech.sadnes.client.util.color.ColorUtil;

public enum GlowRenderer {
    INSTANCE;

    private static final int DOWNSCALE = 2;
    private static final long START_TIME = System.nanoTime();
    private SimpleFramebuffer captureTarget;
    private GpuTextureView maskView;
    private final PingPongBlurTarget blur = new PingPongBlurTarget("sadnes_glow_blur");
    private SimpleFramebuffer flameTargetA;
    private SimpleFramebuffer flameTargetB;
    private long lastFlameTime;
    private float lastYaw;
    private float lastPitch;
    private float yawVelocity;
    private float pitchVelocity;
    private boolean hasLastRotation;
    private float motionAmount;
    private float motionVelocity;
    private long lastGhostTime;
    private static final int SWING_HISTORY_SIZE = 160;
    private final float[] swingHistory = new float[160];
    private final long[] swingTimes = new long[160];
    private int swingIndex = -1;
    private SimpleFramebuffer ghostTarget;
    private SimpleFramebuffer scriptSharpTarget;
    private SimpleFramebuffer scriptSceneTarget;

    public void renderEntityGlow(MatrixStack matrixStack, Camera camera, float f, List<Entity> entities, int i, float f2) {
        if (!entities.isEmpty() && i >>> 24 != 0) {
            this.prepareCapture();
            EntityMaskRenderer.renderEntities(this.captureTarget, matrixStack, camera, f, entities);
            this.maskView = this.captureTarget.getColorAttachmentView();
            this.renderGlow(i, f2);
        }
    }

    public void renderScriptGlow(MatrixStack matrixStack, Camera camera, float f, List<Entity> entities, int i, float f2, ScriptShader scriptShader, ScriptFxUniforms scriptFxUniforms) {
        if (!entities.isEmpty() && scriptShader != null) {
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            if (framebuffer != null) {
                this.prepareCapture();
                EntityMaskRenderer.renderEntities(this.captureTarget, matrixStack, camera, f, entities);
                this.maskView = this.captureTarget.getColorAttachmentView();
                this.blurMask(Pipelines.GLOW_MASK, f2);
                GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                GpuTextureView gpuTextureView = this.maskView;
                if (!GlStateSnapshot.isOpenGl()) {
                    this.scriptSharpTarget = ensureTarget(
                        this.scriptSharpTarget, "sadnes_script_glow_sharp", this.captureTarget.textureWidth, this.captureTarget.textureHeight, false
                    );
                    FullscreenPass.compositeUnflipped(Pipelines.COPY, this.scriptSharpTarget.getColorAttachmentView(), this.maskView, gpuSampler, ColorUtil.WHITE);
                    gpuTextureView = this.scriptSharpTarget.getColorAttachmentView();
                }

                GpuTextureView gpuTextureView2 = this.blur.getColorView();
                if (scriptShader.needsScene()) {
                    this.scriptSceneTarget = ensureTarget(this.scriptSceneTarget, "sadnes_script_glow_scene", framebuffer.textureWidth, framebuffer.textureHeight, false);
                    FullscreenPass.compositeUnflipped(
                        Pipelines.COPY, this.scriptSceneTarget.getColorAttachmentView(), framebuffer.getColorAttachmentView(), gpuSampler, ColorUtil.WHITE
                    );
                    gpuTextureView2 = this.scriptSceneTarget.getColorAttachmentView();
                }

                FullscreenPass.drawScriptGlow(
                    scriptShader.pipeline(), framebuffer.getColorAttachmentView(), this.blur.getColorView(), gpuTextureView, gpuTextureView2, gpuSampler, i, ScriptFxUniformStorage.write(scriptFxUniforms)
                );
            }
        }
    }

    public void renderHandGlow(MatrixStack matrixStack, float f, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i, int i2, float f2) {
        if (clientPlayerEntity != null && i2 >>> 24 != 0) {
            this.prepareCapture();
            HandCaptureRenderer.renderHands(this.captureTarget, matrixStack, f, heldItemRenderer, clientPlayerEntity, i);
            this.maskView = this.captureTarget.getColorAttachmentView();
            this.renderGlow(i2, f2);
        }
    }

    public void renderHandFlame(
        MatrixStack matrixStack,
        float f,
        HeldItemRenderer heldItemRenderer,
        ClientPlayerEntity clientPlayerEntity,
        int i,
        int i2,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        boolean flag
    ) {
        if (clientPlayerEntity != null && i2 >>> 24 != 0) {
            this.prepareCapture();
            this.prepareFlameTargets();
            HandCaptureRenderer.renderHands(this.captureTarget, matrixStack, f, heldItemRenderer, clientPlayerEntity, i);
            this.maskView = this.captureTarget.getColorAttachmentView();
            this.blurMask(Pipelines.FLAME_MASK, f2);
            long l = System.nanoTime();
            float f7 = this.lastFlameTime == 0L ? 0.0F : (float)(l - this.lastFlameTime) / 1.0E9F;
            boolean flag2 = f7 > 0.2F;
            if (flag2) {
                this.clearFlameTargets();
                f7 = 0.0F;
            }

            float f8 = Math.min(f7, 0.1F);
            this.lastFlameTime = l;
            float f9 = MathHelper.lerp(f6, 12.0F, 2.0F);
            float f10 = (float)Math.exp(-f9 * f8);
            float f11 = MathHelper.lerp(f6, 8.0E-4F, 0.003F);
            float f12 = MathHelper.lerp(f6, 0.006F, 0.015F);
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            float f13 = camera.getYaw();
            float f14 = camera.getPitch();
            if (flag2) {
                this.yawVelocity = 0.0F;
                this.pitchVelocity = 0.0F;
            }

            float f15 = 0.0F;
            float f16 = 0.0F;
            if (this.hasLastRotation && !flag2 && f8 > 1.0E-4F) {
                float f17 = MathHelper.wrapDegrees(f13 - this.lastYaw) / f8;
                float f18 = MathHelper.wrapDegrees(f14 - this.lastPitch) / f8;
                float f19 = 1.0F - (float)Math.exp(-f8 / 0.06F);
                this.yawVelocity = MathHelper.lerp(f19, this.yawVelocity, f17);
                this.pitchVelocity = MathHelper.lerp(f19, this.pitchVelocity, f18);
                f15 = MathHelper.clamp(this.yawVelocity * f11 * f8, -f12, f12);
                f16 = MathHelper.clamp(-this.pitchVelocity * f11 * f8, -f12, f12);
            }

            this.lastYaw = f13;
            this.lastPitch = f14;
            this.hasLastRotation = true;
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            FullscreenPass.drawFlameAdvect(
                Pipelines.FLAME_ADVECT, this.flameTargetB, this.flameTargetA.getColorAttachmentView(), this.blur.getColorView(), gpuSampler, f10, f15, f16
            );
            SimpleFramebuffer simpleFramebuffer = this.flameTargetB;
            this.flameTargetB = this.flameTargetA;
            this.flameTargetA = simpleFramebuffer;
            float f20 = (float)(l - START_TIME) / 1.0E9F;
            this.compositeFlame(simpleFramebuffer, i2, f20, f3, f4, f5, flag);
        }
    }

    public void renderHandRim(MatrixStack matrixStack, float f, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i, int i2, float f2) {
        if (clientPlayerEntity != null && i2 >>> 24 != 0) {
            this.prepareCapture();
            HandCaptureRenderer.renderHands(this.captureTarget, matrixStack, f, heldItemRenderer, clientPlayerEntity, i);
            this.maskView = this.captureTarget.getColorAttachmentView();
            this.blurMask(Pipelines.GLOW_MASK, f2);
            float f3 = (float)(System.nanoTime() - START_TIME) / 1.0E9F;
            this.compositeRim(this.blur.getFramebuffer(), i2, f3, 0.0F, 0.0F);
        }
    }

    public void renderHandSweep(
        MatrixStack matrixStack,
        float f,
        HeldItemRenderer heldItemRenderer,
        ClientPlayerEntity clientPlayerEntity,
        int i,
        Hand hand,
        float f2,
        int i2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        float f10,
        float f11,
        float f12,
        float f13,
        float f14
    ) {
        if (clientPlayerEntity != null && hand != null && !(f10 <= 0.001F)) {
            if (!(f13 <= f11) && !(f14 <= f12)) {
                this.prepareCapture();
                HandCaptureRenderer.renderHandsWith(this.captureTarget, matrixStack, f, heldItemRenderer, clientPlayerEntity, i, hand, -1.0F);
                this.maskView = this.captureTarget.getColorAttachmentView();
                this.blurMask(Pipelines.GLOW_MASK, f2);
                Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                if (framebuffer != null) {
                    GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                    FullscreenPass.drawHandSweep(
                        Pipelines.HAND_SWEEP,
                        framebuffer.getColorAttachmentView(),
                        this.blur.getFramebuffer().getColorAttachmentView(),
                        this.maskView,
                        gpuSampler,
                        i2,
                        f3,
                        f4,
                        f5,
                        f6,
                        f7,
                        f8,
                        f9,
                        f10,
                        f11,
                        f12,
                        f13,
                        f14
                    );
                }
            }
        }
    }

    public void renderHandGhost(
        MatrixStack matrixStack, float f, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i, int i2, float f2, boolean flag, boolean flag2, float f3
    ) {
        if (clientPlayerEntity != null && i2 >>> 24 != 0) {
            long l = System.nanoTime();
            float f4 = (float)(l - START_TIME) / 1.0E9F;
            this.prepareCapture();
            HandCaptureRenderer.renderHandsMainOnly(this.captureTarget, matrixStack, f, heldItemRenderer, clientPlayerEntity, i, true, -1.0F);
            this.maskView = this.captureTarget.getColorAttachmentView();
            this.blurMask(Pipelines.GLOW_MASK, f2);
            this.compositeRim(this.blur.getFramebuffer(), i2, f4, 0.0F, 0.0F);
            float f5 = 0.0F;
            if (flag) {
                float f6 = this.lastGhostTime == 0L ? 0.0F : (float)(l - this.lastGhostTime) / 1.0E9F;
                float f7 = Math.min(f6, 0.1F);
                this.lastGhostTime = l;
                float f8 = (float)clientPlayerEntity.getVelocity().horizontalLength();
                float f9 = Math.min(f8 / 0.13F, 1.0F);
                this.motionVelocity = this.motionVelocity + (f9 - this.motionAmount) * 24.0F * f7;
                this.motionVelocity = this.motionVelocity - this.motionVelocity * 7.0F * f7;
                this.motionAmount = this.motionAmount + this.motionVelocity * f7;
                f5 = MathHelper.clamp(this.motionAmount, 0.0F, 1.5F);
            }

            float f10 = 0.0F;
            if (flag2) {
                this.recordSwing(l, clientPlayerEntity.getHandSwingProgress(f));
                f10 = this.getSwingAt(l - (long)(f3 * 1.0E9F));
            }

            float f11 = Math.min(f5, 1.0F);
            float f12 = MathHelper.sin(MathHelper.clamp(f10, 0.0F, 1.0F) * (float) Math.PI);
            float f13 = Math.max(f11, f12);
            if (!(f13 < 0.02F)) {
                this.prepareGhostTarget();
                HandCaptureRenderer.renderHandsMainOnly(this.ghostTarget, matrixStack, f, heldItemRenderer, clientPlayerEntity, i, true, flag2 ? f10 : -1.0F);
                this.maskView = this.ghostTarget.getColorAttachmentView();
                this.blurMask(Pipelines.GLOW_MASK, f2);
                int i3 = f12 > 0.0F ? ColorUtil.interpolate(i2, ColorUtil.WHITE, f12 * f12 * 0.65F) : i2;
                this.compositeRimOffset(this.blur.getFramebuffer(), ColorUtil.multiplyAlpha(i3, f13), f4, -f5 * 0.012F, f5 * 0.032F);
            }
        }
    }

    private void prepareCapture() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window window = minecraftClient.getWindow();
        int i = Math.max(1, window.getFramebufferWidth());
        int i2 = Math.max(1, window.getFramebufferHeight());
        this.captureTarget = ensureTarget(this.captureTarget, "sadnes_glow_capture", i, i2, true);
        this.blur.resize(i / 2, i2 / 2);
    }

    private void recordSwing(long l, float f) {
        this.swingIndex = (this.swingIndex + 1) % 160;
        this.swingHistory[this.swingIndex] = f;
        this.swingTimes[this.swingIndex] = l;
    }

    private float getSwingAt(long l) {
        if (this.swingIndex < 0) {
            return 0.0F;
        }

        float f = this.swingHistory[this.swingIndex];

        for (int i = 0; i < 160; i++) {
            int i2 = ((this.swingIndex - i) % 160 + 160) % 160;
            if (this.swingTimes[i2] == 0L) {
                return f;
            }

            f = this.swingHistory[i2];
            if (this.swingTimes[i2] <= l) {
                return f;
            }
        }

        return f;
    }

    private void prepareGhostTarget() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window window = minecraftClient.getWindow();
        int i = Math.max(1, window.getFramebufferWidth());
        int i2 = Math.max(1, window.getFramebufferHeight());
        this.ghostTarget = ensureTarget(this.ghostTarget, "sadnes_glow_ghost", i, i2, true);
    }

    private void renderGlow(int i, float f) {
        this.blurMask(Pipelines.GLOW_MASK, f);
        this.compositeGlow(this.blur.getFramebuffer(), i);
    }

    private void blurMask(RenderPipeline renderPipeline, float f) {
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        float f2 = f / 2.0F;

        try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
            FullscreenPass.blitUnflipped(renderPipeline, this.blur.getFramebuffer(), this.maskView, gpuSampler, f2);
            this.blur.blur(this.blur.getColorView(), f2, f2, gpuSampler);
        }
    }

    private void prepareFlameTargets() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window window = minecraftClient.getWindow();
        int i = Math.max(1, window.getFramebufferWidth() / 2);
        int i2 = Math.max(1, window.getFramebufferHeight() / 2);
        this.flameTargetA = ensureClearedTarget(this.flameTargetA, "sadnes_flame_a", i, i2);
        this.flameTargetB = ensureClearedTarget(this.flameTargetB, "sadnes_flame_b", i, i2);
    }

    private static SimpleFramebuffer ensureClearedTarget(SimpleFramebuffer simpleFramebuffer, String string, int i, int i2) {
        boolean flag = false;
        if (simpleFramebuffer == null) {
            simpleFramebuffer = new SimpleFramebuffer(string, i, i2, false);
            flag = true;
        } else if (simpleFramebuffer.textureWidth != i || simpleFramebuffer.textureHeight != i2) {
            simpleFramebuffer.resize(i, i2);
            flag = true;
        }

        if (flag) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(simpleFramebuffer.getColorAttachment(), 0);
        }

        return simpleFramebuffer;
    }

    private void clearFlameTargets() {
        if (this.flameTargetA != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.flameTargetA.getColorAttachment(), 0);
        }

        if (this.flameTargetB != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.flameTargetB.getColorAttachment(), 0);
        }
    }

    private void compositeFlame(SimpleFramebuffer simpleFramebuffer, int i, float f, float f2, float f3, float f4, boolean flag) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (framebuffer != null) {
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            RenderPipeline renderPipeline = Pipelines.FLAME_COMPOSITE;
            GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
            GpuTextureView gpuTextureView2 = simpleFramebuffer.getColorAttachmentView();
            if (flag) {
                FullscreenPass.drawFlameComposite(renderPipeline, gpuTextureView, gpuTextureView2, gpuSampler, i, f, f2, f3, f4, 1.0F);
            } else {
                FullscreenPass.drawFlameComposite(renderPipeline, gpuTextureView, gpuTextureView2, gpuSampler, i, f, f2, f3, f4, 0.0F);
            }
        }
    }

    private void compositeGlow(SimpleFramebuffer simpleFramebuffer, int i) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (framebuffer != null) {
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            FullscreenPass.drawGlowComposite(Pipelines.GLOW_COMPOSITE, framebuffer.getColorAttachmentView(), simpleFramebuffer.getColorAttachmentView(), this.maskView, gpuSampler, i);
        }
    }

    private void compositeRim(SimpleFramebuffer simpleFramebuffer, int i, float f, float f2, float f3) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (framebuffer != null) {
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            FullscreenPass.drawRimComposite(
                Pipelines.GLOW_RIM_COMPOSITE,
                framebuffer.getColorAttachmentView(),
                simpleFramebuffer.getColorAttachmentView(),
                this.maskView,
                gpuSampler,
                i,
                f,
                1.0F,
                f2,
                f3,
                0.0F,
                0.0F
            );
        }
    }

    private void compositeRimOffset(SimpleFramebuffer simpleFramebuffer, int i, float f, float f2, float f3) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (framebuffer != null) {
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            FullscreenPass.drawRimComposite(
                Pipelines.GLOW_RIM_COMPOSITE,
                framebuffer.getColorAttachmentView(),
                simpleFramebuffer.getColorAttachmentView(),
                this.maskView,
                gpuSampler,
                i,
                f,
                1.0F,
                0.0F,
                0.0F,
                f2,
                f3
            );
        }
    }

    public void reset() {
        deleteTarget(this.captureTarget);
        deleteTarget(this.ghostTarget);
        this.ghostTarget = null;
        deleteTarget(this.scriptSharpTarget);
        deleteTarget(this.scriptSceneTarget);
        this.scriptSharpTarget = null;
        this.scriptSceneTarget = null;
        this.blur.close();
        deleteTarget(this.flameTargetA);
        deleteTarget(this.flameTargetB);
        this.captureTarget = null;
        this.flameTargetA = null;
        this.flameTargetB = null;
        this.lastFlameTime = 0L;
        this.hasLastRotation = false;
        this.yawVelocity = 0.0F;
        this.pitchVelocity = 0.0F;
    }

    private static SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, String string, int i, int i2, boolean flag) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, i, i2, flag);
        }

        if (simpleFramebuffer.textureWidth != i || simpleFramebuffer.textureHeight != i2) {
            simpleFramebuffer.resize(i, i2);
        }

        return simpleFramebuffer;
    }

    private static void deleteTarget(SimpleFramebuffer simpleFramebuffer) {
        if (simpleFramebuffer != null) {
            simpleFramebuffer.delete();
        }
    }
}
