package tech.sadnes.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting.Type;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.sadnes.api.mixin.accessor.SpecialGuiElementRendererAccessor;
import tech.sadnes.client.render.shader.FullscreenPass;
import tech.sadnes.client.render.shader.GlStateSnapshot;
import tech.sadnes.client.render.shader.PingPongBlurTarget;
import tech.sadnes.client.render.shader.Pipelines;

public final class EntityPreviewRenderer extends SpecialGuiElementRenderer<EntityPreviewState> {
    private final EntityRenderManager entityRenderDispatcher;
    private SimpleFramebuffer glowSharpTarget;
    private final PingPongBlurTarget glowBlur = new PingPongBlurTarget("sadnes_preview_glow_blur");

    public EntityPreviewRenderer(Immediate immediate, EntityRenderManager entityRenderManager) {
        super(immediate);
        this.entityRenderDispatcher = entityRenderManager;
    }

    @Override
    public Class<EntityPreviewState> getElementClass() {
        return EntityPreviewState.class;
    }

    @Override
    public String getName() {
        return "sadnes_entity_preview";
    }

    @Override
    public float getYOffset(int i, int i2) {
        return i / 2.0F;
    }

    @Override
    protected void renderElement(EntityPreviewState entityPreviewState, GuiRenderState guiRenderState) {
        GpuTextureView gpuTextureView = ((SpecialGuiElementRendererAccessor)(Object)this).sadnes$getTextureView();
        if (gpuTextureView != null) {
            int i = MathHelper.clamp(Math.round(entityPreviewState.alpha() * 255.0F), 0, 255);
            if (i != 0) {
                int i2 = i << 24 | i << 16 | i << 8 | i;
                TextureSetup textureSetup = TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST));
                float f = GlStateSnapshot.isOpenGl() ? 1.0F : 0.0F;
                guiRenderState.addSimpleElementToCurrentLayer(
                    new TexturedQuadGuiElementRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        textureSetup,
                        entityPreviewState.pose(),
                        entityPreviewState.x1(),
                        entityPreviewState.y1(),
                        entityPreviewState.x2(),
                        entityPreviewState.y2(),
                        0.0F,
                        1.0F,
                        f,
                        1.0F - f,
                        i2,
                        entityPreviewState.scissorArea()
                    )
                );
            }
        }
    }

    @Override
    protected void render(EntityPreviewState entityPreviewState, MatrixStack matrixStack) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        minecraftClient.gameRenderer.getDiffuseLighting().setShaderLights(Type.ENTITY_IN_UI);
        Vector3f vector3f = entityPreviewState.translation();
        matrixStack.translate(vector3f.x, vector3f.y, vector3f.z);
        matrixStack.multiply(entityPreviewState.rotation());
        Quaternionf quaternionf = entityPreviewState.overrideCameraAngle();
        CameraRenderState cameraRenderState = new CameraRenderState();
        if (quaternionf != null) {
            cameraRenderState.orientation = quaternionf.conjugate(new Quaternionf()).rotateY((float) Math.PI);
        }

        RenderDispatcher renderDispatcher = minecraftClient.gameRenderer.getEntityRenderDispatcher();
        if (!entityPreviewState.hideModel()) {
            this.entityRenderDispatcher.render(entityPreviewState.renderState(), cameraRenderState, 0.0, 0.0, 0.0, matrixStack, renderDispatcher.getQueue());
            renderDispatcher.render();
        }

        boolean flag = entityPreviewState.chamsTint() >>> 24 != 0;
        boolean flag2 = entityPreviewState.glowTint() >>> 24 != 0;
        if (flag || flag2) {
            this.vertexConsumers.draw();
        }

        if (flag) {
            this.renderChams(entityPreviewState, matrixStack, cameraRenderState);
        }

        if (flag2) {
            this.renderGlow(entityPreviewState);
        }

        Consumer consumer = entityPreviewState.overlay();
        if (consumer != null) {
            matrixStack.push();

            try {
                consumer.accept(matrixStack);
            } finally {
                matrixStack.pop();
            }
        }
    }

    private void renderChams(EntityPreviewState entityPreviewState, MatrixStack matrixStack, CameraRenderState cameraRenderState) {
        SpecialGuiElementRendererAccessor specialGuiElementRendererAccessor = (SpecialGuiElementRendererAccessor)(Object)this;
        GpuTexture gpuTexture = specialGuiElementRendererAccessor.sadnes$getTexture();
        GpuTextureView gpuTextureView = specialGuiElementRendererAccessor.sadnes$getTextureView();
        if (gpuTexture != null && gpuTextureView != null) {
            ModelChamsRenderer.INSTANCE
                .renderPreview(entityPreviewState.renderState(), cameraRenderState, matrixStack, gpuTexture, gpuTextureView, entityPreviewState.chamsTint(), entityPreviewState.chamsStyle(), entityPreviewState.chamsSpeed(), entityPreviewState.chamsScale());
        }
    }

    private void renderGlow(EntityPreviewState entityPreviewState) {
        SpecialGuiElementRendererAccessor specialGuiElementRendererAccessor = (SpecialGuiElementRendererAccessor)(Object)this;
        GpuTexture gpuTexture = specialGuiElementRendererAccessor.sadnes$getTexture();
        GpuTextureView gpuTextureView = specialGuiElementRendererAccessor.sadnes$getTextureView();
        if (gpuTexture != null && gpuTextureView != null) {
            int i = gpuTexture.getWidth(0);
            int i2 = gpuTexture.getHeight(0);
            this.glowSharpTarget = ensureTarget(this.glowSharpTarget, "sadnes_preview_glow_sharp", i, i2);
            this.glowBlur.resize(i, i2);
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            float f = entityPreviewState.glowRadius();

            try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                FullscreenPass.blitUnflipped(Pipelines.GLOW_MASK, this.glowSharpTarget, gpuTextureView, gpuSampler, f);
                this.glowBlur.blur(this.glowSharpTarget.getColorAttachmentView(), f, f, gpuSampler);
            }

            FullscreenPass.drawGlowComposite(
                Pipelines.GLOW_GUI_COMPOSITE, gpuTextureView, this.glowBlur.getColorView(), this.glowSharpTarget.getColorAttachmentView(), gpuSampler, entityPreviewState.glowTint()
            );
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

    @Override
    public void close() {
        if (this.glowSharpTarget != null) {
            this.glowSharpTarget.delete();
        }

        this.glowBlur.close();
        this.glowSharpTarget = null;
        super.close();
    }
}
