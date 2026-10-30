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

public final class BezierCurveRenderer {
    private static final int CONTROL_POINTS = 4;
    private static final float RADIUS_SCALE = 16.0F;
    private static final int POOL_SIZE = 32;
    private static final NativeImageBackedTexture[] dataTextures = new NativeImageBackedTexture[32];
    private static int nextTextureIndex;

    private BezierCurveRenderer() {
    }

    public static void drawCurve(
        DrawContext drawContext, float f, float f2, float f3, float f4, float[] floats, int i, float f5, float f6, float f7, float f8
    ) {
        if (drawContext != null && floats != null && floats.length >= 8) {
            if (!(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f5 <= 5.0E-4F)) {
                float f9 = f6 * 0.5F + Math.max(f7, 0.0F) + 1.0F;
                float f10 = f - f9;
                float f11 = f2 - f9;
                float f12 = f3 + f9 * 2.0F;
                float f13 = f4 + f9 * 2.0F;
                float f14 = f10 + f12;
                float f15 = f11 + f13;
                NativeImageBackedTexture nativeImageBackedTexture = nextDataTexture();
                NativeImage nativeImage = nativeImageBackedTexture.getImage();
                if (nativeImage != null) {
                    for (int i2 = 0; i2 < 4; i2++) {
                        float f16 = (f9 + floats[i2 * 2] * f3) / f12;
                        float f17 = (f9 + floats[i2 * 2 + 1] * f4) / f13;
                        nativeImage.setColorArgb(i2, 0, packPoint(f16, f17));
                    }

                    nativeImageBackedTexture.upload();
                    GpuTextureView gpuTextureView = ResourceUtil.getView(nativeImageBackedTexture);
                    if (gpuTextureView != null) {
                        TextureSetup textureSetup = TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
                        float f18 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
                        GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                        ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
                        ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f10, f11, f14, f15, matrix3x2f, screenRect);
                        if (screenRect2 != null) {
                            guiRenderState.addSimpleElement(
                                new BezierCurveRenderer.BezierElement(
                                    textureSetup,
                                    matrix3x2f,
                                    f10,
                                    f11,
                                    f14,
                                    f15,
                                    applyAlpha(i, f5),
                                    f6 * f18,
                                    packRadius(f7 * f18),
                                    MathHelper.clamp(f8, 0.0F, 1.0F),
                                    screenRect,
                                    screenRect2
                                )
                            );
                        }
                    }
                }
            }
        }
    }

    public static void close() {
        for (int i = 0; i < 32; i++) {
            if (dataTextures[i] != null) {
                dataTextures[i].close();
                dataTextures[i] = null;
            }
        }

        nextTextureIndex = 0;
    }

    private static NativeImageBackedTexture nextDataTexture() {
        int i = nextTextureIndex;
        nextTextureIndex = (nextTextureIndex + 1) % 32;
        NativeImageBackedTexture nativeImageBackedTexture = dataTextures[i];
        if (nativeImageBackedTexture == null) {
            nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_bezier_data_" + i, 4, 1, false);
            dataTextures[i] = nativeImageBackedTexture;
        }

        return nativeImageBackedTexture;
    }

    private static int packPoint(float f, float f2) {
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

    private static short packRadius(float f) {
        return (short)MathHelper.clamp(Math.round(Math.max(0.0F, f) * 16.0F), 0, 32767);
    }

    record BezierElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int color,
        float bodyThick,
        short glowRadius,
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
                .overlay(this.glowRadius, Math.round(this.phase * 1000.0F))
                .light(0, 0)
                .normal(0.0F, 0.0F, 0.0F)
                .lineWidth(this.bodyThick);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.BEZIER_CURVE;
        }
    }
}
