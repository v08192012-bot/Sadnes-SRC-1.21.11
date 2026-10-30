package tech.sadnes.api.mixin.render;

import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.render.Cosmetics;
import tech.sadnes.client.feature.impl.render.TransparentPlayers;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sadnes$hideUnderBackpack(
        MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, PlayerEntityRenderState playerEntityRenderState, float f, float f2, CallbackInfo callbackInfo
    ) {
        if (Cosmetics.hasBodywear(playerEntityRenderState.getData(Cosmetics.COSMETICS_KEY))) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayers;entitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
        )
    )
    private RenderLayer sadnes$capeLayer(Identifier identifier) {
        return TransparentPlayers.getRenderAlpha() < 1.0F ? RenderLayers.entityTranslucent(identifier) : RenderLayers.entitySolid(identifier);
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        )
    )
    private void sadnes$capeAlpha(
        OrderedRenderCommandQueue orderedRenderCommandQueue, Model model, Object object, MatrixStack matrixStack, RenderLayer renderLayer, int i, int i2, int i3, CrumblingOverlayCommand crumblingOverlayCommand
    ) {
        TransparentPlayers transparentPlayers = TransparentPlayers.getInstance();
        if (transparentPlayers != null && !(TransparentPlayers.getRenderAlpha() >= 1.0F)) {
            orderedRenderCommandQueue.submitModel(model, object, matrixStack, renderLayer, i, i2, transparentPlayers.applyRenderAlpha(-1), null, i3, crumblingOverlayCommand);
        } else {
            orderedRenderCommandQueue.submitModel(model, object, matrixStack, renderLayer, i, i2, i3, crumblingOverlayCommand);
        }
    }
}
