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
import tech.sadnes.client.ui.clickgui.component.SliderDrop;
import tech.sadnes.client.util.color.ColorUtil;

public final class LiquidSliderRenderer {
    private LiquidSliderRenderer() {
    }

    public static void draw(
        DrawContext drawContext, Matrix3x2fc matrix3x2fc, float f, float f2, float f3, float f4, int i, float f5, SliderDrop sliderDrop, float f6
    ) {
        if (drawContext != null && sliderDrop != null && !(f5 <= 5.0E-4F) && !(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2fc, screenRect);
            if (screenRect2 != null) {
                int i2 = ColorUtil.setAlpha(i, Math.round(ColorUtil.alpha(i) * MathHelper.clamp(f5, 0.0F, 1.0F)));
                GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                guiRenderState.addSimpleElement(
                    new LiquidSliderRenderer.LiquidSliderElement(
                        new Matrix3x2f(matrix3x2fc),
                        f,
                        f2,
                        f + f3,
                        f2 + f4,
                        i2,
                        packUnit(sliderDrop.getWaveAge()),
                        packUnit((sliderDrop.getPos() + 0.5F) * 0.5F),
                        packUnit(sliderDrop.getVelocity() * 0.5F + 0.5F),
                        packUnit((sliderDrop.getWaveAt() + 0.5F) * 0.5F),
                        MathHelper.clamp(sliderDrop.getHover(), 0.0F, 1.0F),
                        MathHelper.clamp(sliderDrop.getPress(), 0.0F, 1.0F),
                        MathHelper.clamp(sliderDrop.getPulseAge(), 0.0F, 1.0F),
                        f6,
                        screenRect,
                        screenRect2
                    )
                );
            }
        }
    }

    private static short packUnit(float f) {
        return (short)MathHelper.clamp(Math.round(f * 10000.0F), 0, 10000);
    }

    record LiquidSliderElement(
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int color,
        short waveAge,
        short pos,
        short velocity,
        short waveAt,
        float hover,
        float press,
        float pulseAge,
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
                .overlay(this.waveAge, this.pos)
                .light(this.velocity, this.waveAt)
                .normal(this.hover, this.press, this.pulseAge)
                .lineWidth(this.time);
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.empty();
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.LIQUID_SLIDER_GUI;
        }
    }
}
