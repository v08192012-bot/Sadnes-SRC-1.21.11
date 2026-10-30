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

public final class GlassRenderer {
    private static final float RADIUS_SCALE = 16.0F;
    private static final float FRACTION_SCALE = 8192.0F;

    private GlassRenderer() {
    }

    public static void draw(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        Radius radius,
        float f5,
        int i,
        float f6,
        float f7,
        int i2,
        float f8,
        boolean flag
    ) {
        if (drawContext != null && textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f5 <= 0.0F)) {
            Matrix3x2f matrix3x2f = new Matrix3x2f(matrix3x2fc);
            float f9 = MinecraftClient.getInstance().getWindow().getScaleFactor() * matrix3x2f.m00();
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2f, screenRect);
            if (screenRect2 != null) {
                int i3 = clampByte(Math.round(MathHelper.clamp(f6, 0.0F, 1.0F) * 255.0F)) << 24 | i & 16777215;
                GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                guiRenderState.addSimpleElement(
                    new GlassRenderer.GlassElement(
                        textureSetup,
                        matrix3x2f,
                        f,
                        f2,
                        f + f3,
                        f2 + f4,
                        i3,
                        packRadius(radius.getMax() * f9, radius.isSmooth()),
                        packFraction(f7),
                        packFraction(f8),
                        ColorUtil.redf(i2),
                        ColorUtil.greenf(i2),
                        ColorUtil.bluef(i2),
                        f5 * f9,
                        flag,
                        screenRect,
                        screenRect2
                    )
                );
            }
        }
    }

    private static int clampByte(int i) {
        return MathHelper.clamp(i, 0, 255);
    }

    private static short packRadius(float f, boolean flag) {
        short s = (short)MathHelper.clamp(Math.round(Math.max(0.0F, f) * 16.0F), 0, 32767);
        return flag ? (short)(-s) : s;
    }

    private static short packFraction(float f) {
        return (short)MathHelper.clamp(Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 8192.0F), 0, 32767);
    }

    record GlassElement(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int color,
        short radius,
        short tintAmount,
        short shadowStrength,
        float shadowR,
        float shadowG,
        float shadowB,
        float spread,
        boolean include2D,
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
                .overlay(this.radius, this.tintAmount)
                .light(this.shadowStrength, 0)
                .normal(this.shadowR, this.shadowG, this.shadowB)
                .lineWidth(this.spread);
        }

        @Override
        public RenderPipeline pipeline() {
            return this.include2D ? Pipelines.GLASS_2D_BLUR : Pipelines.GLASS;
        }
    }
}
