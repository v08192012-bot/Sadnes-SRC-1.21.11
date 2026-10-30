package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.feature.impl.render.TransparentPlayers;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyArg(
        method = "renderBakedItemQuads",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;FFFFII)V"
        ),
        index = 5
    )
    private static float sadnes$quadAlpha(float f) {
        float f2 = TransparentPlayers.getRenderAlpha();
        return f2 >= 1.0F ? f : f * f2;
    }

    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true, index = 8)
    private static Glint sadnes$removeGlint(Glint glint) {
        return Removals.isRemoved("enchant_glint") ? Glint.NONE : glint;
    }
}
