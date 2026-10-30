package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tech.sadnes.client.cosmetic.SwordCosmeticRenderer;
import tech.sadnes.client.feature.impl.render.Cosmetics;

@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin {
    @WrapOperation(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"
        )
    )
    private void sadnes$cosmeticSword(
        ItemRenderState itemRenderState,
        MatrixStack matrixStack,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int i,
        int i2,
        int i3,
        Operation<Void> operation,
        @Local(argsOnly = true) ArmedEntityRenderState armedEntityRenderState,
        @Local(argsOnly = true) ItemStack itemStack
    ) {
        SwordCosmeticRenderer.prepare(armedEntityRenderState.getData(Cosmetics.COSMETICS_KEY), itemStack);

        try {
            operation.call(itemRenderState, matrixStack, orderedRenderCommandQueue, i, i2, i3);
        } finally {
            SwordCosmeticRenderer.clear();
        }
    }
}
