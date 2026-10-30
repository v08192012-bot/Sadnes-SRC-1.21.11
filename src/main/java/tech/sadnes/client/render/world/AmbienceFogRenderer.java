package tech.sadnes.client.render.world;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome.Precipitation;
import org.joml.Matrix4fc;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.feature.impl.render.ambience.AmbienceProfile;
import tech.sadnes.client.render.shader.AtmosphereCubeRenderer;
import tech.sadnes.client.render.shader.FogUniformStorage;
import tech.sadnes.client.render.shader.FogUniforms;
import tech.sadnes.client.render.shader.FullscreenPass;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.texture.CloudNoiseTexture;

public enum AmbienceFogRenderer {
    INSTANCE;

    private static final double WRAP_XZ = 65536.0;
    private static final double WRAP_NOISE = 65536.0;
    private static final double WIND_SCROLL_SPEED = 14.0;
    private static final double DRIFT_SPEED = 1.2;
    private static final double VERTICAL_DRIFT_SPEED = 0.45;
    private static final float VOLUME_DISTANCE_SCALE = 2.5F;
    private static final float MIN_VOLUME_DISTANCE = 96.0F;
    private static final float MAX_VOLUME_DISTANCE = 512.0F;
    private static final float VOLUMETRIC_HEIGHT_FOG = 0.03F;
    private static final float FALLOFF_DIVISOR = 3.0F;
    private static final float SKY_LIGHT_SMOOTHING = 1.5F;
    private SimpleFramebuffer volumeFramebuffer;
    private long lastFrameNanos;
    private double scrollX;
    private double scrollZ;
    private double scrollY;
    private float skyLight = 1.0F;
    private FogUniforms uniforms;
    private boolean volumetric;
    private GpuTextureView weatherMapTexture;
    private float fogStart;
    private float fogStrength;

    public boolean isVolumetric(AmbienceProfile ambienceProfile) {
        return ambienceProfile.fogMode.is("volumetric");
    }

    public void update(AmbienceProfile ambienceProfile, ClientWorld clientWorld, Vec3d vec3d, float f, AmbienceWeatherRenderer ambienceWeatherRenderer) {
        long l = System.nanoTime();
        float f2 = this.lastFrameNanos == 0L ? 0.0F : Math.min((float)(l - this.lastFrameNanos) / 1.0E9F, 0.1F);
        this.lastFrameNanos = l;
        this.volumetric = ambienceProfile.fogMode.is("volumetric");
        boolean flag = ambienceProfile.fogMode.is("distance");
        float f3 = clientWorld.getRainGradient(f);
        Precipitation precipitation = Ambience.getPrecipitationAt(clientWorld, vec3d);
        float f4 = precipitation == Precipitation.SNOW ? f3 : 0.0F;
        float f5 = precipitation == Precipitation.RAIN ? clientWorld.getThunderGradient(f) : 0.0F;
        float f6 = precipitation == Precipitation.RAIN ? Math.max(f3 - f5, 0.0F) : 0.0F;
        int i = clientWorld.getLightLevel(LightType.SKY, BlockPos.ofFloored(vec3d));
        this.skyLight = this.skyLight + (i / 15.0F - this.skyLight) * Math.min(f2 * 1.5F, 1.0F);
        float f7 = ambienceProfile.fogThickness.getValue() * (1.0F + 0.8F * f6 + 1.2F * f5 + f4);
        float f8 = flag ? 0.0F : 1.0F / Math.max(f7, 1.0F);
        float f9 = flag ? 0.0F : (this.volumetric ? 0.03F : 0.0F) + 0.1F * f6 + 0.15F * f5 + 0.12F * f4;
        float f10 = f8 / 3.0F;
        float f11 = ambienceProfile.getFogDensityFactor() * (1.0F + 0.9F * f6 + 1.6F * f5 + 1.4F * f4) * MathHelper.lerp(this.skyLight, 0.35F, 1.0F);
        float f12 = ambienceProfile.getFogStartDistance();
        float f13 = flag ? 0.0F : (float)(ambienceProfile.fogHeight.getValue().floatValue() - vec3d.y);
        this.fogStart = f12;
        this.fogStrength = f11 * computeHeightFalloff(-f13, f8, f9, f10);
        float f14 = this.volumetric ? ambienceProfile.fogNoise.getValue() / 100.0F * (1.0F - 0.35F * (f6 + f5)) : 0.0F;
        float f15 = (1.0F - 0.85F * f3) * MathHelper.lerp(this.skyLight, 0.2F, 1.0F);
        float f16 = (1.0F + 0.35F * f4) * MathHelper.lerp(this.skyLight, 0.15F, 1.0F);
        float f17 = ambienceProfile.fogLightShafts.getValue() / 100.0F;
        float f18 = MathHelper.clamp(ambienceProfile.fogDistance.getValue() * 2.5F, 96.0F, 512.0F);
        double d = ambienceProfile.fogSpeed.getValue().floatValue() / 100.0;
        double d2 = ambienceWeatherRenderer.getWindStrength() * 14.0;
        this.scrollX = wrap(this.scrollX + (ambienceWeatherRenderer.getWindX() * d2 + 1.2 * d) * f2, 65536.0);
        this.scrollZ = wrap(this.scrollZ + ambienceWeatherRenderer.getWindZ() * d2 * f2, 65536.0);
        this.scrollY = wrap(this.scrollY + 0.45 * d * (1.0 + ambienceWeatherRenderer.getWindStrength()) * f2, 65536.0);
        this.weatherMapTexture = this.volumetric ? ambienceWeatherRenderer.getWeatherMapTexture() : null;
        float f19 = this.weatherMapTexture != null ? (float)(ambienceWeatherRenderer.getWeatherMapOriginX() - vec3d.x) : 0.0F;
        float f20 = this.weatherMapTexture != null ? (float)(ambienceWeatherRenderer.getWeatherMapOriginZ() - vec3d.z) : 0.0F;
        float f21 = this.weatherMapTexture != null ? ambienceWeatherRenderer.getWeatherMapSize() : 0.0F;
        boolean flag2 = MinecraftClient.getInstance().options.getHideLightningFlashes().getValue();
        AmbienceLightningRenderer.LightningFlash lightningFlash = flag2 ? null : AmbienceLightningRenderer.INSTANCE.getBrightestFlash(vec3d);
        this.uniforms = new FogUniforms(
            f11,
            f12,
            f13,
            f8,
            f9,
            f10,
            f14,
            f18,
            (float)this.scrollX,
            (float)this.scrollY,
            (float)this.scrollZ,
            (float)wrap(vec3d.x, 65536.0),
            (float)vec3d.y,
            (float)wrap(vec3d.z, 65536.0),
            f15,
            f16,
            f17,
            f19,
            f20,
            f21,
            lightningFlash != null ? lightningFlash.x() : 0.0F,
            lightningFlash != null ? lightningFlash.y() : 0.0F,
            lightningFlash != null ? lightningFlash.z() : 0.0F,
            lightningFlash != null ? lightningFlash.power() : 0.0F,
            lightningFlash != null ? lightningFlash.height() : 0.0F,
            ambienceProfile.getFogModeIndex(),
            ambienceProfile.getShaderQualityScale()
        );
    }

    public void render(Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, Matrix4fc matrix4fc) {
        if (this.uniforms != null) {
            GpuTextureView gpuTextureView = CloudNoiseTexture.getNoiseTexture();
            GpuBufferSlice gpuBufferSlice2 = FogUniformStorage.write(this.uniforms);
            GpuTextureView gpuTextureView2 = framebuffer.getDepthAttachmentView();
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
            GpuSampler gpuSampler2 = RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR);
            if (this.volumetric) {
                this.volumeFramebuffer = ensureFramebuffer(this.volumeFramebuffer, Math.max(1, framebuffer.textureWidth / 2), Math.max(1, framebuffer.textureHeight / 2));
            } else {
                this.closeVolumeFramebuffer();
            }

            GpuTextureView gpuTextureView3 = this.volumetric ? this.volumeFramebuffer.getColorAttachmentView() : gpuTextureView;
            GpuTextureView gpuTextureView4 = this.weatherMapTexture != null ? this.weatherMapTexture : gpuTextureView;

            try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                if (this.volumetric) {
                    AtmosphereCubeRenderer.draw(Pipelines.AMBIENCE_FOG_VOLUME, gpuTextureView3, OptionalInt.of(0), gpuBufferSlice, matrix4fc, "fog volume", renderPass -> {
                        renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                        renderPass.bindTexture("Sampler1", gpuTextureView, gpuSampler2);
                        renderPass.bindTexture("Sampler2", gpuTextureView4, gpuSampler);
                        renderPass.setUniform("SadnesFog", gpuBufferSlice2);
                    });
                }

                AtmosphereCubeRenderer.draw(
                    Pipelines.AMBIENCE_FOG_WORLD, framebuffer.getColorAttachmentView(), OptionalInt.empty(), gpuBufferSlice, matrix4fc, "world fog", renderPass2 -> {
                        renderPass2.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                        renderPass2.bindTexture("Sampler1", gpuTextureView3, gpuSampler);
                        renderPass2.setUniform("SadnesFog", gpuBufferSlice2);
                    }
                );
            }
        }
    }

    public float getFogStart() {
        return this.fogStart;
    }

    public float getFogStrength() {
        return this.fogStrength;
    }

    public void reset() {
        this.closeVolumeFramebuffer();
        this.uniforms = null;
        this.weatherMapTexture = null;
        this.lastFrameNanos = 0L;
    }

    private void closeVolumeFramebuffer() {
        if (this.volumeFramebuffer != null) {
            this.volumeFramebuffer.delete();
        }

        this.volumeFramebuffer = null;
    }

    private static float computeHeightFalloff(float f, float f2, float f3, float f4) {
        if (f2 <= 0.0F) {
            return 1.0F;
        } else {
            return f <= 0.0F ? 1.0F + f3 : (float)(Math.exp(-f2 * f) + f3 * Math.exp(-f4 * f));
        }
    }

    private static SimpleFramebuffer ensureFramebuffer(SimpleFramebuffer simpleFramebuffer, int i, int i2) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer("sadnes_ambience_fog_volume", i, i2, false);
        }

        if (simpleFramebuffer.textureWidth != i || simpleFramebuffer.textureHeight != i2) {
            simpleFramebuffer.resize(i, i2);
        }

        return simpleFramebuffer;
    }

    private static double wrap(double d, double d2) {
        return d - Math.floor(d / d2) * d2;
    }
}
