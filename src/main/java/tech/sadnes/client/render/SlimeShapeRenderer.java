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

public final class SlimeShapeRenderer {
    private SlimeShapeRenderer() {
    }

    public static void drawSlime(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        Radius radius,
        SlimeShapeRenderer.Style style,
        int i,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        float f10,
        float f11,
        float f12
    ) {
        if (drawContext != null && textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            if (!(f5 <= 5.0E-4F) && !(f7 <= 5.0E-4F)) {
                float f13 = matrix3x2fc.m00();
                float f14 = matrix3x2fc.m01();
                float f15 = MinecraftClient.getInstance().getWindow().getScaleFactor() * (float)Math.sqrt(f13 * f13 + f14 * f14);
                ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
                ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2fc, screenRect);
                if (screenRect2 != null) {
                    int i2 = ColorUtil.setAlpha(
                        i, Math.round(ColorUtil.alpha(i) * MathHelper.clamp(f5, 0.0F, 1.0F) * MathHelper.clamp(f7, 0.0F, 1.0F))
                    );
                    GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                    guiRenderState.addSimpleElement(
                        new SlimeShapeRenderer.SlimeElement(
                            textureSetup,
                            new Matrix3x2f(matrix3x2fc),
                            f,
                            f2,
                            f + f3,
                            f2 + f4,
                            i2,
                            RoundedRectRenderer.packRadius(radius.getMax() * f15, radius.isSmooth()),
                            packUnit(f9),
                            packUnit(f10),
                            packUnit(f11 * 0.5F + 0.5F),
                            MathHelper.clamp(f6, 0.0F, 1.0F),
                            MathHelper.clamp(f8, 0.0F, 1.0F),
                            style.modeSignal,
                            f12,
                            screenRect,
                            screenRect2
                        )
                    );
                }
            }
        }
    }

    private static short packUnit(float f) {
        return (short)MathHelper.clamp(Math.round(f * 10000.0F), 0, 10000);
    }

    record SlimeElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int color,
        short radius,
        short splashAge,
        short splashX,
        short tilt,
        float hover,
        float cursor,
        float modeSignal,
        float time,
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
                .overlay(this.radius, this.splashAge)
                .light(this.splashX, this.tilt)
                .normal(this.hover, this.cursor, this.modeSignal)
                .lineWidth(this.time);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.SLIME_GUI;
        }
    }

    public enum Style {
        SLIME(0.0F),
        WATER(0.25F),
        OVERLAY(1.0F);

        final float modeSignal;

        Style(float f) {
            this.modeSignal = f;
        }
    }
}
