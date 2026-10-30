package tech.sadnes.client.render;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.util.color.ColorUtil;

public final class ChinaHatRenderer {
    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final int SEGMENTS = 64;
    private static final int RINGS = 7;
    private static final float HEIGHT = 0.3F;
    private static final float CURVE_EXPONENT = 1.7F;
    private static final float BRIM_CURL = 0.09F;
    private static final int WAVE_COUNT = 6;
    private static final float LINE_WIDTH = 1.5F;
    private static final float OUTLINE_SCALE = 1.0025F;
    private static final int MAX_VERTICES = 512;
    private static final float[] GRID_X = new float[512];
    private static final float[] GRID_Y = new float[512];
    private static final float[] GRID_Z = new float[512];
    private static final int[] GRID_COLORS = new int[512];
    private static final Matrix4f INVERSE_MATRIX = new Matrix4f();
    private static final Vector3f CAMERA_POS = new Vector3f();

    private ChinaHatRenderer() {
    }

    public static void render(MatrixStack matrixStack, ChinaHatRenderer.Style style, float f, int i, float f2, ChinaHatPhysics chinaHatPhysics) {
        float f3 = chinaHatPhysics.getScale();
        if (!(f3 <= 0.001F)) {
            matrixStack.push();
            matrixStack.translate(0.0F, chinaHatPhysics.getYOffset(), 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chinaHatPhysics.getRoll()));
            matrixStack.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(chinaHatPhysics.getPitch()));
            matrixStack.scale(f3, f3, f3);
            Entry entry = matrixStack.peek();
            float f4 = f2 * chinaHatPhysics.getAlpha();
            int i2 = getFarthestSegment(entry.getPositionMatrix());
            if (style == ChinaHatRenderer.Style.CLASSIC) {
                renderClassic(entry, i2, f, i, f4);
                matrixStack.pop();
            } else {
                buildMafiaGrid(entry.getPositionMatrix(), f, i, f4, chinaHatPhysics.getWaveAmplitude(), chinaHatPhysics.getWavePhase());
                renderMafiaSurface(entry.getPositionMatrix(), i2);
                renderMafiaOutline(entry, i, f4);
                matrixStack.pop();
            }
        }
    }

    private static int getFarthestSegment(Matrix4f matrix4f) {
        INVERSE_MATRIX.set(matrix4f).invert();
        CAMERA_POS.set(0.0F, 0.0F, 0.0F);
        INVERSE_MATRIX.transformPosition(CAMERA_POS);
        float f = (float)Math.atan2(-CAMERA_POS.x, CAMERA_POS.z);
        return Math.round(f / (float) (Math.PI * 2) * 64.0F) + 32;
    }

    private static void renderClassic(Entry entry, int i, float f, int i2, float f2) {
        Matrix4f matrix4f = entry.getPositionMatrix();
        int i3 = ColorUtil.multiplyAlpha(i2, f2);
        int i4 = ColorUtil.darken(i2, 0.5F);

        try (BufferAllocator bufferAllocator = new BufferAllocator(3328)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            for (int i5 = 0; i5 <= 32; i5++) {
                for (int i6 = 0; i6 < 2; i6++) {
                    if (i6 != 1 || i5 != 0 && i5 != 32) {
                        int i7 = Math.floorMod(i6 == 0 ? i + i5 : i - i5, 64);
                        float f3 = i7 * (float) (Math.PI * 2) / 64.0F;
                        float f4 = (i7 + 1) * (float) (Math.PI * 2) / 64.0F;
                        int i8 = ColorUtil.multiplyAlpha(ColorUtil.fade(10, segmentToDegrees(i7), i2, i4), f2);
                        int i9 = ColorUtil.multiplyAlpha(ColorUtil.fade(10, segmentToDegrees(i7 + 1), i2, i4), f2);
                        bufferBuilder.vertex(matrix4f, 0.0F, 0.3F, 0.0F).color(i3);
                        bufferBuilder.vertex(matrix4f, -MathHelper.sin(f3) * f, 0.0F, MathHelper.cos(f3) * f).color(i8);
                        bufferBuilder.vertex(matrix4f, -MathHelper.sin(f4) * f, 0.0F, MathHelper.cos(f4) * f).color(i9);
                    }
                }
            }

            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                MeshDrawUtil.drawLabeled(Pipelines.CHINA_HAT, builtBuffer, "sadnes china hat");
            }
        }

        float f5 = f * 1.0025F;

        try (BufferAllocator bufferAllocator2 = new BufferAllocator(6400)) {
            BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

            for (int i10 = 0; i10 < 64; i10++) {
                float f6 = i10 * (float) (Math.PI * 2) / 64.0F;
                float f7 = (i10 + 1) * (float) (Math.PI * 2) / 64.0F;
                float f8 = -MathHelper.sin(f6) * f5;
                float f9 = MathHelper.cos(f6) * f5;
                float f10 = -MathHelper.sin(f7) * f5;
                float f11 = MathHelper.cos(f7) * f5;
                int i11 = ColorUtil.multiplyAlpha(ColorUtil.darken(ColorUtil.fade(10, segmentToDegrees(i10), i2, i4), 0.9F), f2);
                int i12 = ColorUtil.multiplyAlpha(ColorUtil.darken(ColorUtil.fade(10, segmentToDegrees(i10 + 1), i2, i4), 0.9F), f2);
                float f12 = f10 - f8;
                float f13 = f11 - f9;
                float f14 = 1.0F / Math.max(1.0E-5F, (float)Math.sqrt(f12 * f12 + f13 * f13));
                float f15 = f12 * f14;
                float f16 = f13 * f14;
                bufferBuilder2.vertex(entry, f8, 0.0F, f9).color(i11).normal(entry, f15, 0.0F, f16).lineWidth(1.5F);
                bufferBuilder2.vertex(entry, f10, 0.0F, f11).color(i12).normal(entry, f15, 0.0F, f16).lineWidth(1.5F);
            }

            BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
            if (builtBuffer2 != null) {
                MeshDrawUtil.drawLabeled(Pipelines.CHINA_HAT_LINE, builtBuffer2, "sadnes china hat line");
            }
        }
    }

    private static void buildMafiaGrid(Matrix4f matrix4f, float f, int i, float f2, float f3, float f4) {
        int i2 = ColorUtil.darken(i, 0.45F);
        float f5 = f3 * f * 1.6F;

        for (int i3 = 0; i3 <= 7; i3++) {
            float f6 = i3 / 7.0F;
            float f7 = f * f6;
            float f8 = 0.3F * (float)Math.pow(1.0F - f6, 1.7F) + 0.09F * (f6 * f6 * f6 - f6);
            float f9 = 0.09F * (3.0F * f6 * f6 - 1.0F) - 0.51000005F * (float)Math.pow(1.0F - f6, 0.70000005F);
            float f10 = Math.max(1.0E-5F, (float)Math.sqrt(f9 * f9 + f * f));
            float f11 = -f9 / f10;
            float f12 = f / f10;
            int i4 = ColorUtil.interpolate(i, i2, f6 * 0.85F);
            int i5 = i3 * 64;

            for (int i6 = 0; i6 < 64; i6++) {
                float f13 = i6 * (float) (Math.PI * 2) / 64.0F;
                float f14 = MathHelper.sin(f13);
                float f15 = MathHelper.cos(f13);
                float f16 = MathHelper.sin(f13 * 6.0F + f4);
                float f17 = f7 * (1.0F + f3 * f16 * f6);
                float f18 = -f14 * f17;
                float f19 = f15 * f17;
                float f20 = f8 + f5 * f16 * f6 * f6;
                float f21 = CAMERA_POS.x - f18;
                float f22 = CAMERA_POS.y - f20;
                float f23 = CAMERA_POS.z - f19;
                float f24 = 1.0F / Math.max(1.0E-5F, (float)Math.sqrt(f21 * f21 + f22 * f22 + f23 * f23));
                float f25 = Math.abs(f11 * -f14 * f21 + f12 * f22 + f11 * f15 * f23) * f24;
                float f26 = 1.0F - Math.min(1.0F, f25);
                float f27 = MathHelper.sin(f13 - f4 * 0.5F);
                float f28 = f27 > 0.0F ? f27 * f27 : 0.0F;
                int i7 = i5 + i6;
                GRID_X[i7] = f18;
                GRID_Y[i7] = f20;
                GRID_Z[i7] = f19;
                GRID_COLORS[i7] = ColorUtil.multiplyAlpha(
                    ColorUtil.interpolate(i4, ColorUtil.WHITE, Math.min(0.4F, f26 * 0.32F + f28 * 0.1F)), f2 * (0.72F + 0.28F * f26)
                );
            }
        }
    }

    private static void renderMafiaSurface(Matrix4f matrix4f, int i) {
        try (BufferAllocator bufferAllocator = new BufferAllocator(43264)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            for (int i2 = 0; i2 <= 32; i2++) {
                emitColumn(bufferBuilder, matrix4f, i + i2);
                if (i2 > 0 && i2 < 32) {
                    emitColumn(bufferBuilder, matrix4f, i - i2);
                }
            }

            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                MeshDrawUtil.drawLabeled(Pipelines.CHINA_HAT, builtBuffer, "sadnes china hat");
            }
        }
    }

    private static void emitColumn(BufferBuilder bufferBuilder, Matrix4f matrix4f, int i) {
        int i2 = Math.floorMod(i, 64);
        int i3 = (i2 + 1) % 64;

        for (int i4 = 0; i4 < 7; i4++) {
            int i5 = i4 * 64;
            int i6 = i5 + 64;
            emitVertex(bufferBuilder, matrix4f, i5 + i2);
            emitVertex(bufferBuilder, matrix4f, i6 + i2);
            emitVertex(bufferBuilder, matrix4f, i6 + i3);
            if (i4 != 0) {
                emitVertex(bufferBuilder, matrix4f, i5 + i2);
                emitVertex(bufferBuilder, matrix4f, i6 + i3);
                emitVertex(bufferBuilder, matrix4f, i5 + i3);
            }
        }
    }

    private static void renderMafiaOutline(Entry entry, int i, float f) {
        int i2 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(i, ColorUtil.WHITE, 0.2F), f);

        try (BufferAllocator bufferAllocator = new BufferAllocator(6400)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

            for (int i3 = 0; i3 < 64; i3++) {
                int i4 = 448 + i3;
                int i5 = 448 + (i3 + 1) % 64;
                float f2 = GRID_X[i4] * 1.0025F;
                float f3 = GRID_Z[i4] * 1.0025F;
                float f4 = GRID_X[i5] * 1.0025F;
                float f5 = GRID_Z[i5] * 1.0025F;
                float f6 = f4 - f2;
                float f7 = GRID_Y[i5] - GRID_Y[i4];
                float f8 = f5 - f3;
                float f9 = 1.0F / Math.max(1.0E-5F, (float)Math.sqrt(f6 * f6 + f7 * f7 + f8 * f8));
                float f10 = f6 * f9;
                float f11 = f7 * f9;
                float f12 = f8 * f9;
                bufferBuilder.vertex(entry, f2, GRID_Y[i4], f3).color(i2).normal(entry, f10, f11, f12).lineWidth(1.5F);
                bufferBuilder.vertex(entry, f4, GRID_Y[i5], f5).color(i2).normal(entry, f10, f11, f12).lineWidth(1.5F);
            }

            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                MeshDrawUtil.drawLabeled(Pipelines.CHINA_HAT_LINE, builtBuffer, "sadnes china hat line");
            }
        }
    }

    private static int segmentToDegrees(int i) {
        return Math.round(i * 360.0F / 64.0F);
    }

    private static void emitVertex(BufferBuilder bufferBuilder, Matrix4f matrix4f, int i) {
        bufferBuilder.vertex(matrix4f, GRID_X[i], GRID_Y[i], GRID_Z[i]).color(GRID_COLORS[i]);
    }

    public enum Style {
        CLASSIC,
        MAFIA;
    }
}
