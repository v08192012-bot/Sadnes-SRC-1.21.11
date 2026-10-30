package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.cosmetic.SwordCosmeticRenderer;
import tech.sadnes.client.feature.impl.render.hand.HandOffset;

@Mixin(targets = "net/minecraft/client/render/item/ItemRenderState$LayerRenderState")
public abstract class LayerRenderStateMixin {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            shift = Shift.AFTER,
            target = "Lnet/minecraft/client/render/model/json/Transformation;apply(ZLnet/minecraft/client/util/math/MatrixStack$Entry;)V"
        )
    )
    private void sadnes$captureLayerTransform(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, int i2, int i3, CallbackInfo callbackInfo) {
        if (HandOffset.isCapturing()) {
            HandOffset handOffset = HandOffset.getInstance();
            if (handOffset != null) {
                handOffset.captureMatrix(matrixStack);
            }
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"
        )
    )
    private void sadnes$captureHandItemBounds(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, int i2, int i3, CallbackInfo callbackInfo) {
        if (HandOffset.isCapturing()) {
            HandOffset handOffset = HandOffset.getInstance();
            if (handOffset != null) {
                handOffset.captureQuads(matrixStack, ((LayerRenderState)(Object)this).getQuads());
            }
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"
        )
    )
    private void sadnes$cosmeticSword(
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        MatrixStack matrixStack,
        ItemDisplayContext itemDisplayContext,
        int i,
        int i2,
        int i3,
        int[] ints,
        List<BakedQuad> bakedQuads,
        RenderLayer renderLayer,
        Glint glint,
        Operation<Void> operation
    ) {
        boolean flag = SwordCosmeticRenderer.render(matrixStack, orderedRenderCommandQueue, i);
        if (!flag) {
            operation.call(orderedRenderCommandQueue, matrixStack, itemDisplayContext, i, i2, i3, ints, bakedQuads, renderLayer, glint);
        }
    }
}
