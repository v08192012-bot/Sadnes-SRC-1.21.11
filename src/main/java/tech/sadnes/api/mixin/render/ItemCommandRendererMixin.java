package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.ItemCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl.ItemCommand;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.api.duck.ItemAlphaHolder;
import tech.sadnes.client.feature.impl.render.TransparentPlayers;

@Mixin(ItemCommandRenderer.class)
public abstract class ItemCommandRendererMixin {
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"
        )
    )
    private void sadnes$itemAlpha(
        ItemDisplayContext itemDisplayContext,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int i,
        int i2,
        int[] ints,
        List<BakedQuad> bakedQuads,
        RenderLayer renderLayer,
        Glint glint,
        @Local ItemCommand itemCommand
    ) {
        float f = ((ItemAlphaHolder)(Object)itemCommand).sadnes$itemAlpha();
        if (f >= 1.0F) {
            ItemRenderer.renderItem(itemDisplayContext, matrixStack, vertexConsumerProvider, i, i2, ints, bakedQuads, renderLayer, glint);
        } else {
            TransparentPlayers.setRenderAlpha(f);

            try {
                ItemRenderer.renderItem(itemDisplayContext, matrixStack, vertexConsumerProvider, i, i2, ints, bakedQuads, TexturedRenderLayers.getItemTranslucentCull(), glint);
            } finally {
                TransparentPlayers.resetRenderAlpha();
            }
        }
    }
}
