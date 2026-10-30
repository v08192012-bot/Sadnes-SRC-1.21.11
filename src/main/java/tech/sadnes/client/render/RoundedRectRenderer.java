package tech.sadnes.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Arrays;
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

public final class RoundedRectRenderer {
    public static final int MODE_FILL = 0;
    public static final int MODE_OUTLINE = 1;
    public static final int MODE_SOFT = 2;
    public static final int MODE_BORDER = 3;
    public static final int MODE_GLOW = 4;
    private static final float RADIUS_SCALE = 16.0F;
    private static RoundedRectRenderer.Batch activeBatch;

    private RoundedRectRenderer() {
    }

    public static RoundedRectRenderer.Batch createBatch() {
        return new RoundedRectRenderer.Batch();
    }

    public static void beginBatch(RoundedRectRenderer.Batch batch) {
        activeBatch = batch;
    }

    public static void endBatch() {
        activeBatch = null;
    }

    public static void draw(
        DrawContext drawContext,
        Matrix3x2fc matrix3x2fc,
        float f,
        float f2,
        float f3,
        float f4,
        Radius radius,
        float f5,
        float f6,
        float f7,
        int i,
        int i2,
        int i3,
        int i4,
        int i5
    ) {
        if (drawContext != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f7 <= 5.0E-4F)) {
            float f8 = i5 == 4 ? f6 : f5;
            float f9 = matrix3x2fc.m00();
            float f10 = matrix3x2fc.m01();
            float f11 = MinecraftClient.getInstance().getWindow().getScaleFactor() * (float)Math.sqrt(f9 * f9 + f10 * f10);
            ScreenRect screenRect = GuiBoundsUtil.getScissor(drawContext);
            ScreenRect screenRect2 = GuiBoundsUtil.computeBounds(f, f2, f + f3, f2 + f4, matrix3x2fc, screenRect);
            if (screenRect2 != null) {
                int i6 = applyAlpha(i, f7);
                int i7 = applyAlpha(i2, f7);
                int i8 = applyAlpha(i3, f7);
                int i9 = applyAlpha(i4, f7);
                if (VerticalFadeMask.isActive()) {
                    i6 = VerticalFadeMask.applyAlpha(i6, f2);
                    i9 = VerticalFadeMask.applyAlpha(i9, f2);
                    i7 = VerticalFadeMask.applyAlpha(i7, f2 + f4);
                    i8 = VerticalFadeMask.applyAlpha(i8, f2 + f4);
                }

                short s = packRadius(radius.getTopLeft() * f11, radius.isSmooth());
                short s2 = packRadius(radius.getTopRight() * f11, radius.isSmooth());
                short s3 = packRadius(radius.getBottomRight() * f11, radius.isSmooth());
                short s4 = packRadius(radius.getBottomLeft() * f11, radius.isSmooth());
                float f12 = f8 * f11;
                float f13 = modeValue(i5);
                if (activeBatch != null) {
                    activeBatch.add(
                        matrix3x2fc, screenRect, screenRect2, f, f2, f + f3, f2 + f4, i6, i7, i8, i9, s, s2, s3, s4, f12, f13
                    );
                } else {
                    GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                    guiRenderState.addSimpleElement(
                        new RoundedRectRenderer.RectElement(
                            new Matrix3x2f(matrix3x2fc),
                            f,
                            f2,
                            f + f3,
                            f2 + f4,
                            i6,
                            i7,
                            i8,
                            i9,
                            s,
                            s2,
                            s3,
                            s4,
                            f12,
                            f13,
                            screenRect,
                            screenRect2
                        )
                    );
                }
            }
        }
    }

    private static int applyAlpha(int i, float f) {
        int i2 = Math.round(ColorUtil.alpha(i) * MathHelper.clamp(f, 0.0F, 1.0F));
        return ColorUtil.setAlpha(i, i2);
    }

    static short packRadius(float f, boolean flag) {
        int i = MathHelper.clamp(Math.round(Math.max(0.0F, f) * 16.0F), 0, 32767);
        return flag ? (short)(-i) : (short)i;
    }

    private static float modeValue(int i) {
        switch (i) {
            case 1:
                return 0.0F;
            case 2:
                return -0.5F;
            case 3:
                return 0.25F;
            case 4:
                return 1.0F;
            default:
                return -1.0F;
        }
    }

    static void emitQuad(
        VertexConsumer vertexConsumer,
        Matrix3x2f matrix3x2f,
        float f,
        float f2,
        float f3,
        float f4,
        int i,
        int i2,
        int i3,
        int i4,
        short s,
        short s2,
        short s3,
        short s4,
        float f5,
        float f6
    ) {
        emitVertex(vertexConsumer, matrix3x2f, f, f2, 0.0F, 0.0F, i, s, s2, s3, s4, f5, f6);
        emitVertex(vertexConsumer, matrix3x2f, f, f4, 0.0F, 1.0F, i2, s, s2, s3, s4, f5, f6);
        emitVertex(vertexConsumer, matrix3x2f, f3, f4, 1.0F, 1.0F, i3, s, s2, s3, s4, f5, f6);
        emitVertex(vertexConsumer, matrix3x2f, f3, f2, 1.0F, 0.0F, i4, s, s2, s3, s4, f5, f6);
    }

    private static void emitVertex(
        VertexConsumer vertexConsumer,
        Matrix3x2f matrix3x2f,
        float f,
        float f2,
        float f3,
        float f4,
        int i,
        short s,
        short s2,
        short s3,
        short s4,
        float f5,
        float f6
    ) {
        vertexConsumer.vertex(matrix3x2f, f, f2).color(i).texture(f3, f4).overlay(s, s2).light(s3, s4).normal(f6, 0.0F, 0.0F).lineWidth(f5);
    }

    public static final class Batch {
        private Matrix3x2f pose;
        private ScreenRect scissor;
        private boolean hasScissor;
        private float[] coords = new float[256];
        private int[] colors = new int[256];
        private short[] radii = new short[256];
        private float[] extra = new float[128];
        private int quadCount;
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private boolean hasBounds;

        void add(
            Matrix3x2fc matrix3x2fc,
            ScreenRect screenRect,
            ScreenRect screenRect2,
            float f,
            float f2,
            float f3,
            float f4,
            int i,
            int i2,
            int i3,
            int i4,
            short s,
            short s2,
            short s3,
            short s4,
            float f5,
            float f6
        ) {
            if (this.pose == null) {
                this.pose = new Matrix3x2f(matrix3x2fc);
            }

            if (!this.hasScissor) {
                this.scissor = screenRect;
                this.hasScissor = true;
            }

            this.ensureCapacity();
            int i5 = this.quadCount * 4;
            this.coords[i5] = f;
            this.coords[i5 + 1] = f2;
            this.coords[i5 + 2] = f3;
            this.coords[i5 + 3] = f4;
            this.colors[i5] = i;
            this.colors[i5 + 1] = i2;
            this.colors[i5 + 2] = i3;
            this.colors[i5 + 3] = i4;
            this.radii[i5] = s;
            this.radii[i5 + 1] = s2;
            this.radii[i5 + 2] = s3;
            this.radii[i5 + 3] = s4;
            int i6 = this.quadCount * 2;
            this.extra[i6] = f5;
            this.extra[i6 + 1] = f6;
            this.quadCount++;
            int i7 = screenRect2.getLeft();
            int i8 = screenRect2.getTop();
            int i9 = screenRect2.getRight();
            int i10 = screenRect2.getBottom();
            if (!this.hasBounds) {
                this.minX = i7;
                this.minY = i8;
                this.maxX = i9;
                this.maxY = i10;
                this.hasBounds = true;
            } else {
                if (i7 < this.minX) {
                    this.minX = i7;
                }

                if (i8 < this.minY) {
                    this.minY = i8;
                }

                if (i9 > this.maxX) {
                    this.maxX = i9;
                }

                if (i10 > this.maxY) {
                    this.maxY = i10;
                }
            }
        }

        public void flush(DrawContext drawContext) {
            if (this.quadCount != 0 && drawContext != null) {
                int i = this.quadCount;
                float[] floats = Arrays.copyOf(this.coords, i * 4);
                int[] ints = Arrays.copyOf(this.colors, i * 4);
                short[] shorts = Arrays.copyOf(this.radii, i * 4);
                float[] floats2 = Arrays.copyOf(this.extra, i * 2);
                ScreenRect screenRect = new ScreenRect(this.minX, this.minY, this.maxX - this.minX, this.maxY - this.minY);
                GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                guiRenderState.addSimpleElement(new RoundedRectRenderer.BatchedElement(this.pose, floats, ints, shorts, floats2, i, this.scissor, screenRect));
                this.reset();
            } else {
                this.reset();
            }
        }

        private void ensureCapacity() {
            if ((this.quadCount + 1) * 4 > this.coords.length) {
                int i = this.coords.length / 4 * 2;
                this.coords = Arrays.copyOf(this.coords, i * 4);
                this.colors = Arrays.copyOf(this.colors, i * 4);
                this.radii = Arrays.copyOf(this.radii, i * 4);
                this.extra = Arrays.copyOf(this.extra, i * 2);
            }
        }

        private void reset() {
            this.quadCount = 0;
            this.pose = null;
            this.scissor = null;
            this.hasScissor = false;
            this.hasBounds = false;
        }
    }

    record BatchedElement(Matrix3x2f pose, float[] coords, int[] colors, short[] radii, float[] extra, int quadCount, ScreenRect scissorArea, ScreenRect bounds)
        implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            for (int i = 0; i < this.quadCount; i++) {
                int i2 = i * 4;
                int i3 = i * 2;
                RoundedRectRenderer.emitQuad(
                    vertexConsumer,
                    this.pose,
                    this.coords[i2],
                    this.coords[i2 + 1],
                    this.coords[i2 + 2],
                    this.coords[i2 + 3],
                    this.colors[i2],
                    this.colors[i2 + 1],
                    this.colors[i2 + 2],
                    this.colors[i2 + 3],
                    this.radii[i2],
                    this.radii[i2 + 1],
                    this.radii[i2 + 2],
                    this.radii[i2 + 3],
                    this.extra[i3],
                    this.extra[i3 + 1]
                );
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.ROUNDED_GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.empty();
        }
    }

    record RectElement(
        Matrix3x2f pose,
        float x1,
        float y1,
        float x2,
        float y2,
        int colorTopLeft,
        int colorBottomLeft,
        int colorBottomRight,
        int colorTopRight,
        short radiusLeftBottom,
        short radiusLeftTop,
        short radiusRightBottom,
        short radiusRightTop,
        float param,
        float typeSignal,
        ScreenRect scissorArea,
        ScreenRect bounds
    ) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer vertexConsumer) {
            RoundedRectRenderer.emitQuad(
                vertexConsumer,
                this.pose,
                this.x1,
                this.y1,
                this.x2,
                this.y2,
                this.colorTopLeft,
                this.colorBottomLeft,
                this.colorBottomRight,
                this.colorTopRight,
                this.radiusLeftBottom,
                this.radiusLeftTop,
                this.radiusRightBottom,
                this.radiusRightTop,
                this.param,
                this.typeSignal
            );
        }

        @Override
        public RenderPipeline pipeline() {
            return Pipelines.ROUNDED_GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.empty();
        }
    }
}
