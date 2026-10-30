package tech.sadnes.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import tech.sadnes.api.mixin.accessor.DrawContextAccessor;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.util.ResourceUtil;
import tech.sadnes.client.util.color.ColorUtil;

public final class RopeRenderer {
    private static final int SAMPLES = 64;
    private static final float WIDTH_SCALE = 16.0F;
    private static NativeImageBackedTexture dataTexture;
    private static final float[] SAMPLED_X = new float[64];
    private static final float[] SAMPLED_Y = new float[64];

    private RopeRenderer() {
    }

    public static void draw(
        DrawContext drawContext, float[] floats, float[] floats2, int i, float f, float f2, float f3, float f4, int i2, float f5, float f6
    ) {
        if (drawContext != null && i >= 2 && !(f5 <= 5.0E-4F)) {
            resample(floats, floats2, i);
            float f7 = SAMPLED_X[0];
            float f8 = SAMPLED_X[0];
            float f9 = SAMPLED_Y[0];
            float f10 = SAMPLED_Y[0];

            for (int i3 = 1; i3 < 64; i3++) {
                f7 = Math.min(f7, SAMPLED_X[i3]);
                f8 = Math.max(f8, SAMPLED_X[i3]);
                f9 = Math.min(f9, SAMPLED_Y[i3]);
                f10 = Math.max(f10, SAMPLED_Y[i3]);
            }

            float f11 = Math.max(f * 0.5F + f3, f4) + 1.0F;
            float f12 = f7 - f11;
            float f13 = f9 - f11;
            float f14 = f8 + f11;
            float f15 = f10 + f11;
            float f16 = Math.max(1.0F, f14 - f12);
            float f17 = Math.max(1.0F, f15 - f13);
            NativeImageBackedTexture nativeImageBackedTexture = getDataTexture();
            NativeImage nativeImage = nativeImageBackedTexture.getImage();
            if (nativeImage != null) {
                for (int i4 = 0; i4 < 64; i4++) {
                    nativeImage.setColorArgb(i4, 0, packPosition((SAMPLED_X[i4] - f12) / f16, (SAMPLED_Y[i4] - f13) / f17));
                }

                nativeImageBackedTexture.upload();
                GpuTextureView gpuTextureView = ResourceUtil.getView(nativeImageBackedTexture);
                if (gpuTextureView != null) {
                    TextureSetup textureSetup = TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                    Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
                    float f18 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
                    GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                    ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
                    ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f12, f13, f14, f15, matrix3x2f, screenRect);
                    if (screenRect2 != null) {
                        guiRenderState.addSimpleElement(
                            new RopeRenderer.RopeElement(
                                textureSetup,
                                matrix3x2f,
                                f12,
                                f13,
                                f14,
                                f15,
                                applyAlpha(i2, f5),
                                f * f18,
                                packWidth(f2 * f18),
                                packWidth(f3 * f18),
                                packWidth(f4 * f18),
                                MathHelper.clamp(f6, 0.0F, 1.0F),
                                screenRect,
                                screenRect2
                            )
                        );
                    }
                }
            }
        }
    }

    public static void close() {
        if (dataTexture != null) {
            dataTexture.close();
            dataTexture = null;
        }
    }

    private static NativeImageBackedTexture getDataTexture() {
        if (dataTexture == null) {
            dataTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_rope_data", 64, 1, false);
        }

        return dataTexture;
    }

    private static void resample(float[] floats, float[] floats2, int i) {
        int i2 = i - 1;

        for (int i3 = 0; i3 < 64; i3++) {
            float f = i3 / 63.0F * i2;
            int i4 = (int)f;
            if (i4 >= i2) {
                SAMPLED_X[i3] = floats[i2];
                SAMPLED_Y[i3] = floats2[i2];
            } else {
                float f2 = f - i4;
                SAMPLED_X[i3] = floats[i4] + (floats[i4 + 1] - floats[i4]) * f2;
                SAMPLED_Y[i3] = floats2[i4] + (floats2[i4 + 1] - floats2[i4]) * f2;
            }
        }
    }

    private static int packPosition(float f, float f2) {
        int i = MathHelper.clamp(Math.round(f * 65535.0F), 0, 65535);
        int i2 = MathHelper.clamp(Math.round(f2 * 65535.0F), 0, 65535);
        int i3 = i >> 8 & 0xFF;
        int i4 = i & 0xFF;
        int i5 = i2 >> 8 & 0xFF;
        int i6 = i2 & 0xFF;
        return i6 << 24 | i3 << 16 | i4 << 8 | i5;
    }

    private static int applyAlpha(int i, float f) {
        int i2 = Math.round(ColorUtil.alpha(i) * MathHelper.clamp(f, 0.0F, 1.0F));
        return ColorUtil.setAlpha(i, i2);
    }

    private static short packWidth(float f) {
        return (short)MathHelper.clamp(Math.round(Math.max(0.0F, f) * 16.0F), 0, 32767);
    }

    record RopeElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int color,
        float bodyThick,
        short spineThick,
        short glowRadius,
        short knotRadius,
        float phase,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            this.emit(vertexConsumer, this.x1, this.y1, 0.0F, 0.0F);
            this.emit(vertexConsumer, this.x1, this.y2, 0.0F, 1.0F);
            this.emit(vertexConsumer, this.x2, this.y2, 1.0F, 1.0F);
            this.emit(vertexConsumer, this.x2, this.y1, 1.0F, 0.0F);
        }

        private void emit(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4) {
            vertexConsumer.vertex(this.pose, f, f2)
                .color(this.color)
                .texture(f3, f4)
                .overlay(this.spineThick, this.glowRadius)
                .light(this.knotRadius, Math.round(this.phase * 1000.0F))
                .normal(0.0F, 0.0F, 0.0F)
                .lineWidth(this.bodyThick);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.ROPE_LINE;
        }
    }
}
