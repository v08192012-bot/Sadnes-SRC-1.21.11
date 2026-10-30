package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.figura.FiguraAvatarManager;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sadnes$skipArmorUnderModel(
        MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, BipedEntityRenderState bipedEntityRenderState, float f, float f2, CallbackInfo callbackInfo
    ) {
        if (FiguraAvatarManager.hasCustomAvatar(bipedEntityRenderState)) {
            callbackInfo.cancel();
        }
    }
}
