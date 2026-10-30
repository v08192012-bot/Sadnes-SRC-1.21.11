package tech.sadnes.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import tech.sadnes.api.mixin.accessor.DrawContextAccessor;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.util.color.ColorUtil;

public final class TexturedShapeRenderer {
    public static final int TYPE_GRADIENT = 2;
    public static final int TYPE_TEXTURE = 3;
    public static final int TYPE_FULL = 6;
    private static final float RADIUS_PRECISION = 16.0F;

    private TexturedShapeRenderer() {
    }

    public static void drawTexture(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        Radius radius,
        float f5,
        float f6,
        float f7,
        float f8,
        int i,
        int i2
    ) {
        drawTexture(drawContext, textureSetup, matrix3x2fc, f, f2, f3, f4, radius, f5, f6, f7, f8, i, i2, false);
    }

    public static void drawTexture(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        Radius radius,
        float f5,
        float f6,
        float f7,
        float f8,
        int i,
        int i2,
        boolean flag
    ) {
        drawTextureRegion(drawContext, textureSetup, matrix3x2fc, f, f2, f3, f4, 0.0F, 0.0F, 1.0F, 1.0F, radius, f5, f6, f7, f8, i, i, i2, flag);
    }

    public static void drawTextureRegion(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        Radius radius,
        float f9,
        float f10,
        float f11,
        float f12,
        int i,
        int i2,
        int i3,
        boolean flag
    ) {
        if (drawContext != null && textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f12 <= 5.0E-4F)) {
            Matrix3x2f matrix3x2f = new Matrix3x2f(matrix3x2fc);
            float f13 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
            float f14 = i3 != 6 && i3 != 3 ? f9 * f13 : f9;
            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2f, screenRect);
            if (screenRect2 != null) {
                int i4 = applyAlpha(i, f12);
                int i5 = applyAlpha(i2, f12);
                if (VerticalFadeMask.isActive()) {
                    float f15 = f2 + f4 / 2.0F;
                    i4 = VerticalFadeMask.applyAlpha(i4, f15);
                    i5 = VerticalFadeMask.applyAlpha(i5, f15);
                }

                guiRenderState.addSimpleElement(
                    new TexturedShapeRenderer.TexturedShapeElement(
                        textureSetup,
                        matrix3x2f,
                        f,
                        f2,
                        f + f3,
                        f2 + f4,
                        f5,
                        f6,
                        f7,
                        f8,
                        i4,
                        i5,
                        packRadiusSigned(radius.getTopLeft() * f13, radius.isSmooth()),
                        packRadiusSigned(radius.getTopRight() * f13, radius.isSmooth()),
                        packRadiusSigned(radius.getBottomRight() * f13, radius.isSmooth()),
                        packRadiusSigned(radius.getBottomLeft() * f13, radius.isSmooth()),
                        f14,
                        MathHelper.clamp(f10, -1.0F, 1.0F),
                        MathHelper.clamp(f11, -1.0F, 1.0F),
                        getTypeSignal(i3),
                        flag && i3 == 3,
                        false,
                        screenRect,
                        screenRect2
                    )
                );
            }
        }
    }

    public static void drawBloom(DrawContext drawContext, TextureSetup textureSetup, Matrix3x2fc matrix3x2fc, float f, float f2, float f3, float f4, float f5, int i) {
        if (drawContext != null && textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f5 <= 5.0E-4F)) {
            Matrix3x2f matrix3x2f = new Matrix3x2f(matrix3x2fc);
            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2f, screenRect);
            if (screenRect2 != null) {
                int i2 = applyAlpha(i, f5);
                guiRenderState.addSimpleElement(
                    new TexturedShapeRenderer.TexturedShapeElement(
                        textureSetup,
                        matrix3x2f,
                        f,
                        f2,
                        f + f3,
                        f2 + f4,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F,
                        i2,
                        i2,
                        (short)0,
                        (short)0,
                        (short)0,
                        (short)0,
                        0.0F,
                        -0.5F,
                        0.5F,
                        getTypeSignal(2),
                        false,
                        true,
                        screenRect,
                        screenRect2
                    )
                );
            }
        }
    }

    public static void drawOutline(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        float f10,
        int i
    ) {
        drawOutline(drawContext, textureSetup, matrix3x2fc, f, f2, f3, f4, f5, f6, f7, f8, Radius.all(f9), f10, i);
    }

    public static void drawOutline(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        Radius radius,
        float f9,
        int i
    ) {
        if (drawContext != null && textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f9 <= 5.0E-4F)) {
            Matrix3x2f matrix3x2f = new Matrix3x2f(matrix3x2fc);
            float f10 = (float)Math.sqrt(matrix3x2f.m00() * matrix3x2f.m00() + matrix3x2f.m01() * matrix3x2f.m01());
            float f11 = MinecraftClient.getInstance().getWindow().getScaleFactor() * f10;
            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2f, screenRect);
            if (screenRect2 != null) {
                float f12 = (radius != null ? radius.getMax() : 0.0F) * f11;
                float f13 = radius != null && radius.isSmooth() ? -f12 : f12;
                guiRenderState.addSimpleElement(
                    new TexturedShapeRenderer.TexturedShapeElement(
                        textureSetup,
                        matrix3x2f,
                        f,
                        f2,
                        f + f3,
                        f2 + f4,
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F,
                        applyAlpha(i, f9),
                        applyAlpha(i, f9),
                        packRadius(f5),
                        packRadius(f6),
                        packRadius(f7),
                        packRadius(f8),
                        f13,
                        0.0F,
                        0.0F,
                        0.5F,
                        false,
                        false,
                        screenRect,
                        screenRect2
                    )
                );
            }
        }
    }

    private static int applyAlpha(int i, float f) {
        int i2 = Math.round(ColorUtil.alpha(i) * MathHelper.clamp(f, 0.0F, 1.0F));
        return ColorUtil.setAlpha(i, i2);
    }

    private static short packRadius(float f) {
        return (short)MathHelper.clamp(Math.round(Math.max(0.0F, f) * 16.0F), 0, 32767);
    }

    private static short packRadiusSigned(float f, boolean flag) {
        short s = packRadius(f);
        return flag ? (short)(-s) : s;
    }

    private static float getTypeSignal(int i) {
        if (i == 6) {
            return 1.0F;
        } else {
            return i == 3 ? 0.0F : -1.0F;
        }
    }

    record TexturedShapeElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        float u1,
        float v1,
        float u2,
        float v2,
        int colorLeft,
        int colorRight,
        short radiusLeftBottom,
        short radiusLeftTop,
        short radiusRightBottom,
        short radiusRightTop,
        float value,
        float smoothness1,
        float smoothness2,
        float typeSignal,
        boolean include2DBlur,
        boolean bloom,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            this.emit(vertexConsumer, this.x1, this.y1, this.u1, this.v1, this.colorLeft);
            this.emit(vertexConsumer, this.x1, this.y2, this.u1, this.v2, this.colorLeft);
            this.emit(vertexConsumer, this.x2, this.y2, this.u2, this.v2, this.colorRight);
            this.emit(vertexConsumer, this.x2, this.y1, this.u2, this.v1, this.colorRight);
        }

        private void emit(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int i) {
            vertexConsumer.vertex(this.pose, f, f2)
                .color(i)
                .texture(f3, f4)
                .overlay(this.radiusLeftBottom, this.radiusLeftTop)
                .light(this.radiusRightBottom, this.radiusRightTop)
                .normal(this.typeSignal, this.smoothness1, this.smoothness2)
                .lineWidth(this.value);
        }

        @Override
        public RenderPipeline pipeline() {
            if (this.bloom) {
                return Pipelines.SHAPE_TEXTURE_GUI_BLOOM;
            } else {
                return this.include2DBlur ? Pipelines.SHAPE_TEXTURE_GUI_2D_BLUR : Pipelines.SHAPE_TEXTURE_GUI;
            }
        }
    }
}
