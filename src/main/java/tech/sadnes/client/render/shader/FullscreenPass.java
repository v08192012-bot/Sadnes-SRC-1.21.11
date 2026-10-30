package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.OptionalInt;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.lwjgl.opengl.GL11;

public final class FullscreenPass {
    private static final BufferAllocator QUAD_ALLOCATOR = new BufferAllocator(256) {
        @Override
        public void close() {
            this.reset();
        }
    };

    private FullscreenPass() {
    }

    public static FullscreenPass.RenderStateGuard guardState() {
        return new FullscreenPass.RenderStateGuard();
    }

    public static void blitGuarded(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, float f) {
        try (FullscreenPass.RenderStateGuard renderStateGuard = guardState()) {
            blitUnflipped(renderPipeline, framebuffer, gpuTextureView, gpuSampler, f);
        }
    }

    private static float getTopV() {
        return GlStateSnapshot.isOpenGl() ? 1.0F : 0.0F;
    }

    private static float getNonGlFlag() {
        return GlStateSnapshot.isOpenGl() ? 0.0F : 1.0F;
    }

    public static void copyTexture(Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler) {
        GpuBufferSlice gpuBufferSlice = BlurRectUniformStorage.write(new BlurRectUniforms(0.0F, 0.0F, 1.0F, 1.0F, 0.0F));
        blitWithUniforms(Pipelines.CLAMP_COPY, framebuffer, gpuTextureView, gpuSampler, gpuBufferSlice);
    }

    public static void blitUnflipped(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, float f) {
        blitQuad(renderPipeline, framebuffer, gpuTextureView, gpuSampler, f, 1.0F);
    }

    public static void blit(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, float f) {
        blitQuad(renderPipeline, framebuffer, gpuTextureView, gpuSampler, f, getTopV());
    }

    private static void blitQuad(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, float f, float f2) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuSampler != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                float f3 = 1.0F - f2;
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, CustomVertexFormats.POSITION_TEX_LINE_WIDTH);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, f2).lineWidth(f);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, f3).lineWidth(f);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, f3).lineWidth(f);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, f2).lineWidth(f);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                drawFullscreenPipeline(renderPipeline, framebuffer, gpuTextureView, gpuSampler, builtBuffer, null);
            }
        }
    }

    public static void drawMotionBlur(
        RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler, GpuSampler gpuSampler2, GpuBufferSlice gpuBufferSlice
    ) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuTextureView2 != null && gpuBufferSlice != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                float f = getTopV();
                float f2 = 1.0F - f;
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, CustomVertexFormats.POSITION_TEX_LINE_WIDTH);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, f).lineWidth(0.0F);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, f2).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, f2).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, f).lineWidth(0.0F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes motion blur vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "sadnes motion blur", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                    renderPass.bindTexture("Sampler1", gpuTextureView2, gpuSampler2);
                    renderPass.setUniform("SadnesMotionBlur", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    public static void drawAmbiencePass(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, GpuBufferSlice gpuBufferSlice) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuSampler != null && gpuBufferSlice != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                BuiltBuffer builtBuffer = buildFullscreenQuad(bufferAllocator, 0.0F);
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes ambience pass vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "sadnes ambience pass", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                    renderPass.setUniform("SadnesAmbiencePost", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    public static void drawAmbienceDepthBlur(
        RenderPipeline renderPipeline,
        Framebuffer framebuffer,
        GpuTextureView gpuTextureView,
        GpuTextureView gpuTextureView2,
        GpuTextureView gpuTextureView3,
        GpuSampler gpuSampler,
        GpuSampler gpuSampler2,
        GpuBufferSlice gpuBufferSlice
    ) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuTextureView2 != null && gpuTextureView3 != null && gpuSampler != null && gpuSampler2 != null && gpuBufferSlice != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                BuiltBuffer builtBuffer = buildFullscreenQuad(bufferAllocator, 0.0F);
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes ambience depth blur vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "sadnes ambience depth blur", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                    renderPass.bindTexture("Sampler1", gpuTextureView2, gpuSampler2);
                    renderPass.bindTexture("Sampler2", gpuTextureView3, gpuSampler2);
                    renderPass.setUniform("SadnesAmbiencePost", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    public static void drawAmbienceReflection(
        RenderPipeline renderPipeline,
        Framebuffer framebuffer,
        GpuTextureView gpuTextureView,
        GpuTextureView gpuTextureView2,
        GpuTextureView gpuTextureView3,
        GpuSampler gpuSampler,
        GpuSampler gpuSampler2,
        GpuBufferSlice gpuBufferSlice
    ) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuTextureView2 != null && gpuTextureView3 != null && gpuSampler != null && gpuSampler2 != null && gpuBufferSlice != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                BuiltBuffer builtBuffer = buildFullscreenQuad(bufferAllocator, 0.0F);
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes ambience reflection vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "sadnes ambience reflection", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                    renderPass.bindTexture("Sampler1", gpuTextureView2, gpuSampler2);
                    renderPass.bindTexture("Sampler2", gpuTextureView3, gpuSampler2);
                    renderPass.setUniform("SadnesAmbiencePost", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    public static void drawAmbienceDepthPass(
        RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler, GpuSampler gpuSampler2, GpuBufferSlice gpuBufferSlice, float f
    ) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuTextureView2 != null && gpuSampler != null && gpuSampler2 != null && gpuBufferSlice != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                BuiltBuffer builtBuffer = buildFullscreenQuad(bufferAllocator, f);
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes ambience depth pass vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "sadnes ambience depth pass", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                    renderPass.bindTexture("Sampler1", gpuTextureView2, gpuSampler2);
                    renderPass.setUniform("SadnesAmbiencePost", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    private static BuiltBuffer buildFullscreenQuad(BufferAllocator bufferAllocator, float f) {
        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, CustomVertexFormats.POSITION_TEX_LINE_WIDTH);
        bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).lineWidth(f);
        bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).lineWidth(f);
        bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).lineWidth(f);
        bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).lineWidth(f);
        return bufferBuilder.endNullable();
    }

    public static void drawGenerative(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, String string) {
        if (renderPipeline != null && framebuffer != null && gpuBufferSlice != null && string != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                float f = getTopV();
                float f2 = 1.0F - f;
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, CustomVertexFormats.POSITION_TEX_LINE_WIDTH);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, f).lineWidth(0.0F);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, f2).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, f2).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, f).lineWidth(0.0F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes generative vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (
                    FullscreenPass.RenderStateGuard renderStateGuard = guardState();
                    RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "sadnes generative", framebuffer.getColorAttachmentView(), OptionalInt.empty());
                ) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.setUniform(string, gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    public static void blitWithUniformsUnflipped(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, GpuBufferSlice gpuBufferSlice) {
        blitWithUniformsQuad(renderPipeline, framebuffer, gpuTextureView, gpuSampler, gpuBufferSlice, 1.0F);
    }

    public static void blitWithUniforms(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, GpuBufferSlice gpuBufferSlice) {
        blitWithUniformsQuad(renderPipeline, framebuffer, gpuTextureView, gpuSampler, gpuBufferSlice, getTopV());
    }

    private static void blitWithUniformsQuad(RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, GpuBufferSlice gpuBufferSlice, float f) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuSampler != null && gpuBufferSlice != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                float f2 = 1.0F - f;
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, CustomVertexFormats.POSITION_TEX_LINE_WIDTH);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, f).lineWidth(0.0F);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, f2).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, f2).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, f).lineWidth(0.0F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                drawFullscreenPipeline(renderPipeline, framebuffer, gpuTextureView, gpuSampler, builtBuffer, gpuBufferSlice);
            }
        }
    }

    public static void drawGlowComposite(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuSampler gpuSampler, int i) {
        drawGlowCompositeWithFlame(renderPipeline, gpuTextureView, gpuTextureView2, gpuTextureView3, gpuSampler, i, null);
    }

    public static void drawGlowCompositeWithFlame(
        RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuSampler gpuSampler, int i, Float floatValue
    ) {
        if (renderPipeline != null && gpuTextureView != null && gpuTextureView2 != null && gpuTextureView3 != null && gpuSampler != null) {
            GpuBufferSlice gpuBufferSlice = floatValue == null ? null : FlameUniformStorage.write(new FlameUniforms(floatValue, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            drawColoredComposite(renderPipeline, gpuTextureView, gpuTextureView2, gpuTextureView3, gpuSampler, i, gpuBufferSlice, "sadnes glow composite");
        }
    }

    public static void drawDischargeShell(
        RenderPipeline renderPipeline,
        GpuTextureView gpuTextureView,
        GpuTextureView gpuTextureView2,
        GpuTextureView gpuTextureView3,
        GpuSampler gpuSampler,
        int i,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        float f10
    ) {
        if (renderPipeline != null && gpuTextureView != null && gpuTextureView2 != null && gpuTextureView3 != null && gpuSampler != null) {
            GpuBufferSlice gpuBufferSlice = FlameUniformStorage.write(
                new FlameUniforms(f, f2, f3, f4, f5, f6, f7, f8, getNonGlFlag(), f9, f10)
            );
            drawColoredComposite(renderPipeline, gpuTextureView, gpuTextureView2, gpuTextureView3, gpuSampler, i, gpuBufferSlice, "sadnes discharge shell");
        }
    }

    private static void drawColoredComposite(
        RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuSampler gpuSampler, int i, GpuBufferSlice gpuBufferSlice, String string
    ) {
        drawColoredCompositeQuad(renderPipeline, gpuTextureView, gpuTextureView2, gpuTextureView3, gpuSampler, i, gpuBufferSlice, string, 1.0F);
    }

    public static void drawTemporalComposite(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuSampler gpuSampler, int i) {
        drawColoredCompositeQuad(renderPipeline, gpuTextureView, gpuTextureView2, gpuTextureView3, gpuSampler, i, null, "sadnes temporal composite", getTopV());
    }

    private static void drawColoredCompositeQuad(
        RenderPipeline renderPipeline,
        GpuTextureView gpuTextureView,
        GpuTextureView gpuTextureView2,
        GpuTextureView gpuTextureView3,
        GpuSampler gpuSampler,
        int i,
        GpuBufferSlice gpuBufferSlice,
        String string,
        float f
    ) {
        try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
            float f2 = 1.0F - f;
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, f).color(i);
            bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, f2).color(i);
            bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, f2).color(i);
            bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, f).color(i);
            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer == null) {
                return;
            }

            GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> string + " vbo", builtBuffer.getBuffer());
            ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
            GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
            GlStateSnapshot glStateSnapshot = GlStateSnapshot.capture();

            try {
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> string, gpuTextureView, OptionalInt.empty())) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                    renderPass.bindTexture("Sampler1", gpuTextureView3, gpuSampler);
                    if (gpuBufferSlice != null) {
                        renderPass.setUniform("SadnesFlame", gpuBufferSlice);
                    }

                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            } finally {
                glStateSnapshot.restore();
            }
        }
    }

    public static void drawScriptGlow(
        RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4, GpuSampler gpuSampler, int i, GpuBufferSlice gpuBufferSlice
    ) {
        if (renderPipeline != null && gpuTextureView != null && gpuTextureView2 != null && gpuTextureView3 != null && gpuTextureView4 != null && gpuSampler != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(i);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(i);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(i);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(i);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes script glow vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                GlStateSnapshot glStateSnapshot = GlStateSnapshot.capture();

                try {
                    BuiltBuffer builtBuffer2 = builtBuffer;

                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "sadnes script glow", gpuTextureView, OptionalInt.empty())) {
                        renderPass.setPipeline(renderPipeline);
                        renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                        renderPass.bindTexture("Sampler1", gpuTextureView3, gpuSampler);
                        renderPass.bindTexture("Sampler2", gpuTextureView4, gpuSampler);
                        renderPass.setUniform("SadnesScriptFx", gpuBufferSlice);
                        renderPass.setVertexBuffer(0, gpuBuffer);
                        renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                        renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                    } catch (Throwable throwable) {
                        if (builtBuffer2 != null) {
                            try {
                                builtBuffer2.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }

                        throw throwable;
                    }

                    if (builtBuffer != null) {
                        builtBuffer.close();
                    }
                } finally {
                    glStateSnapshot.restore();
                }
            }
        }
    }

    public static void drawRimComposite(
        RenderPipeline renderPipeline,
        GpuTextureView gpuTextureView,
        GpuTextureView gpuTextureView2,
        GpuTextureView gpuTextureView3,
        GpuSampler gpuSampler,
        int i,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6
    ) {
        if (renderPipeline != null && gpuTextureView != null && gpuTextureView2 != null && gpuTextureView3 != null && gpuSampler != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F - f5, 1.0F - f6).color(i);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F - f5, 0.0F - f6).color(i);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F - f5, 0.0F - f6).color(i);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F - f5, 1.0F - f6).color(i);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                GpuBufferSlice gpuBufferSlice = FlameUniformStorage.write(new FlameUniforms(f, f2, 0.0F, 0.0F, 0.0F, f3, f4, 0.0F, getNonGlFlag()));
                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes rim composite vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                GlStateSnapshot glStateSnapshot = GlStateSnapshot.capture();

                try {
                    BuiltBuffer builtBuffer2 = builtBuffer;

                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "sadnes rim composite", gpuTextureView, OptionalInt.empty())) {
                        renderPass.setPipeline(renderPipeline);
                        renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                        renderPass.bindTexture("Sampler1", gpuTextureView3, gpuSampler);
                        renderPass.setUniform("SadnesFlame", gpuBufferSlice);
                        renderPass.setVertexBuffer(0, gpuBuffer);
                        renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                        renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                    } catch (Throwable throwable) {
                        if (builtBuffer2 != null) {
                            try {
                                builtBuffer2.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }

                        throw throwable;
                    }

                    if (builtBuffer != null) {
                        builtBuffer.close();
                    }
                } finally {
                    glStateSnapshot.restore();
                }
            }
        }
    }

    public static void drawHandSweep(
        RenderPipeline renderPipeline,
        GpuTextureView gpuTextureView,
        GpuTextureView gpuTextureView2,
        GpuTextureView gpuTextureView3,
        GpuSampler gpuSampler,
        int i,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        float f10,
        float f11,
        float f12
    ) {
        if (renderPipeline != null && gpuTextureView != null && gpuTextureView2 != null && gpuTextureView3 != null && gpuSampler != null) {
            if (!(f11 <= f9) && !(f12 <= f10)) {
                try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                    float f13 = f9 * 2.0F - 1.0F;
                    float f14 = f11 * 2.0F - 1.0F;
                    float f15 = f10 * 2.0F - 1.0F;
                    float f16 = f12 * 2.0F - 1.0F;
                    BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    bufferBuilder.vertex(f13, f16, 0.0F).texture(f9, f12).color(i);
                    bufferBuilder.vertex(f13, f15, 0.0F).texture(f9, f10).color(i);
                    bufferBuilder.vertex(f14, f15, 0.0F).texture(f11, f10).color(i);
                    bufferBuilder.vertex(f14, f16, 0.0F).texture(f11, f12).color(i);
                    BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                    if (builtBuffer == null) {
                        return;
                    }

                    GpuBufferSlice gpuBufferSlice = FlameUniformStorage.write(new FlameUniforms(f, f2, f3, f4, f5, f6, f7, f8, getNonGlFlag()));
                    GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes hand sweep vbo", builtBuffer.getBuffer());
                    ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                    GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                    GlStateSnapshot glStateSnapshot = GlStateSnapshot.capture();

                    try {
                        BuiltBuffer builtBuffer2 = builtBuffer;

                        try (RenderPass renderPass = RenderSystem.getDevice()
                                .createCommandEncoder()
                                .createRenderPass(() -> "sadnes hand sweep", gpuTextureView, OptionalInt.empty())) {
                            renderPass.setPipeline(renderPipeline);
                            renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                            renderPass.bindTexture("Sampler1", gpuTextureView3, gpuSampler);
                            renderPass.setUniform("SadnesFlame", gpuBufferSlice);
                            renderPass.setVertexBuffer(0, gpuBuffer);
                            renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                            renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                        } catch (Throwable throwable) {
                            if (builtBuffer2 != null) {
                                try {
                                    builtBuffer2.close();
                                } catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }

                            throw throwable;
                        }

                        if (builtBuffer != null) {
                            builtBuffer.close();
                        }
                    } finally {
                        glStateSnapshot.restore();
                    }
                }
            }
        }
    }

    public static void compositeUnflipped(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler, int i) {
        compositeQuad(renderPipeline, gpuTextureView, gpuTextureView2, gpuSampler, i, 1.0F);
    }

    public static void composite(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler, int i) {
        compositeQuad(renderPipeline, gpuTextureView, gpuTextureView2, gpuSampler, i, getTopV());
    }

    private static void compositeQuad(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler, int i, float f) {
        if (renderPipeline != null && gpuTextureView != null && gpuTextureView2 != null && gpuSampler != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                float f2 = 1.0F - f;
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, f).color(i);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, f2).color(i);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, f2).color(i);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, f).color(i);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes composite vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                GlStateSnapshot glStateSnapshot = GlStateSnapshot.capture();

                try {
                    BuiltBuffer builtBuffer2 = builtBuffer;

                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "sadnes composite", gpuTextureView, OptionalInt.empty())) {
                        renderPass.setPipeline(renderPipeline);
                        renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                        renderPass.setVertexBuffer(0, gpuBuffer);
                        renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                        renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                    } catch (Throwable throwable) {
                        if (builtBuffer2 != null) {
                            try {
                                builtBuffer2.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }

                        throw throwable;
                    }

                    if (builtBuffer != null) {
                        builtBuffer.close();
                    }
                } finally {
                    glStateSnapshot.restore();
                }
            }
        }
    }

    public static void drawFlameAdvect(
        RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler, float f, float f2, float f3
    ) {
        if (renderPipeline != null && framebuffer != null && gpuTextureView != null && gpuTextureView2 != null && gpuSampler != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                float f4 = getTopV();
                float f5 = 1.0F - f4;
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, CustomVertexFormats.POSITION_TEX_LINE_WIDTH);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, f4).lineWidth(0.0F);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, f5).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, f5).lineWidth(0.0F);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, f4).lineWidth(0.0F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                GpuBufferSlice gpuBufferSlice = FlameUniformStorage.write(new FlameUniforms(0.0F, 0.0F, 0.0F, f, 0.0F, f2, f3, 0.0F, 0.0F));
                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes flame advect vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                BuiltBuffer builtBuffer2 = builtBuffer;

                try (
                    FullscreenPass.RenderStateGuard renderStateGuard = guardState();
                    RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "sadnes flame advect", framebuffer.getColorAttachmentView(), OptionalInt.empty());
                ) {
                    renderPass.setPipeline(renderPipeline);
                    renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                    renderPass.bindTexture("Sampler1", gpuTextureView2, gpuSampler);
                    renderPass.setUniform("SadnesFlame", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    public static void drawFlameComposite(
        RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuSampler gpuSampler, int i, float f, float f2, float f3, float f4, float f5
    ) {
        if (renderPipeline != null && gpuTextureView != null && gpuTextureView2 != null && gpuSampler != null) {
            try (BufferAllocator bufferAllocator = QUAD_ALLOCATOR) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(i);
                bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(i);
                bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(i);
                bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(i);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                GpuBufferSlice gpuBufferSlice = FlameUniformStorage.write(new FlameUniforms(f, f2, f3, 0.0F, f4, 0.0F, 0.0F, f5, 0.0F));
                GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes flame composite vbo", builtBuffer.getBuffer());
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
                GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
                GlStateSnapshot glStateSnapshot = GlStateSnapshot.capture();

                try {
                    BuiltBuffer builtBuffer2 = builtBuffer;

                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "sadnes flame composite", gpuTextureView, OptionalInt.empty())) {
                        renderPass.setPipeline(renderPipeline);
                        renderPass.bindTexture("Sampler0", gpuTextureView2, gpuSampler);
                        renderPass.setUniform("SadnesFlame", gpuBufferSlice);
                        renderPass.setVertexBuffer(0, gpuBuffer);
                        renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                        renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
                    } catch (Throwable throwable) {
                        if (builtBuffer2 != null) {
                            try {
                                builtBuffer2.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }

                        throw throwable;
                    }

                    if (builtBuffer != null) {
                        builtBuffer.close();
                    }
                } finally {
                    glStateSnapshot.restore();
                }
            }
        }
    }

    private static void drawFullscreenPipeline(
        RenderPipeline renderPipeline, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuSampler gpuSampler, BuiltBuffer builtBuffer, GpuBufferSlice gpuBufferSlice
    ) {
        GpuBuffer gpuBuffer = FrameBufferPool.createBuffer(() -> "sadnes fullscreen pipeline vbo", builtBuffer.getBuffer());
        ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.getDrawParameters().mode());
        GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(builtBuffer.getDrawParameters().indexCount());
        BuiltBuffer builtBuffer2 = builtBuffer;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "sadnes fullscreen pipeline", framebuffer.getColorAttachmentView(), OptionalInt.of(0))) {
            renderPass.setPipeline(renderPipeline);
            renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
            if (gpuBufferSlice != null) {
                renderPass.setUniform("SadnesBlurRect", gpuBufferSlice);
            }

            renderPass.setVertexBuffer(0, gpuBuffer);
            renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
            renderPass.drawIndexed(0, 0, builtBuffer.getDrawParameters().indexCount(), 1);
        } catch (Throwable throwable) {
            if (builtBuffer2 != null) {
                try {
                    builtBuffer2.close();
                } catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
            }

            throw throwable;
        }

        if (builtBuffer != null) {
            builtBuffer.close();
        }
    }

    public static final class RenderStateGuard implements AutoCloseable {
        private final GlStateSnapshot glState = GlStateSnapshot.capture();
        private final boolean openGl = GlStateSnapshot.isOpenGl();
        private final int[] scissorBox = new int[4];
        private final boolean scissorEnabled;

        RenderStateGuard() {
            if (!this.openGl) {
                this.scissorEnabled = false;
            } else {
                GL11.glGetIntegerv(3088, this.scissorBox);
                this.scissorEnabled = GL11.glIsEnabled(3089);
            }
        }

        @Override
        public void close() {
            this.glState.restore();
            if (this.openGl) {
                GlStateManager._scissorBox(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
                if (this.scissorEnabled) {
                    GlStateManager._enableScissorTest();
                } else {
                    GlStateManager._disableScissorTest();
                }
            }
        }
    }
}
