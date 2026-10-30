package tech.sadnes.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
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

public final class Arrow3dRenderer {
    private static final float ANGLE_SCALE = 16.0F;
    private static final float FRACTION_SCALE = 10000.0F;

    private Arrow3dRenderer() {
    }

    public static void drawArrow(
        DrawContext drawContext,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        int i,
        float f7,
        float f8,
        float f9
    ) {
        if (drawContext != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f7 <= 5.0E-4F)) {
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2fc, screenRect);
            if (screenRect2 != null) {
                int i2 = ColorUtil.setAlpha(i, Math.round(ColorUtil.alpha(i) * MathHelper.clamp(f7, 0.0F, 1.0F)));
                short s = packAngle((f5 % 360.0F + 360.0F) % 360.0F);
                short s2 = packAngle(MathHelper.clamp(f6, 1.0F, 89.0F));
                short s3 = packFraction(f8 - (float)Math.floor(f8));
                short s4 = packFraction(f9);
                GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                guiRenderState.addSimpleElement(
                    new Arrow3dRenderer.Arrow3dElement(
                        new Matrix3x2f(matrix3x2fc), f, f2, f + f3, f2 + f4, i2, s, s2, s3, s4, screenRect, screenRect2
                    )
                );
            }
        }
    }

    private static short packAngle(float f) {
        return (short)MathHelper.clamp(Math.round(f * 16.0F), 0, 32767);
    }

    private static short packFraction(float f) {
        return (short)MathHelper.clamp(Math.round(f * 10000.0F), 0, 32767);
    }

    record Arrow3dElement(
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int color,
        short yaw,
        short tilt,
        short shinePhase,
        short shineStrength,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            this.emitVertex(vertexConsumer, this.x1, this.y1, 0.0F, 0.0F);
            this.emitVertex(vertexConsumer, this.x1, this.y2, 0.0F, 1.0F);
            this.emitVertex(vertexConsumer, this.x2, this.y2, 1.0F, 1.0F);
            this.emitVertex(vertexConsumer, this.x2, this.y1, 1.0F, 0.0F);
        }

        private void emitVertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4) {
            vertexConsumer.vertex(this.pose, f, f2)
                .color(this.color)
                .texture(f3, f4)
                .overlay(this.yaw, this.tilt)
                .light(this.shinePhase, this.shineStrength)
                .normal(0.0F, 0.0F, 0.0F)
                .lineWidth(0.0F);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.ARROW3D_GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.empty();
        }
    }
}
