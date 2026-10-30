package tech.sadnes.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tech.sadnes.client.render.shader.ShaderSkyRenderer;

public final class CustomSkyRenderer {
    private static final GpuBuffer[] FACE_BUFFERS = createFaceBuffers();
    private static int pendingShaderType;
    private static int pendingColor;
    private static float pendingBrightness;
    private static float pendingRotation;
    private static int pendingQuality;

    private CustomSkyRenderer() {
    }

    public static void renderTextured(Identifier identifier, int i, float f, float f2) {
        if (identifier != null) {
            AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
            ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(36);
            GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
            GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();
            Vector4f vector4f = ColorHelper.toRgbaVector(i);
            float f3 = Math.max(0.0F, f);
            vector4f.x *= f3;
            vector4f.y *= f3;
            vector4f.z *= f3;
            GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[6];

            for (int i2 = 0; i2 < 6; i2++) {
                Matrix4f matrix4f = new Matrix4f(RenderSystem.getModelViewMatrix());
                applyFaceRotation(matrix4f, i2, f2);
                gpuBufferSlices[i2] = RenderSystem.getDynamicUniforms().write(matrix4f, vector4f, new Vector3f(), new Matrix4f());
            }

            RenderPipeline renderPipeline = RenderPipelines.POSITION_TEX_COLOR_END_SKY;

            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "Sadnes custom sky", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.bindTexture("Sampler0", abstractTexture.getGlTextureView(), abstractTexture.getSampler());
                renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());

                for (int i3 = 0; i3 < 6; i3++) {
                    renderPass.setUniform("DynamicTransforms", gpuBufferSlices[i3]);
                    renderPass.setVertexBuffer(0, FACE_BUFFERS[i3]);
                    renderPass.drawIndexed(0, 0, 6, 1);
                }
            }
        }
    }

    public static void queueShaderSky(int i, int i2, float f, float f2, int i3) {
        pendingShaderType = i;
        pendingColor = i2;
        pendingBrightness = f;
        pendingRotation = f2;
        pendingQuality = i3;
    }

    public static void flushShaderSky() {
        if (pendingShaderType <= 0) {
            ShaderSkyRenderer.releaseIfIdle();
        } else {
            ShaderSkyRenderer.render(pendingShaderType, pendingColor, pendingBrightness, pendingRotation, pendingQuality);
            pendingShaderType = 0;
        }
    }

    public static void applyFaceRotation(Matrix4f matrix4f, int i, float f) {
        label37: {
            label36: {
                label35: {
                    matrix4f.rotateY((float)Math.toRadians(f));
                    label30:
                    switch (i) {
                        case 1:
                            matrix4f.rotateX((float)Math.toRadians(90.0));
                            switch (i) {
                                case 2:
                                    break;
                                case 3:
                                    break label30;
                                case 4:
                                    break label35;
                                case 5:
                                    break label37;
                                default:
                                    return;
                            }
                        case 2:
                            matrix4f.rotateX((float)Math.toRadians(-90.0)).rotateY((float)Math.toRadians(180.0));
                            switch (i) {
                                case 3:
                                    break;
                                case 4:
                                    break label35;
                                case 5:
                                    break label37;
                                default:
                                    return;
                            }
                        case 3:
                            break;
                        case 4:
                            break label35;
                        case 5:
                            break label37;
                        default:
                            return;
                    }

                    matrix4f.rotateX((float)Math.toRadians(180.0));
                    if (i != 4) {
                        break label36;
                    }
                }

                matrix4f.rotateZ((float)Math.toRadians(90.0)).rotateY((float)Math.toRadians(-90.0));
            }

            if (i != 5) {
                return;
            }
        }

        matrix4f.rotateZ((float)Math.toRadians(-90.0)).rotateY((float)Math.toRadians(90.0));
    }

    private static GpuBuffer[] createFaceBuffers() {
        return new GpuBuffer[]{
            createFaceBuffer(0.0F, 0.0F, 0.33333334F, 0.5F),
            createFaceBuffer(0.33333334F, 0.5F, 0.6666667F, 1.0F),
            createFaceBuffer(0.6666667F, 0.0F, 1.0F, 0.5F),
            createFaceBuffer(0.33333334F, 0.0F, 0.6666667F, 0.5F),
            createFaceBuffer(0.6666667F, 0.5F, 1.0F, 1.0F),
            createFaceBuffer(0.0F, 0.5F, 0.33333334F, 1.0F)
        };
    }

    private static GpuBuffer createFaceBuffer(float f, float f2, float f3, float f4) {
        try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(-100.0F, -100.0F, -100.0F).texture(f, f2).color(-1);
            bufferBuilder.vertex(-100.0F, -100.0F, 100.0F).texture(f, f4).color(-1);
            bufferBuilder.vertex(100.0F, -100.0F, 100.0F).texture(f3, f4).color(-1);
            bufferBuilder.vertex(100.0F, -100.0F, -100.0F).texture(f3, f2).color(-1);
            BuiltBuffer builtBuffer = bufferBuilder.end();

            try {
                return RenderSystem.getDevice().createBuffer(() -> "Sadnes custom sky face buffer", 40, builtBuffer.getBuffer());
            } finally {
                builtBuffer.close();
            }
        }
    }
}
