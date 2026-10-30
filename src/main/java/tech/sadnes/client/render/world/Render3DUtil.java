package tech.sadnes.client.render.world;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
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

public final class Render3DUtil {
    private static final BufferAllocator BUFFER = new BufferAllocator(2048) {
        @Override
        public void close() {
            this.reset();
        }
    };
    public static final float LINE_WIDTH = 2.0F;

    public static void drawBox(MatrixStack matrixStack, Box box, int i, int i2) {
        drawFilledBox(matrixStack, box, i2);
        drawOutlinedBox(matrixStack, box, i);
    }

    public static void drawGlowBox(MatrixStack matrixStack, Box box, int i, int i2) {
        drawFilledBox(matrixStack, box, i2);
        drawGlowOutlinedBox(matrixStack, box, i);
    }

    public static void drawDepthBox(MatrixStack matrixStack, Box box, int i, int i2) {
        Vec3d vec3d = getCameraPos();
        float f = (float)(box.minX - vec3d.x);
        float f2 = (float)(box.minY - vec3d.y);
        float f3 = (float)(box.minZ - vec3d.z);
        float f4 = (float)(box.maxX - vec3d.x);
        float f5 = (float)(box.maxY - vec3d.y);
        float f6 = (float)(box.maxZ - vec3d.z);
        drawFilledWithPipeline(matrixStack, f, f2, f3, f4, f5, f6, i2, Pipelines.BOX_QUADS_DEPTH);
        drawOutlineWithPipeline(matrixStack, f, f2, f3, f4, f5, f6, i, 2.0F, Pipelines.AA_LINE_DEPTH);
    }

    public static void drawFilledBox(MatrixStack matrixStack, Box box, int i) {
        Vec3d vec3d = getCameraPos();
        drawFilled(
            matrixStack,
            (float)(box.minX - vec3d.x),
            (float)(box.minY - vec3d.y),
            (float)(box.minZ - vec3d.z),
            (float)(box.maxX - vec3d.x),
            (float)(box.maxY - vec3d.y),
            (float)(box.maxZ - vec3d.z),
            i
        );
    }

    public static void drawOutlinedBox(MatrixStack matrixStack, Box box, int i) {
        Vec3d vec3d = getCameraPos();
        drawOutline(
            matrixStack,
            (float)(box.minX - vec3d.x),
            (float)(box.minY - vec3d.y),
            (float)(box.minZ - vec3d.z),
            (float)(box.maxX - vec3d.x),
            (float)(box.maxY - vec3d.y),
            (float)(box.maxZ - vec3d.z),
            i
        );
    }

    public static void drawGlowOutlinedBox(MatrixStack matrixStack, Box box, int i) {
        Vec3d vec3d = getCameraPos();
        drawOutlineWithPipeline(
            matrixStack,
            (float)(box.minX - vec3d.x),
            (float)(box.minY - vec3d.y),
            (float)(box.minZ - vec3d.z),
            (float)(box.maxX - vec3d.x),
            (float)(box.maxY - vec3d.y),
            (float)(box.maxZ - vec3d.z),
            i,
            2.0F,
            Pipelines.AA_LINE_GLOW
        );
    }

    public static void drawBox(
        MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, boolean flag, boolean flag2
    ) {
        if (flag2) {
            drawFilled(matrixStack, f, f2, f3, f4, f5, f6, i2);
        }

        if (flag) {
            drawOutline(matrixStack, f, f2, f3, f4, f5, f6, i);
        }
    }

    public static void drawOutline(MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        drawOutline(matrixStack, f, f2, f3, f4, f5, f6, i, 2.0F);
    }

    public static void drawOutline(MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i, float f7) {
        drawOutlineWithPipeline(matrixStack, f, f2, f3, f4, f5, f6, i, f7, Pipelines.LINES_NO_DEPTH);
    }

    private static void drawOutlineWithPipeline(
        MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i, float f7, RenderPipeline renderPipeline
    ) {
        try (BufferAllocator bufferAllocator = BUFFER) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
            emitOutline(bufferBuilder, matrixStack.peek(), f, f2, f3, f4, f5, f6, i, f7);
            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                drawMesh(renderPipeline, builtBuffer);
            }
        }
    }

    public static void drawLines(MatrixStack matrixStack, List<Vec3d> vec3ds, int i, float f) {
        drawLinesInternal(matrixStack, vec3ds, i, f, Pipelines.AA_LINE);
    }

    public static void drawGlowLines(MatrixStack matrixStack, List<Vec3d> vec3ds, int i, float f) {
        drawLinesInternal(matrixStack, vec3ds, i, f, Pipelines.AA_LINE_GLOW);
    }

    public static void drawLines(MatrixStack matrixStack, List<Vec3d> vec3ds, int i, float f, RenderPipeline renderPipeline) {
        if (vec3ds.size() >= 2) {
            try (BufferAllocator bufferAllocator = new BufferAllocator(vec3ds.size() * 64 + 256)) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                emitLines(bufferBuilder, matrixStack.peek(), vec3ds, i, f);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    drawMesh(renderPipeline, builtBuffer);
                }
            }
        }
    }

    public static void drawGradientLines(MatrixStack matrixStack, List<Vec3d> vec3ds, int[] ints, float[] floats, RenderPipeline renderPipeline) {
        int i = Math.min(vec3ds.size(), Math.min(ints.length, floats.length));
        if (i >= 2) {
            Vec3d vec3d = getCameraPos();

            try (BufferAllocator bufferAllocator = new BufferAllocator(i * 64 + 256)) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                Entry entry = matrixStack.peek();

                for (int i2 = 0; i2 < i - 1; i2++) {
                    Vec3d vec3d2 = (Vec3d)vec3ds.get(i2);
                    Vec3d vec3d3 = (Vec3d)vec3ds.get(i2 + 1);
                    if (!(vec3d2.squaredDistanceTo(vec3d3) < 1.0E-8)) {
                        emitGradientLine(
                            bufferBuilder,
                            entry,
                            (float)(vec3d2.x - vec3d.x),
                            (float)(vec3d2.y - vec3d.y),
                            (float)(vec3d2.z - vec3d.z),
                            (float)(vec3d3.x - vec3d.x),
                            (float)(vec3d3.y - vec3d.y),
                            (float)(vec3d3.z - vec3d.z),
                            ints[i2],
                            ints[i2 + 1],
                            floats[i2],
                            floats[i2 + 1]
                        );
                    }
                }

                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    drawMesh(renderPipeline, builtBuffer);
                }
            }
        }
    }

    public static void emitLines(BufferBuilder bufferBuilder, Entry entry, List<Vec3d> vec3ds, int i, float f) {
        int i2 = vec3ds.size();
        if (i2 >= 2) {
            Vec3d vec3d = getCameraPos();

            for (int i3 = 0; i3 < i2; i3++) {
                Vec3d vec3d2 = (Vec3d)vec3ds.get(i3);
                Vec3d vec3d3 = (Vec3d)vec3ds.get((i3 + 1) % i2);
                if (!(vec3d2.squaredDistanceTo(vec3d3) < 1.0E-8)) {
                    emitLine(
                        bufferBuilder,
                        entry,
                        (float)(vec3d2.x - vec3d.x),
                        (float)(vec3d2.y - vec3d.y),
                        (float)(vec3d2.z - vec3d.z),
                        (float)(vec3d3.x - vec3d.x),
                        (float)(vec3d3.y - vec3d.y),
                        (float)(vec3d3.z - vec3d.z),
                        i,
                        f
                    );
                }
            }
        }
    }

    private static void drawLinesInternal(MatrixStack matrixStack, List<Vec3d> vec3ds, int i, float f, RenderPipeline renderPipeline) {
        int i2 = vec3ds.size();
        if (i2 >= 2) {
            Vec3d vec3d = getCameraPos();

            try (BufferAllocator bufferAllocator = BUFFER) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                Entry entry = matrixStack.peek();
                int i3 = Math.max(1, i2 - 1);

                for (int i4 = 0; i4 < i2 - 1; i4++) {
                    Vec3d vec3d2 = (Vec3d)vec3ds.get(i4);
                    Vec3d vec3d3 = (Vec3d)vec3ds.get(i4 + 1);
                    if (!(vec3d2.squaredDistanceTo(vec3d3) < 1.0E-8)) {
                        int i5 = ColorUtil.multiplyAlpha(i, fadeFactor((float)i4 / i3));
                        int i6 = ColorUtil.multiplyAlpha(i, fadeFactor((float)(i4 + 1) / i3));
                        emitGradientLine(
                            bufferBuilder,
                            entry,
                            (float)(vec3d2.x - vec3d.x),
                            (float)(vec3d2.y - vec3d.y),
                            (float)(vec3d2.z - vec3d.z),
                            (float)(vec3d3.x - vec3d.x),
                            (float)(vec3d3.y - vec3d.y),
                            (float)(vec3d3.z - vec3d.z),
                            i5,
                            i6,
                            f,
                            f
                        );
                    }
                }

                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    drawMesh(renderPipeline, builtBuffer);
                }
            }
        }
    }

    public static void drawFilled(MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        drawFilledWithPipeline(matrixStack, f, f2, f3, f4, f5, f6, i, Pipelines.BOX_QUADS);
    }

    private static void drawFilledWithPipeline(
        MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i, RenderPipeline renderPipeline
    ) {
        try (BufferAllocator bufferAllocator = BUFFER) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            emitFilled(bufferBuilder, matrixStack.peek().getPositionMatrix(), f, f2, f3, f4, f5, f6, i);
            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                drawMesh(renderPipeline, builtBuffer);
            }
        }
    }

    public static void emitOutline(BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i, float f7) {
        emitLine(bufferBuilder, entry, f, f2, f3, f4, f2, f3, i, f7);
        emitLine(bufferBuilder, entry, f4, f2, f3, f4, f2, f6, i, f7);
        emitLine(bufferBuilder, entry, f4, f2, f6, f, f2, f6, i, f7);
        emitLine(bufferBuilder, entry, f, f2, f6, f, f2, f3, i, f7);
        emitLine(bufferBuilder, entry, f, f5, f3, f4, f5, f3, i, f7);
        emitLine(bufferBuilder, entry, f4, f5, f3, f4, f5, f6, i, f7);
        emitLine(bufferBuilder, entry, f4, f5, f6, f, f5, f6, i, f7);
        emitLine(bufferBuilder, entry, f, f5, f6, f, f5, f3, i, f7);
        emitLine(bufferBuilder, entry, f, f2, f3, f, f5, f3, i, f7);
        emitLine(bufferBuilder, entry, f4, f2, f3, f4, f5, f3, i, f7);
        emitLine(bufferBuilder, entry, f4, f2, f6, f4, f5, f6, i, f7);
        emitLine(bufferBuilder, entry, f, f2, f6, f, f5, f6, i, f7);
    }

    public static void emitFilled(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        emitFilledQuads(bufferBuilder, matrix4f, f, f2, f3, f4, f5, f6, i);
    }

    public static Render3DUtil.BoxBatch boxBatch() {
        return new Render3DUtil.BoxBatch(Pipelines.LINES_NO_DEPTH);
    }

    public static Render3DUtil.BoxBatch glowBoxBatch() {
        return new Render3DUtil.BoxBatch(Pipelines.AA_LINE_GLOW);
    }

    public static Render3DUtil.LineBatch lineBatch() {
        return new Render3DUtil.LineBatch(Pipelines.AA_LINE);
    }

    public static Render3DUtil.LineBatch glowLineBatch() {
        return new Render3DUtil.LineBatch(Pipelines.AA_LINE_GLOW);
    }

    private static void emitLine(BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i, float f7) {
        emitGradientLine(bufferBuilder, entry, f, f2, f3, f4, f5, f6, i, i, f7, f7);
    }

    static void emitGradientLine(
        BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        float f9 = f4 - f;
        float f10 = f5 - f2;
        float f11 = f6 - f3;
        float f12 = 1.0F / (float)Math.sqrt(f9 * f9 + f10 * f10 + f11 * f11);
        float f13 = f9 * f12;
        float f14 = f10 * f12;
        float f15 = f11 * f12;
        bufferBuilder.vertex(entry, f, f2, f3).color(i).normal(entry, f13, f14, f15).lineWidth(f7);
        bufferBuilder.vertex(entry, f4, f5, f6).color(i2).normal(entry, f13, f14, f15).lineWidth(f8);
    }

    private static float fadeFactor(float f) {
        return Math.min(f / 0.33F, 1.0F);
    }

    private static void emitFilledQuads(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        bufferBuilder.vertex(matrix4f, f, f2, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f2, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f2, f6).color(i);
        bufferBuilder.vertex(matrix4f, f, f2, f6).color(i);
        bufferBuilder.vertex(matrix4f, f, f5, f6).color(i);
        bufferBuilder.vertex(matrix4f, f4, f5, f6).color(i);
        bufferBuilder.vertex(matrix4f, f4, f5, f3).color(i);
        bufferBuilder.vertex(matrix4f, f, f5, f3).color(i);
        bufferBuilder.vertex(matrix4f, f, f2, f3).color(i);
        bufferBuilder.vertex(matrix4f, f, f5, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f5, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f2, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f2, f6).color(i);
        bufferBuilder.vertex(matrix4f, f4, f5, f6).color(i);
        bufferBuilder.vertex(matrix4f, f, f5, f6).color(i);
        bufferBuilder.vertex(matrix4f, f, f2, f6).color(i);
        bufferBuilder.vertex(matrix4f, f, f2, f6).color(i);
        bufferBuilder.vertex(matrix4f, f, f5, f6).color(i);
        bufferBuilder.vertex(matrix4f, f, f5, f3).color(i);
        bufferBuilder.vertex(matrix4f, f, f2, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f2, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f5, f3).color(i);
        bufferBuilder.vertex(matrix4f, f4, f5, f6).color(i);
        bufferBuilder.vertex(matrix4f, f4, f2, f6).color(i);
    }

    static void drawMesh(RenderPipeline renderPipeline, BuiltBuffer builtBuffer) {
        MeshDrawUtil.drawLabeled(renderPipeline, builtBuffer, "sadnes 3d box");
    }

    static Vec3d getCameraPos() {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        return camera.getCameraPos();
    }

    public static final class BoxBatch implements AutoCloseable {
        private final RenderPipeline pipeline;
        private BufferAllocator fillAllocator;
        private BufferBuilder fillBuilder;
        private BufferAllocator lineAllocator;
        private BufferBuilder lineBuilder;

        BoxBatch(RenderPipeline renderPipeline) {
            this.pipeline = renderPipeline;
        }

        public void fill(MatrixStack matrixStack, Box box, int i) {
            this.fill(matrixStack, (float)box.minX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.maxY, (float)box.maxZ, i);
        }

        public void fill(MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i) {
            if (this.fillBuilder == null) {
                this.fillAllocator = new BufferAllocator(8192);
                this.fillBuilder = new BufferBuilder(this.fillAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            }

            Render3DUtil.emitFilled(this.fillBuilder, matrixStack.peek().getPositionMatrix(), f, f2, f3, f4, f5, f6, i);
        }

        public void outline(MatrixStack matrixStack, Box box, int i) {
            this.outline(matrixStack, (float)box.minX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.maxY, (float)box.maxZ, i);
        }

        public void outline(MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i) {
            if (this.lineBuilder == null) {
                this.lineAllocator = new BufferAllocator(8192);
                this.lineBuilder = new BufferBuilder(this.lineAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
            }

            Render3DUtil.emitOutline(this.lineBuilder, matrixStack.peek(), f, f2, f3, f4, f5, f6, i, 2.0F);
        }

        public void draw() {
            if (this.fillBuilder != null) {
                BuiltBuffer builtBuffer = this.fillBuilder.endNullable();
                if (builtBuffer != null) {
                    Render3DUtil.drawMesh(Pipelines.BOX_QUADS, builtBuffer);
                }

                this.fillBuilder = null;
            }

            if (this.lineBuilder != null) {
                BuiltBuffer builtBuffer2 = this.lineBuilder.endNullable();
                if (builtBuffer2 != null) {
                    Render3DUtil.drawMesh(this.pipeline, builtBuffer2);
                }

                this.lineBuilder = null;
            }
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
    }

    public static final class LineBatch implements AutoCloseable {
        private final RenderPipeline pipeline;
        private BufferAllocator allocator;
        private BufferBuilder builder;

        LineBatch(RenderPipeline renderPipeline) {
            this.pipeline = renderPipeline;
        }

        public void line(MatrixStack matrixStack, Vec3d vec3d, Vec3d vec3d2, int i, int i2, float f) {
            if (!(vec3d.squaredDistanceTo(vec3d2) < 1.0E-8)) {
                if (this.builder == null) {
                    if (this.allocator == null) {
                        this.allocator = new BufferAllocator(8192);
                    }

                    this.builder = new BufferBuilder(this.allocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                }

                Vec3d vec3d3 = Render3DUtil.getCameraPos();
                Render3DUtil.emitGradientLine(
                    this.builder,
                    matrixStack.peek(),
                    (float)(vec3d.x - vec3d3.x),
                    (float)(vec3d.y - vec3d3.y),
                    (float)(vec3d.z - vec3d3.z),
                    (float)(vec3d2.x - vec3d3.x),
                    (float)(vec3d2.y - vec3d3.y),
                    (float)(vec3d2.z - vec3d3.z),
                    i,
                    i2,
                    f,
                    f
                );
            }
        }

        public void draw() {
            if (this.builder != null) {
                BuiltBuffer builtBuffer = this.builder.endNullable();
                if (builtBuffer != null) {
                    Render3DUtil.drawMesh(this.pipeline, builtBuffer);
                }

                this.builder = null;
            }
        }

        @Override
        public void close() {
            if (this.allocator != null) {
                this.allocator.close();
            }
        }
    }
}
