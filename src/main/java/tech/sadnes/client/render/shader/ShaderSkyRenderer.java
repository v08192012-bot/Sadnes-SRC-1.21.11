package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tech.sadnes.client.render.CustomSkyRenderer;

public final class ShaderSkyRenderer {
    private static final int[][] JITTER_OFFSETS = new int[][]{{0, 0}, {1, 1}, {1, 0}, {0, 1}};
    private static final GpuBuffer CUBE_BUFFER = createCubeBuffer();
    private static final Matrix4f projectionMatrix = new Matrix4f();
    private static final Matrix4f prevViewProjection = new Matrix4f();
    private static SimpleFramebuffer freshTarget;
    private static SimpleFramebuffer historyTarget;
    private static SimpleFramebuffer resolvedTarget;
    private static int frameIndex;
    private static int lastType;
    private static int lastQuality;
    private static long lastRenderNanos;
    private static boolean historyValid;

    private ShaderSkyRenderer() {
    }

    public static void setProjectionMatrix(Matrix4fc matrix4fc) {
        projectionMatrix.set(matrix4fc);
    }

    public static void render(int i, int i2, float f, float f2, int i3) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        int i4 = Math.max(1, framebuffer.textureWidth);
        int i5 = Math.max(1, framebuffer.textureHeight);
        int i6 = (i4 + 1) / 2;
        int i7 = (i5 + 1) / 2;
        long l = System.nanoTime();
        boolean flag = historyTarget == null
            || historyTarget.textureWidth != i4
            || historyTarget.textureHeight != i5
            || i != lastType
            || i3 != lastQuality
            || l - lastRenderNanos > 250000000L;
        if (flag) {
            historyValid = false;
        }

        freshTarget = ensureTarget(freshTarget, "sadnes_sky_fresh", i6, i7);
        historyTarget = ensureTarget(historyTarget, "sadnes_sky_history", i4, i5);
        resolvedTarget = ensureTarget(resolvedTarget, "sadnes_sky_resolved", i4, i5);
        boolean flag2 = GlStateSnapshot.isOpenGl();
        int[] ints = JITTER_OFFSETS[frameIndex & 3];
        float f3 = (i4 * 0.5F + 0.5F - ints[0]) / i6 - 1.0F;
        float f4 = (i5 * 0.5F + 0.5F - ints[1]) / i7 - 1.0F;
        float f5 = (float)(l % 1000000000000L / 1.0E9);
        Vector4f vector4f = ColorHelper.toRgbaVector(i2);
        float f6 = Math.max(0.0F, f);
        int i8 = Math.max(1, Math.min(i3, 4));
        float f7 = flag2 ? 0.0F : 1.0F;
        float f8 = historyValid ? 1.0F : 0.0F;
        GpuBufferSlice gpuBufferSlice = SkyUniformStorage.write(
            new SkyUniforms(
                f5,
                i,
                f6,
                f2,
                i8,
                vector4f.x,
                vector4f.y,
                vector4f.z,
                prevViewProjection,
                i4 / (2.0F * i6),
                i5 / (2.0F * i7),
                f3,
                flag2 ? f4 : -f4,
                ints[0],
                ints[1],
                f7,
                f8,
                0.5F
            )
        );
        GpuBufferSlice gpuBufferSlice2 = SkyUniformStorage.write(
            new SkyUniforms(
                f5, i, f6, f2, i8, vector4f.x, vector4f.y, vector4f.z, prevViewProjection, 1.0F, 1.0F, 0.0F, 0.0F, ints[0], ints[1], f7, f8, 1.0F
            )
        );
        GpuBufferSlice gpuBufferSlice3 = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), vector4f, new Vector3f(), new Matrix4f());
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
        GpuSampler gpuSampler2 = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        drawPass(flag2 ? Pipelines.SKY_FRESH : Pipelines.SKY_FRESH_VK, freshTarget.getColorAttachmentView(), null, gpuBufferSlice, gpuBufferSlice3, "sky fresh", renderPass -> {});
        drawPass(Pipelines.SKY_RESOLVE, resolvedTarget.getColorAttachmentView(), null, gpuBufferSlice2, gpuBufferSlice3, "sky resolve", renderPass2 -> {
            renderPass2.bindTexture("Sampler0", freshTarget.getColorAttachmentView(), gpuSampler2);
            renderPass2.bindTexture("Sampler1", historyTarget.getColorAttachmentView(), gpuSampler2);
        });
        drawPass(
            Pipelines.SKY_COMPOSITE,
            framebuffer.getColorAttachmentView(),
            framebuffer.getDepthAttachmentView(),
            gpuBufferSlice2,
            gpuBufferSlice3,
            "sky composite",
            renderPass3 -> renderPass3.bindTexture("Sampler0", resolvedTarget.getColorAttachmentView(), gpuSampler)
        );
        SimpleFramebuffer simpleFramebuffer = historyTarget;
        historyTarget = resolvedTarget;
        resolvedTarget = simpleFramebuffer;
        prevViewProjection.set(projectionMatrix).mul(RenderSystem.getModelViewMatrix());
        lastType = i;
        lastQuality = i3;
        lastRenderNanos = l;
        historyValid = true;
        frameIndex++;
    }

    public static void releaseIfIdle() {
        if (freshTarget != null && System.nanoTime() - lastRenderNanos > 2000000000L) {
            release();
        }
    }

    public static void release() {
        if (freshTarget != null) {
            freshTarget.delete();
        }

        if (historyTarget != null) {
            historyTarget.delete();
        }

        if (resolvedTarget != null) {
            resolvedTarget.delete();
        }

        freshTarget = null;
        historyTarget = null;
        resolvedTarget = null;
        historyValid = false;
    }

    private static void drawPass(
        RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2, String string, Consumer<RenderPass> consumer
    ) {
        ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
        GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(36);

        try (RenderPass renderPass = gpuTextureView2 == null
                ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sadnes " + string, gpuTextureView, OptionalInt.empty())
                : RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "Sadnes " + string, gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
            renderPass.setPipeline(renderPipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SadnesSky", gpuBufferSlice);
            renderPass.setUniform("DynamicTransforms", gpuBufferSlice2);
            consumer.accept(renderPass);
            renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());
            renderPass.setVertexBuffer(0, CUBE_BUFFER);
            renderPass.drawIndexed(0, 0, 36, 1);
        }
    }

    private static SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, String string, int i, int i2) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, i, i2, false);
        }

        if (simpleFramebuffer.textureWidth != i || simpleFramebuffer.textureHeight != i2) {
            simpleFramebuffer.resize(i, i2);
        }

        return simpleFramebuffer;
    }

    private static GpuBuffer createCubeBuffer() {
        try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(24 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float[][] floatses = new float[][]{{-100.0F, -100.0F}, {-100.0F, 100.0F}, {100.0F, 100.0F}, {100.0F, -100.0F}};
            Vector3f vector3f = new Vector3f();

            for (int i = 0; i < 6; i++) {
                Matrix4f matrix4f = new Matrix4f();
                CustomSkyRenderer.applyFaceRotation(matrix4f, i, 0.0F);

                for (float[] floats : floatses) {
                    matrix4f.transformPosition(floats[0], -100.0F, floats[1], vector3f);
                    bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z).texture(0.0F, 0.0F).color(-1);
                }
            }

            BuiltBuffer builtBuffer = bufferBuilder.end();

            try {
                return RenderSystem.getDevice().createBuffer(() -> "Sadnes sky shader cube", 40, builtBuffer.getBuffer());
            } finally {
                builtBuffer.close();
            }
        }
    }
}
