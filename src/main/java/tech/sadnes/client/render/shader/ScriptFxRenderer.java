package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tech.sadnes.client.render.MeshDrawUtil;

public final class ScriptFxRenderer {
    private static final BufferAllocator BUFFER = new BufferAllocator(256) {
        @Override
        public void close() {
            this.reset();
        }
    };
    private static final long START_NANOS = System.nanoTime();

    private ScriptFxRenderer() {
    }

    public static void renderBillboard(
        MatrixStack matrixStack,
        RenderPipeline renderPipeline,
        double d,
        double d2,
        double d3,
        float f,
        int i,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6
    ) {
        if (renderPipeline != null && !(f <= 0.0F)) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            Camera camera = minecraftClient.gameRenderer.getCamera();
            if (camera != null) {
                Vec3d vec3d = camera.getCameraPos();
                float f7 = (float)(d - vec3d.x);
                float f8 = (float)(d2 - vec3d.y);
                float f9 = (float)(d3 - vec3d.z);
                Vector3f vector3f = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
                Vector3f vector3f2 = new Vector3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());
                float f10 = f * 0.5F;
                vector3f.mul(f10);
                vector3f2.mul(f10);
                Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

                try (BufferAllocator bufferAllocator = BUFFER) {
                    BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    putVertex(bufferBuilder, matrix4f, f7, f8, f9, vector3f, vector3f2, -1.0F, 1.0F, 0.0F, 1.0F, i);
                    putVertex(bufferBuilder, matrix4f, f7, f8, f9, vector3f, vector3f2, -1.0F, -1.0F, 0.0F, 0.0F, i);
                    putVertex(bufferBuilder, matrix4f, f7, f8, f9, vector3f, vector3f2, 1.0F, -1.0F, 1.0F, 0.0F, i);
                    putVertex(bufferBuilder, matrix4f, f7, f8, f9, vector3f, vector3f2, 1.0F, 1.0F, 1.0F, 1.0F, i);
                    BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                    if (builtBuffer == null) {
                        return;
                    }

                    GpuBufferSlice gpuBufferSlice = ScriptFxUniformStorage.write(new ScriptFxUniforms(f2, f, f3, f4, f5, f6, 0.0F, 0.0F));
                    MeshDrawUtil.drawWithUniform(renderPipeline, builtBuffer, "sadnes script fx", "SadnesScriptFx", gpuBufferSlice);
                }
            }
        }
    }

    private static void putVertex(
        BufferBuilder bufferBuilder,
        Matrix4f matrix4f,
        float f,
        float f2,
        float f3,
        Vector3f vector3f,
        Vector3f vector3f2,
        float f4,
        float f5,
        float f6,
        float f7,
        int i
    ) {
        bufferBuilder.vertex(matrix4f, f + vector3f.x * f4 + vector3f2.x * f5, f2 + vector3f.y * f4 + vector3f2.y * f5, f3 + vector3f.z * f4 + vector3f2.z * f5)
            .texture(f6, f7)
            .color(i);
    }

    public static float getTime() {
        return (float)((System.nanoTime() - START_NANOS) / 1.0E9);
    }
}
