package tech.sadnes.client.render.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome.Precipitation;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tech.sadnes.client.feature.impl.render.ambience.AmbienceProfile;
import tech.sadnes.client.render.WindStreakRenderer;
import tech.sadnes.client.render.shader.AmbienceRenderer;
import tech.sadnes.client.render.shader.AtmosphereCubeRenderer;
import tech.sadnes.client.render.shader.FullscreenPass;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.shader.WeatherUniformStorage;
import tech.sadnes.client.render.shader.WeatherUniforms;
import tech.sadnes.client.util.color.ColorUtil;

public enum AmbienceWeatherRenderer {
    INSTANCE;

    private static final int MAX_PARTICLES = 32768;
    private static final double TIME_WRAP = 1024.0;
    private static final double OFFSET_WRAP = 192.0;
    private static final AmbienceWeatherRenderer.ParticleLayer[] RAIN_LAYERS = new AmbienceWeatherRenderer.ParticleLayer[]{
        new AmbienceWeatherRenderer.ParticleLayer(12.0F, 0.0F, 2000, 0.012F, 0.45F),
        new AmbienceWeatherRenderer.ParticleLayer(32.0F, 5.0F, 10000, 0.014F, 0.38F),
        new AmbienceWeatherRenderer.ParticleLayer(64.0F, 14.0F, 18000, 0.02F, 0.32F)
    };
    private static final AmbienceWeatherRenderer.ParticleLayer[] SNOW_LAYERS = new AmbienceWeatherRenderer.ParticleLayer[]{
        new AmbienceWeatherRenderer.ParticleLayer(16.0F, 0.0F, 2400, 0.05F, 0.95F), new AmbienceWeatherRenderer.ParticleLayer(48.0F, 7.0F, 12000, 0.04F, 0.85F)
    };
    private static final int MAX_BATCHES = 5;
    private static final float[] RAIN_FALL_SPEEDS = new float[]{12.5F, 14.5F, 16.5F};
    private static final float[] SNOW_FALL_SPEEDS = new float[]{1.1F, 1.6F, 2.2F};
    private static final float RAIN_STRETCH = 0.75F;
    private static final float SNOW_SWAY = 0.35F;
    private static final float RAIN_WIND_FACTOR = 10.0F;
    private static final float SNOW_WIND_FACTOR = 5.0F;
    private static final float[] QUALITY_MULTIPLIERS = new float[]{0.5F, 1.0F, 1.25F, 1.5F};
    private static final Vector4f WHITE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f ZERO_OFFSET = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();
    private final WeatherMap weatherMap = new WeatherMap();
    private final WindStreakRenderer windRenderer = new WindStreakRenderer();
    private final double[] rainOffsets = new double[9];
    private final double[] snowOffsets = new double[9];
    private final RenderPipeline[] batchPipelines = new RenderPipeline[5];
    private final GpuBufferSlice[] batchUniforms = new GpuBufferSlice[5];
    private final int[] batchCounts = new int[5];
    private GpuBuffer particleBuffer;
    private long lastFrameNanos;
    private double elapsedSeconds;
    private Vec3d cameraPos = Vec3d.ZERO;
    private float windX;
    private float windZ;
    private float windStrength;
    private float windOpacity;
    private boolean raining;
    private boolean snowing;
    private float density;
    private float rainSurfaceIntensity;
    private float snowSurfaceIntensity;
    private int quality;
    private float ambientRed;
    private float ambientGreen;
    private float ambientBlue;
    private float lightningFlash;
    private float cameraParamA;
    private float cameraParamB;

    public boolean update(AmbienceProfile ambienceProfile, Precipitation precipitation, ClientWorld clientWorld, Vec3d vec3d, float f, boolean flag) {
        long l = System.nanoTime();
        float f2 = this.lastFrameNanos == 0L ? 0.0F : Math.min((float)(l - this.lastFrameNanos) / 1.0E9F, 0.1F);
        this.lastFrameNanos = l;
        this.elapsedSeconds += f2;
        this.cameraPos = vec3d;
        this.updateWind(ambienceProfile);
        this.advanceOffsets(this.rainOffsets, RAIN_FALL_SPEEDS, 10.0F, f2);
        this.advanceOffsets(this.snowOffsets, SNOW_FALL_SPEEDS, 5.0F, f2);
        boolean flag2 = ambienceProfile.isWeatherEnabled();
        if (!flag2) {
            this.closeParticleBuffer();
        }

        float f3 = flag2 ? clientWorld.getRainGradient(f) : 0.0F;
        if (f3 > 0.0F || flag) {
            this.weatherMap.update(clientWorld, vec3d, precipitation);
        }

        this.raining = f3 > 0.0F && this.weatherMap.hasRain();
        this.snowing = f3 > 0.0F && this.weatherMap.hasSnow();
        this.density = f3 * ambienceProfile.precipitationDensity.getValue() / 100.0F;
        this.rainSurfaceIntensity = ambienceProfile.rainRipples.getValue() ? f3 * (0.4F + 0.6F * ambienceProfile.precipitationDensity.getValue() / 100.0F) : 0.0F;
        this.snowSurfaceIntensity = f3 * ambienceProfile.snowCover.getValue() / 100.0F;
        this.windOpacity = ambienceProfile.isWindEnabled() ? ambienceProfile.windStreaks.getValue() / 100.0F : 0.0F;
        this.quality = ambienceProfile.getShaderQualityScale();
        this.updateAmbientColor();
        boolean flag3 = MinecraftClient.getInstance().options.getHideLightningFlashes().getValue();
        this.lightningFlash = flag2 && !flag3 ? AmbienceLightningRenderer.INSTANCE.getFlashIntensity(vec3d) : 0.0F;
        boolean flag4 = this.windOpacity > 0.0F && this.windStrength > 0.0F;
        return this.raining || this.snowing || flag4 || flag2 && AmbienceLightningRenderer.INSTANCE.hasBolts();
    }

    public boolean hasSurfaceEffects() {
        return this.raining && this.rainSurfaceIntensity > 0.0F || this.snowing && this.snowSurfaceIntensity > 0.0F;
    }

    public void renderSurface(Framebuffer framebuffer, Framebuffer framebuffer2, GpuBufferSlice gpuBufferSlice, Matrix4fc matrix4fc) {
        GpuTextureView gpuTextureView = this.weatherMap.getTextureView();
        if (gpuTextureView != null) {
            GpuBufferSlice gpuBufferSlice2 = this.createUniforms(0.0F, 0.0F, this.rainOffsets, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            GpuTextureView gpuTextureView2 = framebuffer.getDepthAttachmentView();
            GpuTextureView gpuTextureView3 = framebuffer2.getColorAttachmentView();
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
            GpuSampler gpuSampler2 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

            try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                AtmosphereCubeRenderer.draw(
                    Pipelines.WEATHER_SURFACE, framebuffer.getColorAttachmentView(), OptionalInt.empty(), gpuBufferSlice, matrix4fc, "weather surface", renderPass -> {
                        renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                        renderPass.bindTexture("Sampler1", gpuTextureView, gpuSampler);
                        renderPass.bindTexture("Sampler2", gpuTextureView3, gpuSampler2);
                        renderPass.setUniform("SadnesWeather", gpuBufferSlice2);
                    }
                );
            }
        }
    }

    public void renderParticles(ClientWorld clientWorld, Framebuffer framebuffer, Quaternionf quaternionf, float f, float f2) {
        this.cameraParamA = f;
        this.cameraParamB = f2;
        Matrix4f matrix4f = new Matrix4f().rotation(quaternionf);
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.set(matrix4f);

        try {
            AmbienceLightningRenderer.INSTANCE.render(this.cameraPos, f, f2);
            if (this.windOpacity > 0.0F && this.windStrength > 0.0F) {
                int i = ColorUtil.rgbaf(
                    this.ambientRed + (1.0F - this.ambientRed) * 0.4F,
                    this.ambientGreen + (1.0F - this.ambientGreen) * 0.4F,
                    this.ambientBlue + (1.0F - this.ambientBlue) * 0.4F,
                    1.0F
                );
                this.windRenderer.render(clientWorld, this.cameraPos, this.elapsedSeconds, this.windX, this.windZ, this.windStrength, this.windOpacity, i);
            }

            if (this.raining || this.snowing) {
                this.drawPrecipitation(framebuffer, matrix4f);
            }
        } finally {
            matrix4fStack.popMatrix();
        }
    }

    public float getWindX() {
        return this.windX;
    }

    public float getWindZ() {
        return this.windZ;
    }

    public float getWindStrength() {
        return this.windStrength;
    }

    public GpuTextureView getWeatherMapTexture() {
        return this.weatherMap.getTextureView();
    }

    public int getWeatherMapOriginX() {
        return this.weatherMap.getOriginX();
    }

    public int getWeatherMapOriginZ() {
        return this.weatherMap.getOriginZ();
    }

    public int getWeatherMapSize() {
        return 96;
    }

    public void reset() {
        this.closeParticleBuffer();
        this.weatherMap.close();
        AmbienceLightningRenderer.INSTANCE.clear();
    }

    private void closeParticleBuffer() {
        if (this.particleBuffer != null) {
            this.particleBuffer.close();
        }

        this.particleBuffer = null;
    }

    private void updateWind(AmbienceProfile ambienceProfile) {
        if (!ambienceProfile.isWindEnabled()) {
            this.windStrength = 0.0F;
        } else {
            double d = 0.8 + 0.2 * Math.sin(this.elapsedSeconds * 0.37) * Math.sin(this.elapsedSeconds * 0.13 + 1.3);
            this.windStrength = (float)(ambienceProfile.windStrength.getValue().floatValue() / 100.0 * d);
            float f = (float)Math.toRadians(ambienceProfile.windDirection.getValue().floatValue());
            this.windX = -MathHelper.sin(f);
            this.windZ = MathHelper.cos(f);
        }
    }

    private void advanceOffsets(double[] doubles, float[] floats, float f, float f2) {
        float f3 = this.windX * this.windStrength * f * f2;
        float f4 = this.windZ * this.windStrength * f * f2;
        int i = floats.length;

        for (int i2 = 0; i2 < i; i2++) {
            int i3 = i2 * 3;
            doubles[i3] = wrap(doubles[i3] + f3, 192.0);
            doubles[i3 + 1] = wrap(doubles[i3 + 1] - floats[i2] * f2, 192.0);
            doubles[i3 + 2] = wrap(doubles[i3 + 2] + f4, 192.0);
        }
    }

    private void updateAmbientColor() {
        int i = AmbienceRenderer.getSkyColor();
        int i2 = AmbienceRenderer.getHorizonColor();
        float f = smoothstep(-0.15F, 0.25F, MathHelper.cos(AmbienceRenderer.getSunAngle()));
        float f2 = 0.3F + 0.7F * f;
        this.ambientRed = Math.max(blendAmbient(ColorUtil.redf(i2), ColorUtil.redf(i)) * f2, 0.1F);
        this.ambientGreen = Math.max(blendAmbient(ColorUtil.greenf(i2), ColorUtil.greenf(i)) * f2, 0.11F);
        this.ambientBlue = Math.max(blendAmbient(ColorUtil.bluef(i2), ColorUtil.bluef(i)) * f2, 0.14F);
    }

    private static float blendAmbient(float f, float f2) {
        return MathHelper.lerp(0.45F, MathHelper.lerp(0.35F, f, f2), 1.0F);
    }

    private void drawPrecipitation(Framebuffer framebuffer, Matrix4f matrix4f) {
        GpuTextureView gpuTextureView = this.weatherMap.getTextureView();
        if (gpuTextureView != null) {
            float f = QUALITY_MULTIPLIERS[MathHelper.clamp(this.quality, 1, QUALITY_MULTIPLIERS.length) - 1] * this.density;
            int i = 0;
            if (this.snowing) {
                i = this.addBatches(0, Pipelines.WEATHER_SNOW, SNOW_LAYERS, this.snowOffsets, SNOW_FALL_SPEEDS, 5.0F, 0.0F, 0.35F, f);
            }

            if (this.raining) {
                i = this.addBatches(i, Pipelines.WEATHER_RAIN, RAIN_LAYERS, this.rainOffsets, RAIN_FALL_SPEEDS, 10.0F, 0.75F, 0.0F, f);
            }

            if (i != 0) {
                GpuBuffer gpuBuffer = this.getParticleBuffer();
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(196608);
                GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrix4f, WHITE, ZERO_OFFSET, IDENTITY);
                GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

                try (
                    FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState();
                    RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                            () -> "Sadnes ambience precipitation",
                            framebuffer.getColorAttachmentView(),
                            OptionalInt.empty(),
                            framebuffer.getDepthAttachmentView(),
                            OptionalDouble.empty()
                        );
                ) {
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                    renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());

                    for (int i2 = 0; i2 < i; i2++) {
                        renderPass.setPipeline(this.batchPipelines[i2]);
                        renderPass.setUniform("SadnesWeather", this.batchUniforms[i2]);
                        renderPass.drawIndexed(0, 0, this.batchCounts[i2] * 6, 1);
                    }
                }
            }
        }
    }

    private int addBatches(
        int i,
        RenderPipeline renderPipeline,
        AmbienceWeatherRenderer.ParticleLayer[] particleLayers,
        double[] doubles,
        float[] floats,
        float f,
        float f2,
        float f3,
        float f4
    ) {
        float f5 = this.windX * this.windStrength * f;
        float f6 = -floats[1];
        float f7 = this.windZ * this.windStrength * f;
        float f8 = MathHelper.sqrt(f5 * f5 + f6 * f6 + f7 * f7);
        float f9 = f5 / f8;
        float f10 = f6 / f8;
        float f11 = f7 / f8;

        for (AmbienceWeatherRenderer.ParticleLayer particleLayer : particleLayers) {
            int i2 = Math.min(Math.round(particleLayer.count() * f4), 32768);
            if (i2 > 0) {
                this.batchPipelines[i] = renderPipeline;
                this.batchUniforms[i] = this.createUniforms(
                    particleLayer.box(), particleLayer.innerFade(), doubles, f9, f10, f11, f2, particleLayer.size(), particleLayer.opacity(), f3
                );
                this.batchCounts[i] = i2;
                i++;
            }
        }

        return i;
    }

    private GpuBufferSlice createUniforms(
        float f, float f2, double[] doubles, float f3, float f4, float f5, float f6, float f7, float f8, float f9
    ) {
        return WeatherUniformStorage.write(
            new WeatherUniforms(
                f,
                f2,
                wrapOffset(this.cameraPos.x, doubles[0], f),
                wrapOffset(this.cameraPos.y, doubles[1], f),
                wrapOffset(this.cameraPos.z, doubles[2], f),
                wrapOffset(this.cameraPos.x, doubles[3], f),
                wrapOffset(this.cameraPos.y, doubles[4], f),
                wrapOffset(this.cameraPos.z, doubles[5], f),
                wrapOffset(this.cameraPos.x, doubles[6], f),
                wrapOffset(this.cameraPos.y, doubles[7], f),
                wrapOffset(this.cameraPos.z, doubles[8], f),
                f3,
                f4,
                f5,
                f6,
                f7,
                f8,
                (float)(this.elapsedSeconds % 1024.0),
                (float)(this.weatherMap.getOriginX() - this.cameraPos.x),
                (float)(this.weatherMap.getOriginZ() - this.cameraPos.z),
                (float)this.cameraPos.y,
                96.0F,
                this.ambientRed,
                this.ambientGreen,
                this.ambientBlue,
                this.lightningFlash,
                this.cameraParamA,
                this.cameraParamB,
                this.rainSurfaceIntensity,
                this.snowSurfaceIntensity,
                f9,
                this.weatherMap.getCenterPrecipitation(),
                this.quality
            )
        );
    }

    private static float wrapOffset(double d, double d2, float f) {
        return f > 0.0F ? (float)wrap(d - d2, f) : 0.0F;
    }

    private static double wrap(double d, double d2) {
        return d - Math.floor(d / d2) * d2;
    }

    private static float smoothstep(float f, float f2, float f3) {
        float f4 = MathHelper.clamp((f3 - f) / (f2 - f), 0.0F, 1.0F);
        return f4 * f4 * (3.0F - 2.0F * f4);
    }

    private GpuBuffer getParticleBuffer() {
        if (this.particleBuffer != null && !this.particleBuffer.isClosed()) {
            return this.particleBuffer;
        }

        Random random = new Random(5953509L);
        int i = VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize();

        try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(131072 * i)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (int i2 = 0; i2 < 32768; i2++) {
                float f = random.nextFloat();
                float f2 = random.nextFloat();
                float f3 = random.nextFloat();
                int i3 = random.nextInt();
                bufferBuilder.vertex(f, f2, f3).texture(0.0F, 0.0F).color(i3);
                bufferBuilder.vertex(f, f2, f3).texture(1.0F, 0.0F).color(i3);
                bufferBuilder.vertex(f, f2, f3).texture(1.0F, 1.0F).color(i3);
                bufferBuilder.vertex(f, f2, f3).texture(0.0F, 1.0F).color(i3);
            }

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                this.particleBuffer = RenderSystem.getDevice().createBuffer(() -> "Sadnes ambience weather particles", 40, builtBuffer.getBuffer());
            }
        }

        return this.particleBuffer;
    }

    record ParticleLayer(float box, float innerFade, int count, float size, float opacity) {
    }
}
