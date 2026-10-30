package tech.sadnes.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import tech.sadnes.client.render.shader.GlStateSnapshot;

public final class HandCaptureRenderer {
    private static volatile boolean capturing;
    private static volatile Hand captureHand;
    private static volatile float captureProgress = -1.0F;

    private HandCaptureRenderer() {
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static Hand getCaptureHand() {
        return captureHand;
    }

    public static float getCaptureProgress() {
        return captureProgress;
    }

    public static void renderHands(SimpleFramebuffer simpleFramebuffer, MatrixStack matrixStack, float f, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i) {
        renderHandsWith(simpleFramebuffer, matrixStack, f, heldItemRenderer, clientPlayerEntity, i, null, -1.0F);
    }

    public static void renderHandsMainOnly(
        SimpleFramebuffer simpleFramebuffer, MatrixStack matrixStack, float f, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i, boolean flag, float f2
    ) {
        if (flag) {
            renderHandsWith(simpleFramebuffer, matrixStack, f, heldItemRenderer, clientPlayerEntity, i, Hand.MAIN_HAND, f2);
        } else {
            renderHandsWith(simpleFramebuffer, matrixStack, f, heldItemRenderer, clientPlayerEntity, i, null, f2);
        }
    }

    public static void renderHandsWith(
        SimpleFramebuffer simpleFramebuffer, MatrixStack matrixStack, float f, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i, Hand hand, float f2
    ) {
        if (heldItemRenderer != null && clientPlayerEntity != null) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            RenderDispatcher renderDispatcher = minecraftClient.gameRenderer.getEntityRenderDispatcher();
            GlStateSnapshot glStateSnapshot = GlStateSnapshot.capture();
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(simpleFramebuffer.getColorAttachment(), 0, simpleFramebuffer.getDepthAttachment(), 1.0);
            GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride;
            GpuTextureView gpuTextureView2 = RenderSystem.outputDepthTextureOverride;
            RenderSystem.outputColorTextureOverride = simpleFramebuffer.getColorAttachmentView();
            RenderSystem.outputDepthTextureOverride = simpleFramebuffer.getDepthAttachmentView();
            capturing = true;
            captureHand = hand;
            captureProgress = f2;

            try {
                heldItemRenderer.renderItem(f, matrixStack, renderDispatcher.getQueue(), clientPlayerEntity, i);
            } finally {
                capturing = false;
                captureHand = null;
                captureProgress = -1.0F;
                RenderSystem.outputColorTextureOverride = gpuTextureView;
                RenderSystem.outputDepthTextureOverride = gpuTextureView2;
                glStateSnapshot.restore();
            }
        }
    }
}
