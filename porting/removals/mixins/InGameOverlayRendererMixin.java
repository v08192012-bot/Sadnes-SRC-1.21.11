package tech.sadnes.api.mixin.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.movement.NoPush;
import tech.sadnes.client.feature.impl.render.Removals;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void sadnes$removeFireOverlay(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Sprite sprite, CallbackInfo callbackInfo) {
        if (Removals.isRemoved("fire")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void sadnes$removeUnderwaterOverlay(MinecraftClient minecraftClient, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
        if (Removals.isRemoved("water")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void sadnes$removeInWallOverlay(Sprite sprite, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, CallbackInfo callbackInfo) {
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("blocks")) {
            callbackInfo.cancel();
        }
    }
}
