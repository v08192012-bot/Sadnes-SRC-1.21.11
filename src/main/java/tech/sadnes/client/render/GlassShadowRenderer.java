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

public final class GlassShadowRenderer {
    private static final float RADIUS_SCALE = 16.0F;

    private GlassShadowRenderer() {
    }

    public static void draw(
        DrawContext drawContext, TextureSetup textureSetup, Matrix3x2fc matrix3x2fc, float f, float f2, float f3, float f4, Radius radius, float f5, float f6, int i
    ) {
        if (drawContext != null && textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f6 <= 5.0E-4F) && !(f5 <= 0.0F)) {
            Matrix3x2f matrix3x2f = new Matrix3x2f(matrix3x2fc);
            float f7 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2f, screenRect);
            if (screenRect2 != null) {
                GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                guiRenderState.addSimpleElement(
                    new GlassShadowRenderer.GlassShadowElement(
                        textureSetup,
                        matrix3x2f,
                        f,
                        f2,
                        f + f3,
                        f2 + f4,
                        applyAlpha(i, f6),
                        packRadius(radius.getTopLeft() * f7, radius.isSmooth()),
                        packRadius(radius.getTopRight() * f7, radius.isSmooth()),
                        packRadius(radius.getBottomRight() * f7, radius.isSmooth()),
                        packRadius(radius.getBottomLeft() * f7, radius.isSmooth()),
                        f5 * f7,
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

    private static short packRadius(float f, boolean flag) {
        short s = (short)MathHelper.clamp(Math.round(Math.max(0.0F, f) * 16.0F), 0, 32767);
        return flag ? (short)(-s) : s;
    }

    record GlassShadowElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int color,
        short radiusLeftBottom,
        short radiusLeftTop,
        short radiusRightBottom,
        short radiusRightTop,
        float spread,
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
                .overlay(this.radiusLeftBottom, this.radiusLeftTop)
                .light(this.radiusRightBottom, this.radiusRightTop)
                .normal(0.0F, 0.0F, 0.0F)
                .lineWidth(this.spread);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.GLASS_SHADOW;
        }
    }
}
