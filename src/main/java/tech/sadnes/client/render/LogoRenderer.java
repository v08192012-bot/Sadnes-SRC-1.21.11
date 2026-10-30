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

public final class LogoRenderer {
    private static final float OUTLINE_SCALE = 16.0F;

    private LogoRenderer() {
    }

    public static void draw(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        int i,
        int i2,
        float f5,
        float f6,
        float f7,
        float f8
    ) {
        if (drawContext != null && textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f8 <= 5.0E-4F)) {
            Matrix3x2f matrix3x2f = new Matrix3x2f(matrix3x2fc);
            float f9 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2f, screenRect);
            if (screenRect2 != null) {
                guiRenderState.addSimpleElement(
                    new LogoRenderer.LogoElement(
                        textureSetup,
                        matrix3x2f,
                        f,
                        f2,
                        f + f3,
                        f2 + f4,
                        applyAlpha(i, f8),
                        applyAlpha(i2, f8),
                        Math.max(0.0F, f5) * f9,
                        MathHelper.clamp(f6, 0.0F, 1.0F),
                        packOutline(Math.max(0.0F, f7) * f9),
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

    private static short packOutline(float f) {
        return (short)MathHelper.clamp(Math.round(f * 16.0F), 0, 32767);
    }

    record LogoElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int colorLeft,
        int colorRight,
        float glowWidth,
        float glowStrength,
        short outlineWidth,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            this.emit(vertexConsumer, this.x1, this.y1, 0.0F, 0.0F, this.colorLeft);
            this.emit(vertexConsumer, this.x1, this.y2, 0.0F, 1.0F, this.colorLeft);
            this.emit(vertexConsumer, this.x2, this.y2, 1.0F, 1.0F, this.colorRight);
            this.emit(vertexConsumer, this.x2, this.y1, 1.0F, 0.0F, this.colorRight);
        }

        private void emit(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int i) {
            vertexConsumer.vertex(this.pose, f, f2)
                .color(i)
                .texture(f3, f4)
                .overlay(0, 0)
                .light(this.outlineWidth, 0)
                .normal(0.0F, this.glowStrength, 0.0F)
                .lineWidth(this.glowWidth);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.LOGO_GUI;
        }
    }
}
