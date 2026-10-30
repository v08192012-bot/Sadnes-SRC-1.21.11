package tech.sadnes.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import tech.sadnes.client.render.shader.FullscreenPass;
import tech.sadnes.client.render.shader.Pipelines;

public final class BoxEspMaskRenderer implements AutoCloseable {
    SimpleFramebuffer maskTarget;

    public BoxEspMaskRenderer.Batch begin(int i) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        int i2 = framebuffer.textureWidth;
        int i3 = framebuffer.textureHeight;
        if (this.maskTarget == null) {
            this.maskTarget = new SimpleFramebuffer("sadnes_boxesp_mask", i2, i3, true);
        } else if (this.maskTarget.textureWidth != i2 || this.maskTarget.textureHeight != i3) {
            this.maskTarget.resize(i2, i3);
        }

        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(this.maskTarget.getColorAttachment(), 0, this.maskTarget.getDepthAttachment(), 1.0);
        return new BoxEspMaskRenderer.Batch(i);
    }

    @Override
    public void close() {
        if (this.maskTarget != null) {
            this.maskTarget.delete();
        }

        this.maskTarget = null;
    }

    static int faceId(Matrix4f matrix4f, int i, float f) {
        int i2 = Math.round(f * 16.0F) * 31 + Math.round(matrix4f.m30() * 16.0F) * 17 + Math.round(matrix4f.m31() * 16.0F) * 13 + Math.round(matrix4f.m32() * 16.0F);
        return 1 + i * 42 + Math.floorMod(i2, 42) << 24;
    }

    public final class Batch implements AutoCloseable {
        private final BufferAllocator allocator = new BufferAllocator(8192);
        private final int color;
        private BufferBuilder builder;
        private int boxCount;
        private boolean dirty;

        Batch(int i) {
            this.color = i;
        }

        public void addBox(MatrixStack matrixStack, float f, float f2, float f3, float f4, float f5, float f6, int i) {
            if (this.boxCount >= 60000) {
                this.flush();
            }

            if (this.builder == null) {
                this.builder = new BufferBuilder(this.allocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            }

            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            int i2 = i & 16777215;
            int i3 = i2 | BoxEspMaskRenderer.faceId(matrix4f, 0, f2);
            int i4 = i2 | BoxEspMaskRenderer.faceId(matrix4f, 1, f5);
            int i5 = i2 | BoxEspMaskRenderer.faceId(matrix4f, 2, f3);
            int i6 = i2 | BoxEspMaskRenderer.faceId(matrix4f, 3, f6);
            int i7 = i2 | BoxEspMaskRenderer.faceId(matrix4f, 4, f);
            int i8 = i2 | BoxEspMaskRenderer.faceId(matrix4f, 5, f4);
            this.builder.vertex(matrix4f, f, f2, f3).color(i3);
            this.builder.vertex(matrix4f, f4, f2, f3).color(i3);
            this.builder.vertex(matrix4f, f4, f2, f6).color(i3);
            this.builder.vertex(matrix4f, f, f2, f6).color(i3);
            this.builder.vertex(matrix4f, f, f5, f6).color(i4);
            this.builder.vertex(matrix4f, f4, f5, f6).color(i4);
            this.builder.vertex(matrix4f, f4, f5, f3).color(i4);
            this.builder.vertex(matrix4f, f, f5, f3).color(i4);
            this.builder.vertex(matrix4f, f, f2, f3).color(i5);
            this.builder.vertex(matrix4f, f, f5, f3).color(i5);
            this.builder.vertex(matrix4f, f4, f5, f3).color(i5);
            this.builder.vertex(matrix4f, f4, f2, f3).color(i5);
            this.builder.vertex(matrix4f, f4, f2, f6).color(i6);
            this.builder.vertex(matrix4f, f4, f5, f6).color(i6);
            this.builder.vertex(matrix4f, f, f5, f6).color(i6);
            this.builder.vertex(matrix4f, f, f2, f6).color(i6);
            this.builder.vertex(matrix4f, f, f2, f6).color(i7);
            this.builder.vertex(matrix4f, f, f5, f6).color(i7);
            this.builder.vertex(matrix4f, f, f5, f3).color(i7);
            this.builder.vertex(matrix4f, f, f2, f3).color(i7);
            this.builder.vertex(matrix4f, f4, f2, f3).color(i8);
            this.builder.vertex(matrix4f, f4, f5, f3).color(i8);
            this.builder.vertex(matrix4f, f4, f5, f6).color(i8);
            this.builder.vertex(matrix4f, f4, f2, f6).color(i8);
            this.boxCount++;
            this.dirty = true;
        }

        private void flush() {
            if (this.builder != null) {
                BuiltBuffer builtBuffer = this.builder.endNullable();
                this.builder = null;
                this.boxCount = 0;
                if (builtBuffer != null) {
                    GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride;
                    GpuTextureView gpuTextureView2 = RenderSystem.outputDepthTextureOverride;

                    try {
                        RenderSystem.outputColorTextureOverride = BoxEspMaskRenderer.this.maskTarget.getColorAttachmentView();
                        RenderSystem.outputDepthTextureOverride = BoxEspMaskRenderer.this.maskTarget.getDepthAttachmentView();
                        MeshDrawUtil.drawLabeled(Pipelines.BOX_MASK, builtBuffer, "BoxEsp merged mask");
                    } finally {
                        RenderSystem.outputColorTextureOverride = gpuTextureView;
                        RenderSystem.outputDepthTextureOverride = gpuTextureView2;
                    }
                }
            }
        }

        public void finish() {
            if (this.dirty) {
                this.flush();

                try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                    FullscreenPass.composite(
                        Pipelines.BOX_MERGED_COMPOSITE,
                        MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView(),
                        BoxEspMaskRenderer.this.maskTarget.getColorAttachmentView(),
                        RenderSystem.getSamplerCache().get(FilterMode.NEAREST),
                        this.color & 0xFF000000 | 16777215
                    );
                }

                this.dirty = false;
            }
        }

        @Override
        public void close() {
            this.allocator.close();
        }
    }
}
