package tech.sadnes.client.render.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import tech.sadnes.api.mixin.accessor.DrawContextAccessor;
import tech.sadnes.client.render.GuiBoundsUtil;
import tech.sadnes.client.render.shader.Pipelines;

final class MsdfGlyphBatch {
    private MsdfGlyphBatch() {
    }

    static void submit(
        DrawContext drawContext,
        TextureSetup textureSetup,
        Matrix3x2fc matrix3x2fc,
        float[] floats,
        int[] ints,
        int i,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7
    ) {
        if (drawContext != null && textureSetup != null && i > 0) {
            Matrix3x2f matrix3x2f = new Matrix3x2f(matrix3x2fc);
            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f3, f4, matrix3x2f, screenRect);
            if (screenRect2 != null) {
                guiRenderState.addSimpleElement(new MsdfGlyphBatch.GlyphRenderState(textureSetup, matrix3x2f, floats, ints, i, toShort(f5), toShort(f6), f7, screenRect, screenRect2));
            }
        }
    }

    private static short toShort(float f) {
        return (short)Math.max(0, Math.min(32767, Math.round(f)));
    }

    record GlyphRenderState(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float[] geom,
        int[] colors,
        int quadCount,
        short atlasWidth,
        short atlasHeight,
        float distanceRange,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            for (int i = 0; i < this.quadCount; i++) {
                int i2 = i * 8;
                float f = this.geom[i2];
                float f2 = this.geom[i2 + 1];
                float f3 = this.geom[i2 + 2];
                float f4 = this.geom[i2 + 3];
                float f5 = this.geom[i2 + 4];
                float f6 = this.geom[i2 + 5];
                float f7 = this.geom[i2 + 6];
                float f8 = this.geom[i2 + 7];
                int i3 = this.colors[i];
                this.emit(vertexConsumer, f, f2, f5, f6, i3);
                this.emit(vertexConsumer, f, f4, f5, f8, i3);
                this.emit(vertexConsumer, f3, f4, f7, f8, i3);
                this.emit(vertexConsumer, f3, f2, f7, f6, i3);
            }
        }

        private void emit(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int i) {
            vertexConsumer.vertex(this.pose, f, f2).texture(f3, f4).color(i).light(this.atlasWidth, this.atlasHeight).lineWidth(this.distanceRange);
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.MSDF_FONT_GUI;
        }
    }
}
