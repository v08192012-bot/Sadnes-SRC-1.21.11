package tech.sadnes.client.render.world;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.sadnes.client.render.MeshDrawUtil;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.util.color.ColorUtil;

public final class StyledBoxRenderer {
    private static final float BASE_WIDTH = 1.0F;
    private static final int GLOW_LAYERS = 3;
    private static final float GLOW_WIDTH_FACTOR = 1.6F;
    private static final float GLOW_ALPHA = 0.05F;
    private static final float CORNER_LENGTH = 0.25F;
    private static final float GRADIENT_ALPHA = 0.25F;
    private static final float FRAME_INSET_RATIO = 0.12F;
    private static final float MAX_FRAME_INSET = 0.15F;
    private static final int MAX_BOXES_PER_BATCH = 60000;

    private StyledBoxRenderer() {
    }

    public static StyledBoxRenderer.Batch batch(StyledBoxRenderer.Style style) {
        return new StyledBoxRenderer.Batch(style);
    }

    public static void drawBox(MatrixStack matrixStack, Box box, StyledBoxRenderer.Style style, int i) {
        Vec3d vec3d = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();

        try (StyledBoxRenderer.Batch batch2 = new StyledBoxRenderer.Batch(style)) {
            batch2.add(
                matrixStack,
                (float)(box.minX - vec3d.x),
                (float)(box.minY - vec3d.y),
                (float)(box.minZ - vec3d.z),
                (float)(box.maxX - vec3d.x),
                (float)(box.maxY - vec3d.y),
                (float)(box.maxZ - vec3d.z),
                i
            );
            batch2.flush();
        }
    }

    static void emitEdges(BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        emitGlowLine(bufferBuilder, entry, f, f2, f3, f4, f2, f3, i);
        emitGlowLine(bufferBuilder, entry, f4, f2, f3, f4, f2, f6, i);
        emitGlowLine(bufferBuilder, entry, f4, f2, f6, f, f2, f6, i);
        emitGlowLine(bufferBuilder, entry, f, f2, f6, f, f2, f3, i);
        emitGlowLine(bufferBuilder, entry, f, f5, f3, f4, f5, f3, i);
        emitGlowLine(bufferBuilder, entry, f4, f5, f3, f4, f5, f6, i);
        emitGlowLine(bufferBuilder, entry, f4, f5, f6, f, f5, f6, i);
        emitGlowLine(bufferBuilder, entry, f, f5, f6, f, f5, f3, i);
        emitGlowLine(bufferBuilder, entry, f, f2, f3, f, f5, f3, i);
        emitGlowLine(bufferBuilder, entry, f4, f2, f3, f4, f5, f3, i);
        emitGlowLine(bufferBuilder, entry, f4, f2, f6, f4, f5, f6, i);
        emitGlowLine(bufferBuilder, entry, f, f2, f6, f, f5, f6, i);
    }

    static void emitCorners(BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        emitCornerSegment(bufferBuilder, entry, f, f2, f3, f4, f2, f3, i);
        emitCornerSegment(bufferBuilder, entry, f4, f2, f3, f4, f2, f6, i);
        emitCornerSegment(bufferBuilder, entry, f4, f2, f6, f, f2, f6, i);
        emitCornerSegment(bufferBuilder, entry, f, f2, f6, f, f2, f3, i);
        emitCornerSegment(bufferBuilder, entry, f, f5, f3, f4, f5, f3, i);
        emitCornerSegment(bufferBuilder, entry, f4, f5, f3, f4, f5, f6, i);
        emitCornerSegment(bufferBuilder, entry, f4, f5, f6, f, f5, f6, i);
        emitCornerSegment(bufferBuilder, entry, f, f5, f6, f, f5, f3, i);
        emitCornerSegment(bufferBuilder, entry, f, f2, f3, f, f5, f3, i);
        emitCornerSegment(bufferBuilder, entry, f4, f2, f3, f4, f5, f3, i);
        emitCornerSegment(bufferBuilder, entry, f4, f2, f6, f4, f5, f6, i);
        emitCornerSegment(bufferBuilder, entry, f, f2, f6, f, f5, f6, i);
    }

    static void emitFrame(BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        float f7 = Math.min(0.15F, (f4 - f) * 0.12F);
        float f8 = Math.min(0.15F, (f5 - f2) * 0.12F);
        float f9 = Math.min(0.15F, (f6 - f3) * 0.12F);
        float f10 = f + f7;
        float f11 = f2 + f8;
        float f12 = f3 + f9;
        float f13 = f4 - f7;
        float f14 = f5 - f8;
        float f15 = f6 - f9;
        emitEdges(bufferBuilder, entry, f, f2, f3, f4, f5, f6, i);
        emitEdges(bufferBuilder, entry, f10, f11, f12, f13, f14, f15, i);
        emitGlowLine(bufferBuilder, entry, f, f2, f3, f10, f11, f12, i);
        emitGlowLine(bufferBuilder, entry, f4, f2, f3, f13, f11, f12, i);
        emitGlowLine(bufferBuilder, entry, f4, f2, f6, f13, f11, f15, i);
        emitGlowLine(bufferBuilder, entry, f, f2, f6, f10, f11, f15, i);
        emitGlowLine(bufferBuilder, entry, f, f5, f3, f10, f14, f12, i);
        emitGlowLine(bufferBuilder, entry, f4, f5, f3, f13, f14, f12, i);
        emitGlowLine(bufferBuilder, entry, f4, f5, f6, f13, f14, f15, i);
        emitGlowLine(bufferBuilder, entry, f, f5, f6, f10, f14, f15, i);
    }

    private static void emitCornerSegment(BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        float f7 = (f4 - f) * 0.25F;
        float f8 = (f5 - f2) * 0.25F;
        float f9 = (f6 - f3) * 0.25F;
        emitGlowLine(bufferBuilder, entry, f, f2, f3, f + f7, f2 + f8, f3 + f9, i);
        emitGlowLine(bufferBuilder, entry, f4, f5, f6, f4 - f7, f5 - f8, f6 - f9, i);
    }

    private static void emitGlowLine(BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        float f7 = f4 - f;
        float f8 = f5 - f2;
        float f9 = f6 - f3;
        float f10 = 1.0F / (float)Math.sqrt(f7 * f7 + f8 * f8 + f9 * f9);
        float f11 = f7 * f10;
        float f12 = f8 * f10;
        float f13 = f9 * f10;

        for (int i2 = 3; i2 >= 1; i2--) {
            float f14 = 1.0F * (float)Math.pow(1.6F, i2);
            emitLine(bufferBuilder, entry, f, f2, f3, f4, f5, f6, f11, f12, f13, ColorUtil.multiplyAlpha(i, 0.05F / i2), f14);
        }

        emitLine(bufferBuilder, entry, f, f2, f3, f4, f5, f6, f11, f12, f13, i, 1.0F);
    }

    private static void emitLine(
        BufferBuilder bufferBuilder,
        Entry entry,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        int i,
        float f10
    ) {
        bufferBuilder.vertex(entry, f, f2, f3).color(i).normal(entry, f7, f8, f9).lineWidth(f10);
        bufferBuilder.vertex(entry, f4, f5, f6).color(i).normal(entry, f7, f8, f9).lineWidth(f10);
    }

    static void emitGradientFill(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        int i2 = ColorUtil.multiplyAlpha(i, 0.25F);
        int i3 = ColorUtil.multiplyAlpha(i, 0.0F);
        emitVertex(bufferBuilder, matrix4f, f, f2, f3, i2);
        emitVertex(bufferBuilder, matrix4f, f4, f2, f3, i2);
        emitVertex(bufferBuilder, matrix4f, f4, f2, f6, i2);
        emitVertex(bufferBuilder, matrix4f, f, f2, f6, i2);
        emitVertex(bufferBuilder, matrix4f, f, f5, f6, i3);
        emitVertex(bufferBuilder, matrix4f, f4, f5, f6, i3);
        emitVertex(bufferBuilder, matrix4f, f4, f5, f3, i3);
        emitVertex(bufferBuilder, matrix4f, f, f5, f3, i3);
        emitVertex(bufferBuilder, matrix4f, f, f2, f3, i2);
        emitVertex(bufferBuilder, matrix4f, f, f5, f3, i3);
        emitVertex(bufferBuilder, matrix4f, f4, f5, f3, i3);
        emitVertex(bufferBuilder, matrix4f, f4, f2, f3, i2);
        emitVertex(bufferBuilder, matrix4f, f4, f2, f6, i2);
        emitVertex(bufferBuilder, matrix4f, f4, f5, f6, i3);
        emitVertex(bufferBuilder, matrix4f, f, f5, f6, i3);
        emitVertex(bufferBuilder, matrix4f, f, f2, f6, i2);
        emitVertex(bufferBuilder, matrix4f, f, f2, f6, i2);
        emitVertex(bufferBuilder, matrix4f, f, f5, f6, i3);
        emitVertex(bufferBuilder, matrix4f, f, f5, f3, i3);
        emitVertex(bufferBuilder, matrix4f, f, f2, f3, i2);
        emitVertex(bufferBuilder, matrix4f, f4, f2, f3, i2);
        emitVertex(bufferBuilder, matrix4f, f4, f5, f3, i3);
        emitVertex(bufferBuilder, matrix4f, f4, f5, f6, i3);
        emitVertex(bufferBuilder, matrix4f, f4, f2, f6, i2);
    }

    private static void emitVertex(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, int i) {
        bufferBuilder.vertex(matrix4f, f, f2, f3).color(i);
    }

    public static final class Batch implements AutoCloseable {
        private final StyledBoxRenderer.Style style;
        private BufferAllocator lineAllocator;
        private BufferBuilder lineBuilder;
        private BufferAllocator fillAllocator;
        private BufferBuilder fillBuilder;
        private int boxCount;

        Batch(StyledBoxRenderer.Style style2) {
            this.style = style2;
        }

        public void add(MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i) {
            if (this.boxCount >= 60000) {
                this.flush();
            }

            Entry entry = matrixStack.peek();
            if (this.style == StyledBoxRenderer.Style.GRADIENT) {
                StyledBoxRenderer.emitGradientFill(this.getFillBuilder(), entry.getPositionMatrix(), f, f2, f3, f4, f5, f6, i);
            }

            BufferBuilder bufferBuilder = this.getLineBuilder();
            switch (this.style) {
                case CORNERS:
                    StyledBoxRenderer.emitCorners(bufferBuilder, entry, f, f2, f3, f4, f5, f6, i);
                    this.boxCount++;
                    return;
                case GRADIENT:
                    StyledBoxRenderer.emitEdges(bufferBuilder, entry, f, f2, f3, f4, f5, f6, i);
                    this.boxCount++;
                    return;
                case FRAME:
                    StyledBoxRenderer.emitFrame(bufferBuilder, entry, f, f2, f3, f4, f5, f6, i);
                default:
                    this.boxCount++;
            }
        }

        public void flush() {
            if (this.fillBuilder != null) {
                BuiltBuffer builtBuffer = this.fillBuilder.endNullable();
                if (builtBuffer != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.BOX_QUADS, builtBuffer, "styled box fill");
                }

                this.fillBuilder = null;
            }

            if (this.lineBuilder != null) {
                BuiltBuffer builtBuffer2 = this.lineBuilder.endNullable();
                if (builtBuffer2 != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.LINES_NO_DEPTH, builtBuffer2, "styled box lines");
                }

                this.lineBuilder = null;
            }

            this.boxCount = 0;
        }

        @Override
        public void close() {
            if (this.fillAllocator != null) {
                this.fillAllocator.close();
            }

            if (this.lineAllocator != null) {
                this.lineAllocator.close();
            }
        }

        private BufferBuilder getFillBuilder() {
            if (this.fillBuilder == null) {
                if (this.fillAllocator == null) {
                    this.fillAllocator = new BufferAllocator(8192);
                }

                this.fillBuilder = new BufferBuilder(this.fillAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            }

            return this.fillBuilder;
        }

        private BufferBuilder getLineBuilder() {
            if (this.lineBuilder == null) {
                if (this.lineAllocator == null) {
                    this.lineAllocator = new BufferAllocator(8192);
                }

                this.lineBuilder = new BufferBuilder(this.lineAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
            }

            return this.lineBuilder;
        }
    }

    public enum Style {
        CORNERS,
        GRADIENT,
        FRAME;
    }
}
