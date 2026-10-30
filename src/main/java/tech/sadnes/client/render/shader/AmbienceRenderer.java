package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.Arrays;
import java.util.OptionalInt;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.feature.impl.render.ambience.AmbienceProfile;
import tech.sadnes.client.render.texture.CloudNoiseTexture;
import tech.sadnes.client.render.world.AmbienceFogRenderer;
import tech.sadnes.client.render.world.AmbienceWeatherRenderer;
import tech.sadnes.client.util.color.ColorUtil;

public enum AmbienceRenderer {
    INSTANCE;

    private static final int BLOOM_MIPS = 4;
    private static final float BLUR_OFFSET = 1.2F;
    private static final double WRAP_SMALL = 16.0;
    private static final double WRAP_MEDIUM = 4096.0;
    private static final double WRAP_LARGE = 65536.0;
    private static final float MAX_RAIN_CLOUD_COVER = 0.68F;
    private static final double CLOUD_SPEED = 0.03;
    private static final long START_TIME = System.nanoTime();
    private static float skyBrightness = 1.0F;
    private static float skyVisibility = 1.0F;
    private static int skyColor = -8408321;
    private static int horizonColor = -4138753;
    private static float sunAngle;
    private SimpleFramebuffer captureTarget;
    private SimpleFramebuffer resultTarget;
    private SimpleFramebuffer[] mipTargets = new SimpleFramebuffer[0];
    private SimpleFramebuffer cloudsTarget;
    private double cloudOffsetX;
    private double cloudOffsetZ;
    private float cloudHeight;
    private int cloudColor = -1;
    private boolean cloudsRendered;
    private final AmbienceRenderer.TextureSnapshot opaqueDepth = new AmbienceRenderer.TextureSnapshot("sadnes_ambience_depth_opaque", TextureFormat.DEPTH32);
    private final AmbienceRenderer.TextureSnapshot worldDepth = new AmbienceRenderer.TextureSnapshot("sadnes_ambience_depth_world", TextureFormat.DEPTH32);
    private final AmbienceRenderer.TextureSnapshot sceneColor = new AmbienceRenderer.TextureSnapshot("sadnes_ambience_scene_color", TextureFormat.RGBA8);
    private boolean opaqueCaptured;
    private boolean reflectionReady;

    public static void setSkyParams(float f, float f2) {
        skyBrightness = f;
        skyVisibility = f2;
    }

    public static boolean isReflectionsEnabled() {
        Ambience ambience = Ambience.getInstance();
        return ambience != null && ambience.isEnabled() && ambience.getActiveProfile().reflections.isEnabled();
    }

    public void captureOpaque(Framebuffer framebuffer) {
        Framebuffer framebuffer2 = MinecraftClient.getInstance().getFramebuffer();
        this.opaqueDepth.copyFrom(framebuffer == null ? null : framebuffer.getDepthAttachment());
        this.sceneColor.copyFrom(framebuffer2 == null ? null : framebuffer2.getColorAttachment());
        if (this.opaqueDepth.getView() != null && this.sceneColor.getView() != null) {
            this.opaqueCaptured = true;
        } else {
            this.opaqueCaptured = false;
        }
    }

    public void captureWorldDepth(Framebuffer framebuffer) {
        if (this.opaqueCaptured) {
            this.worldDepth.copyFrom(framebuffer == null ? null : framebuffer.getDepthAttachment());
            this.opaqueCaptured = false;
            if (this.worldDepth.getView() != null) {
                this.reflectionReady = true;
            } else {
                this.reflectionReady = false;
            }
        }
    }

    public static void setSkyColor(int i, float f) {
        skyColor = i;
        sunAngle = f;
    }

    public static float getSunAngle() {
        return sunAngle;
    }

    public static void setHorizonColor(int i) {
        horizonColor = i;
    }

    public static int getSkyColor() {
        return skyColor;
    }

    public static int getHorizonColor() {
        return horizonColor;
    }

    public void renderClouds(AmbienceProfile ambienceProfile, int i, float f, Vec3d vec3d, long l, float f2) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        GameRenderer gameRenderer2 = minecraftClient.gameRenderer;
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (gameRenderer2.getCamera().getSubmersionType() == CameraSubmersionType.NONE && isFramebufferUsable(framebuffer)) {
            GpuTextureView gpuTextureView = CloudNoiseTexture.getNoiseTexture();
            GpuTextureView gpuTextureView2 = CloudNoiseTexture.getLevelsTexture();
            if (gpuTextureView != null && gpuTextureView2 != null) {
                this.cloudOffsetX = vec3d.x + ((double)l + f2) * 0.03;
                this.cloudOffsetZ = vec3d.z;
                this.cloudHeight = f - (float)vec3d.y;
                this.cloudColor = Ambience.applyWeatherToFogColor(i, minecraftClient.world, vec3d);
                this.cloudsRendered = true;
                Quaternionf quaternionf = gameRenderer2.getCamera().getRotation().conjugate(new Quaternionf());
                GpuBufferSlice gpuBufferSlice = this.createUniforms(gameRenderer2, ambienceProfile, quaternionf);
                Matrix4f matrix4f = new Matrix4f().rotation(quaternionf);
                this.cloudsTarget = ensureTarget(
                    this.cloudsTarget, "sadnes_ambience_clouds", Math.max(1, framebuffer.textureWidth / 2), Math.max(1, framebuffer.textureHeight / 2)
                );
                GpuTextureView gpuTextureView3 = this.cloudsTarget.getColorAttachmentView();
                GpuTextureView gpuTextureView4 = framebuffer.getDepthAttachmentView();
                GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                GpuSampler gpuSampler2 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                GpuSampler gpuSampler3 = RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR);

                try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                    AtmosphereCubeRenderer.draw(Pipelines.AMBIENCE_CLOUDS, gpuTextureView3, OptionalInt.of(0), gpuBufferSlice, matrix4f, "clouds", renderPass -> {
                        renderPass.bindTexture("Sampler0", gpuTextureView4, gpuSampler);
                        renderPass.bindTexture("Sampler1", gpuTextureView, gpuSampler3);
                        renderPass.bindTexture("Sampler2", gpuTextureView2, gpuSampler3);
                    });
                    AtmosphereCubeRenderer.draw(
                        Pipelines.AMBIENCE_CLOUDS_COMPOSITE, framebuffer.getColorAttachmentView(), OptionalInt.empty(), gpuBufferSlice, matrix4f, "clouds composite", renderPass2 -> {
                            renderPass2.bindTexture("Sampler0", gpuTextureView3, gpuSampler2);
                            renderPass2.bindTexture("Sampler1", gpuTextureView4, gpuSampler);
                        }
                    );
                }
            }
        }
    }

    public static boolean isOpenGl() {
        return GlStateSnapshot.isOpenGl();
    }

    public void renderPostEffects(GameRenderer gameRenderer) {
        if (!this.cloudsRendered && this.cloudsTarget != null) {
            deleteTarget(this.cloudsTarget);
            this.cloudsTarget = null;
        }

        this.cloudsRendered = false;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Ambience ambience = Ambience.getInstance();
        if (minecraftClient.player != null && minecraftClient.world != null && ambience != null && ambience.isEnabled()) {
            AmbienceProfile ambienceProfile = ambience.getActiveProfile();
            boolean flag = isOpenGl();
            boolean flag2 = ambienceProfile.reflections.isEnabled() && this.reflectionReady && flag;
            this.reflectionReady = false;
            boolean flag3 = ambienceProfile.distanceBlur.isEnabled() && flag;
            boolean flag4 = ambienceProfile.bloom.isEnabled();
            boolean flag5 = ambienceProfile.colorGrade.isEnabled();
            boolean flag6 = gameRenderer.getCamera().getSubmersionType() == CameraSubmersionType.NONE;
            boolean flag7 = ambienceProfile.isFogEnabled() && flag6 && flag;
            AmbienceWeatherRenderer ambienceWeatherRenderer = AmbienceWeatherRenderer.INSTANCE;
            AmbienceFogRenderer ambienceFogRenderer = AmbienceFogRenderer.INSTANCE;
            Vec3d vec3d = gameRenderer.getCamera().getCameraPos();
            float f = gameRenderer.getCamera().getLastTickProgress();
            boolean flag8 = ambienceWeatherRenderer.update(ambienceProfile, ambience.getPrecipitation(), minecraftClient.world, vec3d, f, flag7 && ambienceFogRenderer.isVolumetric(ambienceProfile));
            if (flag7) {
                ambienceFogRenderer.update(ambienceProfile, minecraftClient.world, vec3d, f, ambienceWeatherRenderer);
            } else {
                ambienceFogRenderer.reset();
            }

            boolean flag9 = flag8 && flag6 && ambienceWeatherRenderer.hasSurfaceEffects();
            if (!flag2 && !flag3 && !flag4 && !flag7 && !flag8 && !flag5) {
                this.releaseIfUnused();
            } else {
                Framebuffer framebuffer = minecraftClient.getFramebuffer();
                if (isFramebufferUsable(framebuffer)) {
                    int i = Math.max(1, framebuffer.textureWidth);
                    int i2 = Math.max(1, framebuffer.textureHeight);
                    if (flag2
                        && (
                            !this.opaqueDepth.matchesSize(i, i2)
                                || !this.worldDepth.matchesSize(i, i2)
                                || !this.sceneColor.matchesSize(i, i2)
                        )) {
                        flag2 = false;
                    }

                    if (flag3 || flag4) {
                        this.resultTarget = ensureTarget(this.resultTarget, "sadnes_ambience_post_result", Math.max(1, i / 2), Math.max(1, i2 / 2));
                    }

                    Quaternionf quaternionf = gameRenderer.getCamera().getRotation().conjugate(new Quaternionf());
                    Matrix4f matrix4f = new Matrix4f().rotation(quaternionf);
                    GpuBufferSlice gpuBufferSlice = this.createUniforms(gameRenderer, ambienceProfile, quaternionf);
                    GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                    GpuSampler gpuSampler2 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
                    if (flag2 || flag3 || flag9 || flag5) {
                        this.captureTarget = ensureTarget(this.captureTarget, "sadnes_ambience_post_capture", i, i2);
                    } else if (this.captureTarget != null) {
                        deleteTarget(this.captureTarget);
                        this.captureTarget = null;
                    }

                    if (flag4) {
                        try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                            this.renderBloom(framebuffer, gpuBufferSlice, gpuSampler);
                        }
                    }

                    if (flag2) {
                        try (FullscreenPass.RenderStateGuard renderStateGuard2 = FullscreenPass.guardState()) {
                            this.renderReflections(framebuffer, gpuBufferSlice, gpuSampler, gpuSampler2);
                        }
                    }

                    if (flag9) {
                        this.captureScene(framebuffer, i, i2, gpuSampler2);
                        ambienceWeatherRenderer.renderSurface(framebuffer, this.captureTarget, gpuBufferSlice, matrix4f);
                    }

                    if (flag7) {
                        ambienceFogRenderer.render(framebuffer, gpuBufferSlice, matrix4f);
                    }

                    if (flag3) {
                        this.captureScene(framebuffer, i, i2, gpuSampler2);

                        try (FullscreenPass.RenderStateGuard renderStateGuard3 = FullscreenPass.guardState()) {
                            this.renderDepthBlur(framebuffer, ambienceProfile.blurStrength.getValue().intValue(), gpuBufferSlice, gpuSampler, gpuSampler2);
                        }
                    }

                    if (flag8) {
                        ambienceWeatherRenderer.renderParticles(minecraftClient.world, framebuffer, quaternionf, flag7 ? ambienceFogRenderer.getFogStart() : 0.0F, flag7 ? ambienceFogRenderer.getFogStrength() : 0.0F);
                    }

                    if (flag5) {
                        this.captureScene(framebuffer, i, i2, gpuSampler2);

                        try (FullscreenPass.RenderStateGuard renderStateGuard4 = FullscreenPass.guardState()) {
                            FullscreenPass.drawAmbiencePass(Pipelines.AMBIENCE_COLOR_GRADE, framebuffer, this.captureTarget.getColorAttachmentView(), gpuSampler2, gpuBufferSlice);
                        }
                    }
                }
            }
        } else {
            this.releaseIfUnused();
        }
    }

    private void captureScene(Framebuffer framebuffer, int i, int i2, GpuSampler gpuSampler) {
        if (GlStateSnapshot.isOpenGl()) {
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToTexture(framebuffer.getColorAttachment(), this.captureTarget.getColorAttachment(), 0, 0, 0, 0, 0, i, i2);
        } else {
            try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                FullscreenPass.copyTexture(this.captureTarget, framebuffer.getColorAttachmentView(), gpuSampler);
            }
        }
    }

    public static boolean isFramebufferUsable(Framebuffer framebuffer) {
        return framebuffer != null
            && framebuffer.getColorAttachment() != null
            && framebuffer.getColorAttachment().getFormat() == TextureFormat.RGBA8
            && framebuffer.getColorAttachmentView() != null
            && !framebuffer.getColorAttachmentView().isClosed()
            && framebuffer.getDepthAttachmentView() != null;
    }

    private GpuBufferSlice createUniforms(GameRenderer gameRenderer, AmbienceProfile ambienceProfile, Quaternionf quaternionf) {
        Vector3f vector3f = quaternionf.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f vector3f2 = quaternionf.transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f vector3f3 = quaternionf.transform(new Vector3f(0.0F, 0.0F, 1.0F));
        Vector3f vector3f4 = quaternionf.transform(new Vector3f(-MathHelper.sin(sunAngle), MathHelper.cos(sunAngle), 0.0F));
        Vec3d vec3d = gameRenderer.getCamera().getCameraPos();
        float f = ambienceProfile.blurDistance.getValue();
        float f2 = ambienceProfile.getFogStartDistance();
        float f3 = ambienceProfile.getFogDensityFactor();
        float f4 = ambienceProfile.fogMode.is("distance") ? 0.0F : (float)(vec3d.y - ambienceProfile.fogHeight.getValue().floatValue());
        float f5 = ambienceProfile.fogMode.is("distance") ? 0.0F : 1.0F / Math.max(ambienceProfile.fogThickness.getValue(), 1.0F);
        ClientWorld clientWorld = MinecraftClient.getInstance().world;
        int i = Ambience.applyWeatherToFogColor(ambienceProfile.fogColor.getValue(), clientWorld, vec3d);
        return AmbiencePostUniformStorage.write(
            new AmbiencePostUniforms(
                skyBrightness,
                skyVisibility,
                0.05F,
                gameRenderer.getFarPlaneDistance(),
                f,
                f * 1.5F,
                ambienceProfile.bloomThreshold.getValue(),
                ambienceProfile.bloomIntensity.getValue(),
                vector3f.x,
                vector3f.y,
                vector3f.z,
                0.5F,
                200.0F,
                ColorUtil.redf(skyColor),
                ColorUtil.greenf(skyColor),
                ColorUtil.bluef(skyColor),
                ColorUtil.redf(horizonColor),
                ColorUtil.greenf(horizonColor),
                ColorUtil.bluef(horizonColor),
                vector3f4.x,
                vector3f4.y,
                vector3f4.z,
                vector3f2.x,
                vector3f2.y,
                vector3f2.z,
                vector3f3.x,
                vector3f3.y,
                vector3f3.z,
                wrap(vec3d.x, 16.0),
                wrap(vec3d.z, 16.0),
                (float)(System.nanoTime() - START_TIME) / 1.0E9F,
                ColorUtil.redf(i),
                ColorUtil.greenf(i),
                ColorUtil.bluef(i),
                f3,
                f2,
                f4,
                f5,
                ambienceProfile.fogNoise.getValue() / 100.0F,
                wrap(vec3d.x, 4096.0),
                (float)vec3d.y,
                wrap(vec3d.z, 4096.0),
                ambienceProfile.getShaderQualityScale(),
                ambienceProfile.getFogModeIndex(),
                ambienceProfile.fogSpeed.getValue() / 100.0F,
                GlStateSnapshot.isOpenGl() ? 0.0F : 1.0F,
                wrap(this.cloudOffsetX, 65536.0),
                wrap(this.cloudOffsetZ, 65536.0),
                this.cloudHeight,
                96.0F,
                MathHelper.lerp(getCloudCover(ambienceProfile, clientWorld), 0.75F, 0.1F),
                0.06F,
                ambienceProfile.cloudDistance.getValue(),
                28.0F,
                ColorUtil.redf(this.cloudColor),
                ColorUtil.greenf(this.cloudColor),
                ColorUtil.bluef(this.cloudColor),
                ColorUtil.alphaf(this.cloudColor),
                getFogStrength(ambienceProfile),
                ambienceProfile.saturation.getValue() / 100.0F,
                ambienceProfile.contrast.getValue() / 100.0F,
                ambienceProfile.brightness.getValue() / 100.0F,
                ambienceProfile.temperature.getValue() / 100.0F
            )
        );
    }

    private void renderReflections(Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, GpuSampler gpuSampler, GpuSampler gpuSampler2) {
        FullscreenPass.drawAmbienceReflection(
            Pipelines.AMBIENCE_REFLECTION,
            this.captureTarget,
            this.sceneColor.getView(),
            this.worldDepth.getView(),
            this.opaqueDepth.getView(),
            gpuSampler,
            gpuSampler2,
            gpuBufferSlice
        );
        FullscreenPass.drawAmbiencePass(Pipelines.AMBIENCE_REFLECTION_COMPOSITE, framebuffer, this.captureTarget.getColorAttachmentView(), gpuSampler, gpuBufferSlice);
    }

    private static float getFogStrength(AmbienceProfile ambienceProfile) {
        return ambienceProfile.isSkyEnabled() && !ambienceProfile.skyShader.is("off") ? ambienceProfile.skyShaderOpacity.getValue() / 100.0F : 0.0F;
    }

    private static float getCloudCover(AmbienceProfile ambienceProfile, World world) {
        float f = ambienceProfile.cloudCoverage.getValue() / 100.0F;
        float f2 = world == null ? 0.0F : world.getRainGradient(1.0F);
        return MathHelper.lerp(f2 * 0.7F, f, Math.max(f, 0.68F));
    }

    private static float wrap(double d, double d2) {
        return (float)(d - Math.floor(d / d2) * d2);
    }

    private void renderDepthBlur(Framebuffer framebuffer, int i, GpuBufferSlice gpuBufferSlice, GpuSampler gpuSampler, GpuSampler gpuSampler2) {
        this.ensureMipTargets(i);
        FullscreenPass.drawAmbienceDepthPass(
            Pipelines.AMBIENCE_MASKED_DOWN,
            this.mipTargets[0],
            this.captureTarget.getColorAttachmentView(),
            framebuffer.getDepthAttachmentView(),
            gpuSampler,
            gpuSampler2,
            gpuBufferSlice,
            2.4F
        );

        for (int i2 = 1; i2 < i; i2++) {
            FullscreenPass.blitUnflipped(Pipelines.AMBIENCE_BLUR_DOWN, this.mipTargets[i2], this.mipTargets[i2 - 1].getColorAttachmentView(), gpuSampler, 1.2F);
        }

        for (int i3 = i - 2; i3 >= 0; i3--) {
            FullscreenPass.blitUnflipped(Pipelines.AMBIENCE_BLUR_UP, this.mipTargets[i3], this.mipTargets[i3 + 1].getColorAttachmentView(), gpuSampler, 1.2F);
        }

        FullscreenPass.blitUnflipped(Pipelines.AMBIENCE_BLUR_UP, this.resultTarget, this.mipTargets[0].getColorAttachmentView(), gpuSampler, 1.2F);
        FullscreenPass.drawAmbienceDepthBlur(
            Pipelines.AMBIENCE_DEPTH_BLUR,
            framebuffer,
            this.resultTarget.getColorAttachmentView(),
            this.captureTarget.getColorAttachmentView(),
            framebuffer.getDepthAttachmentView(),
            gpuSampler,
            gpuSampler2,
            gpuBufferSlice
        );
    }

    private void renderBloom(Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, GpuSampler gpuSampler) {
        this.ensureMipTargets(4);
        FullscreenPass.drawAmbiencePass(Pipelines.AMBIENCE_BLOOM_EXTRACT, this.resultTarget, framebuffer.getColorAttachmentView(), gpuSampler, gpuBufferSlice);

        for (int i = 0; i < 4; i++) {
            FullscreenPass.blitUnflipped(
                Pipelines.AMBIENCE_BLUR_DOWN,
                this.mipTargets[i],
                (i == 0 ? this.resultTarget : this.mipTargets[i - 1]).getColorAttachmentView(),
                gpuSampler,
                1.2F
            );
        }

        for (int i2 = 2; i2 >= 0; i2--) {
            FullscreenPass.blitUnflipped(Pipelines.AMBIENCE_BLUR_UP_ADD, this.mipTargets[i2], this.mipTargets[i2 + 1].getColorAttachmentView(), gpuSampler, 1.2F);
        }

        FullscreenPass.blitUnflipped(Pipelines.AMBIENCE_BLUR_UP, this.resultTarget, this.mipTargets[0].getColorAttachmentView(), gpuSampler, 1.2F);
        FullscreenPass.drawAmbiencePass(Pipelines.AMBIENCE_BLOOM_COMPOSITE, framebuffer, this.resultTarget.getColorAttachmentView(), gpuSampler, gpuBufferSlice);
    }

    private void ensureMipTargets(int i) {
        int i2 = this.resultTarget.textureWidth;
        int i3 = this.resultTarget.textureHeight;
        if (this.mipTargets.length < i) {
            this.mipTargets = Arrays.copyOf(this.mipTargets, i);
        }

        for (int i4 = 0; i4 < i; i4++) {
            this.mipTargets[i4] = ensureTarget(
                this.mipTargets[i4], "sadnes_ambience_post_mip" + i4, Math.max(1, i2 >> i4 + 1), Math.max(1, i3 >> i4 + 1)
            );
        }
    }

    public void reset() {
        this.releaseTargets();
        deleteTarget(this.cloudsTarget);
        this.cloudsTarget = null;
        AtmosphereCubeRenderer.close();
        AmbienceWeatherRenderer.INSTANCE.reset();
        AmbienceFogRenderer.INSTANCE.reset();
    }

    private void releaseIfUnused() {
        if (this.captureTarget != null || this.resultTarget != null || this.mipTargets.length > 0 || this.opaqueDepth.getView() != null) {
            this.releaseTargets();
        }

        if (this.cloudsTarget == null) {
            AtmosphereCubeRenderer.close();
        }

        AmbienceWeatherRenderer.INSTANCE.reset();
        AmbienceFogRenderer.INSTANCE.reset();
    }

    private void releaseTargets() {
        deleteTarget(this.captureTarget);
        this.captureTarget = null;
        deleteTarget(this.resultTarget);
        this.resultTarget = null;
        SimpleFramebuffer[] simpleFramebuffers = this.mipTargets;
        int i = simpleFramebuffers.length;

        for (int i2 = 0; i2 < i; i2++) {
            deleteTarget(simpleFramebuffers[i2]);
        }

        this.mipTargets = new SimpleFramebuffer[0];
        this.opaqueDepth.close();
        this.worldDepth.close();
        this.sceneColor.close();
        this.opaqueCaptured = false;
        this.reflectionReady = false;
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

    private static void deleteTarget(SimpleFramebuffer simpleFramebuffer) {
        if (simpleFramebuffer != null) {
            simpleFramebuffer.delete();
        }
    }

    static final class TextureSnapshot {
        private final String label;
        private final TextureFormat format;
        private GpuTexture texture;
        private GpuTextureView textureView;

        TextureSnapshot(String string, TextureFormat textureFormat) {
            this.label = string;
            this.format = textureFormat;
        }

        GpuTextureView getView() {
            return this.textureView;
        }

        boolean matchesSize(int i, int i2) {
            return this.texture != null && this.texture.getWidth(0) == i && this.texture.getHeight(0) == i2;
        }

        void copyFrom(GpuTexture gpuTexture) {
            if (gpuTexture != null && gpuTexture.getFormat() == this.format) {
                int i = gpuTexture.getWidth(0);
                int i2 = gpuTexture.getHeight(0);
                if (this.texture == null || this.texture.getWidth(0) != i || this.texture.getHeight(0) != i2) {
                    this.close();
                    GpuDevice gpuDevice = RenderSystem.getDevice();
                    this.texture = gpuDevice.createTexture(this.label, 15, this.format, i, i2, 1, 1);
                    this.textureView = gpuDevice.createTextureView(this.texture);
                }

                RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(gpuTexture, this.texture, 0, 0, 0, 0, 0, i, i2);
            }
        }

        void close() {
            if (this.textureView != null) {
                this.textureView.close();
            }

            if (this.texture != null) {
                this.texture.close();
            }

            this.textureView = null;
            this.texture = null;
        }
    }
}
