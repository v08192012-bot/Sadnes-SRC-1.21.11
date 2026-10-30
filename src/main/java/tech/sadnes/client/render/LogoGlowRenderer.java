package tech.sadnes.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
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
import org.joml.Matrix3x2fc;
import tech.sadnes.api.mixin.accessor.DrawContextAccessor;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.texture.LogoTexture;
import tech.sadnes.client.util.ResourceUtil;
import tech.sadnes.client.util.color.ColorUtil;

public final class LogoGlowRenderer {
    private static final float HIT_HALF_WIDTH = 0.24900001F;
    private static final float HIT_HALF_HEIGHT = 0.22299999F;
    private static NativeImageBackedTexture dataTexture;

    private LogoGlowRenderer() {
    }

    public static void draw(DrawContext drawContext, Matrix3x2fc matrix3x2fc, int i, float f, float f2, float f3, float f4, LogoGlowRenderer.GlowData glowData) {
        if (drawContext != null && !(f <= 5.0E-4F)) {
            TextureSetup textureSetup = LogoTexture.getTextureSetup();
            if (textureSetup != null) {
                NativeImageBackedTexture nativeImageBackedTexture = getDataTexture();
                NativeImage nativeImage = nativeImageBackedTexture.getImage();
                if (nativeImage != null) {
                    int i2 = glowData.pixels.length;

                    for (int i3 = 0; i3 < i2; i3++) {
                        nativeImage.setColorArgb(i3, 0, glowData.pixels[i3]);
                    }

                    nativeImageBackedTexture.upload();
                    GpuTextureView gpuTextureView = ResourceUtil.getView(nativeImageBackedTexture);
                    if (gpuTextureView != null) {
                        TextureSetup textureSetup2 = TextureSetup.of(textureSetup.texure0(), textureSetup.sampler0(), gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        float f5 = transformX(matrix3x2fc, 0.251F, 0.277F);
                        float f6 = transformY(matrix3x2fc, 0.251F, 0.277F);
                        float f7 = transformX(matrix3x2fc, 0.251F, 0.723F);
                        float f8 = transformY(matrix3x2fc, 0.251F, 0.723F);
                        float f9 = transformX(matrix3x2fc, 0.749F, 0.723F);
                        float f10 = transformY(matrix3x2fc, 0.749F, 0.723F);
                        float f11 = transformX(matrix3x2fc, 0.749F, 0.277F);
                        float f12 = transformY(matrix3x2fc, 0.749F, 0.277F);
                        Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
                        ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
                        ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(
                            Math.min(Math.min(f5, f7), Math.min(f9, f11)),
                            Math.min(Math.min(f6, f8), Math.min(f10, f12)),
                            Math.max(Math.max(f5, f7), Math.max(f9, f11)),
                            Math.max(Math.max(f6, f8), Math.max(f10, f12)),
                            matrix3x2f,
                            screenRect
                        );
                        if (screenRect2 != null) {
                            int i4 = ColorUtil.setAlpha(i, Math.round(ColorUtil.alpha(i) * MathHelper.clamp(f, 0.0F, 1.0F)));
                            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                            guiRenderState.addSimpleElement(
                                new LogoGlowRenderer.LogoGlowElement(
                                    textureSetup2,
                                    matrix3x2f,
                                    f5,
                                    f6,
                                    f7,
                                    f8,
                                    f9,
                                    f10,
                                    f11,
                                    f12,
                                    0.251F,
                                    0.277F,
                                    0.749F,
                                    0.723F,
                                    i4,
                                    packUnit(f2),
                                    packUnit(f3),
                                    f4,
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
        if (dataTexture != null) {
            dataTexture.close();
            dataTexture = null;
        }
    }

    private static NativeImageBackedTexture getDataTexture() {
        if (dataTexture == null) {
            dataTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_logo_glow_data", 32, 1, false);
        }

        return dataTexture;
    }

    private static float transformX(Matrix3x2fc matrix3x2fc, float f, float f2) {
        return matrix3x2fc.m00() * f + matrix3x2fc.m10() * f2 + matrix3x2fc.m20();
    }

    private static float transformY(Matrix3x2fc matrix3x2fc, float f, float f2) {
        return matrix3x2fc.m01() * f + matrix3x2fc.m11() * f2 + matrix3x2fc.m21();
    }

    private static int packUnit(float f) {
        return Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 10000.0F);
    }

    public static final class GlowData {
        final int[] pixels = new int[32];

        public void setPoint(int i, float f, float f2, float f3, float f4, float f5, float f6) {
            this.pixels[i] = packPosition(f, f2);
            this.pixels[16 + i] = packArgb(Math.max(1, toByte(f3)), toByte(f4), toByte(f5 * 0.5F + 0.5F), toByte(f6 * 0.5F + 0.5F));
        }

        public void clearPoint(int i) {
            this.pixels[16 + i] = 0;
        }

        public void setCursor(float f, float f2, float f3, float f4, float f5) {
            boolean flag = Math.abs(f - 0.5F) <= 0.24900001F && Math.abs(f2 - 0.5F) <= 0.22299999F;
            this.pixels[12] = packPosition(f, f2);
            this.pixels[28] = packArgb(toByte(f3), flag ? 255 : 0, toByte(f4 * 0.5F + 0.5F), toByte(f5 * 0.5F + 0.5F));
        }

        private static int toByte(float f) {
            return Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 255.0F);
        }

        private static int packPosition(float f, float f2) {
            int i = MathHelper.clamp(Math.round((f + 0.5F) * 0.5F * 65535.0F), 0, 65535);
            int i2 = MathHelper.clamp(Math.round((f2 + 0.5F) * 0.5F * 65535.0F), 0, 65535);
            return packArgb(i >> 8, i & 0xFF, i2 >> 8, i2 & 0xFF);
        }

        private static int packArgb(int i, int i2, int i3, int i4) {
            return i4 << 24 | i << 16 | i2 << 8 | i3;
        }
    }

    record LogoGlowElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float x2,
        float y2,
        float x3,
        float y3,
        float u0,
        float v0,
        float u1,
        float v1,
        int color,
        int energy,
        int flash,
        float phase,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            this.emit(vertexConsumer, this.x0, this.y0, this.u0, this.v0);
            this.emit(vertexConsumer, this.x1, this.y1, this.u0, this.v1);
            this.emit(vertexConsumer, this.x2, this.y2, this.u1, this.v1);
            this.emit(vertexConsumer, this.x3, this.y3, this.u1, this.v0);
        }

        private void emit(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4) {
            vertexConsumer.vertex(this.pose, f, f2)
                .color(this.color)
                .texture(f3, f4)
                .overlay(this.energy, this.flash)
                .light(0, 0)
                .normal(0.0F, 0.0F, 0.0F)
                .lineWidth(this.phase);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.LOGO_GLOW_GUI;
        }
    }
}
