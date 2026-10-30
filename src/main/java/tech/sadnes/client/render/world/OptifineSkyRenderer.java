package tech.sadnes.client.render.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tech.sadnes.client.render.shader.Pipelines;

final class OptifineSkyRenderer {
    private static final GpuBuffer[] FACE_BUFFERS = createFaceBuffers();
    private static final float RIGHT_ANGLE = (float)Math.toRadians(90.0);

    private OptifineSkyRenderer() {
    }

    static void render(List<OptifineSkyLayer> optifineSkyLayers, float[] floats, float f, World world) {
        int i = optifineSkyLayers.size();
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
        GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(36);
        GpuTextureView gpuTextureView = minecraftClient.getFramebuffer().getColorAttachmentView();
        GpuTextureView gpuTextureView2 = minecraftClient.getFramebuffer().getDepthAttachmentView();
        GpuBufferSlice[][] gpuBufferSliceses = new GpuBufferSlice[i][6];
        AbstractTexture[] abstractTextures = new AbstractTexture[i];

        for (int i2 = 0; i2 < i; i2++) {
            OptifineSkyLayer optifineSkyLayer = (OptifineSkyLayer)optifineSkyLayers.get(i2);
            abstractTextures[i2] = minecraftClient.getTextureManager().getTexture(optifineSkyLayer.texture);
            Vector4f vector4f = OptifineBlendMode.getColorMultiplier(optifineSkyLayer.blend, floats[i2]);
            Matrix4f matrix4f = new Matrix4f(RenderSystem.getModelViewMatrix());
            if (optifineSkyLayer.rotate) {
                matrix4f.rotate((float)Math.toRadians(getRotationAngle(optifineSkyLayer, f, world)), optifineSkyLayer.axis[0], optifineSkyLayer.axis[1], optifineSkyLayer.axis[2]);
            }

            Matrix4f matrix4f2 = matrix4f.rotateX(RIGHT_ANGLE).rotateZ(-RIGHT_ANGLE);

            for (int i3 = 0; i3 < 6; i3++) {
                Matrix4f matrix4f3 = rotateForFace(matrix4f2, i3);
                gpuBufferSliceses[i2][i3] = RenderSystem.getDynamicUniforms().write(matrix4f3, vector4f, new Vector3f(), new Matrix4f());
            }
        }

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Sadnes optifine sky", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());

            for (int i4 = 0; i4 < i; i4++) {
                RenderPipeline renderPipeline = Pipelines.OPTIFINE_SKY[((OptifineSkyLayer)optifineSkyLayers.get(i4)).blend];
                AbstractTexture abstractTexture = abstractTextures[i4];
                renderPass.setPipeline(renderPipeline);
                renderPass.bindTexture("Sampler0", abstractTexture.getGlTextureView(), abstractTexture.getSampler());

                for (int i5 = 0; i5 < 6; i5++) {
                    renderPass.setUniform("DynamicTransforms", gpuBufferSliceses[i4][i5]);
                    renderPass.setVertexBuffer(0, FACE_BUFFERS[i5]);
                    renderPass.drawIndexed(0, 0, 6, 1);
                }
            }
        }
    }

    private static float getRotationAngle(OptifineSkyLayer optifineSkyLayer, float f, World world) {
        float f2 = 0.0F;
        if (optifineSkyLayer.speed != Math.round(optifineSkyLayer.speed)) {
            long l = (world.getTimeOfDay() + 18000L) / 24000L;
            double d = optifineSkyLayer.speed % 1.0F;
            f2 = (float)(l * d % 1.0);
        }

        return 360.0F * (f2 + f * optifineSkyLayer.speed);
    }

    private static Matrix4f rotateForFace(Matrix4f matrix4f, int i) {
        Matrix4f matrix4f2 = new Matrix4f(matrix4f);
        switch (i) {
            case 0:
                return matrix4f2.rotateX(-RIGHT_ANGLE);
            case 1:
                return matrix4f2.rotateX(RIGHT_ANGLE);
            case 2:
                return matrix4f2.rotateZ(2.0F * RIGHT_ANGLE);
            case 3:
                return matrix4f2.rotateZ(3.0F * RIGHT_ANGLE);
            case 4:
            default:
                return matrix4f2;
            case 5:
                return matrix4f2.rotateZ(RIGHT_ANGLE);
        }
    }

    private static GpuBuffer[] createFaceBuffers() {
        GpuBuffer[] gpuBuffers = new GpuBuffer[6];

        for (int i = 0; i < 6; i++) {
            float f = i % 3 / 3.0F;
            float f2 = i / 3 / 2.0F;
            gpuBuffers[i] = createFaceBuffer(f, f2);
        }

        return gpuBuffers;
    }

    private static GpuBuffer createFaceBuffer(float f, float f2) {
        float f3 = f + 0.33333334F;
        float f4 = f2 + 0.5F;

        try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(-100.0F, -100.0F, -100.0F).texture(f, f2).color(-1);
            bufferBuilder.vertex(-100.0F, -100.0F, 100.0F).texture(f, f4).color(-1);
            bufferBuilder.vertex(100.0F, -100.0F, 100.0F).texture(f3, f4).color(-1);
            bufferBuilder.vertex(100.0F, -100.0F, -100.0F).texture(f3, f2).color(-1);
            BuiltBuffer builtBuffer = bufferBuilder.end();

            try {
                return RenderSystem.getDevice().createBuffer(() -> "Sadnes optifine sky face", 40, builtBuffer.getBuffer());
            } finally {
                builtBuffer.close();
            }
        }
    }
}
