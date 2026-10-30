package tech.sadnes.api.mixin.accessor;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {
    @Invoker("setupTransforms")
    void sadnes$setupTransforms(LivingEntityRenderState livingEntityRenderState, MatrixStack matrixStack, float f, float f2);

    @Invoker("scale")
    void sadnes$scale(LivingEntityRenderState livingEntityRenderState, MatrixStack matrixStack);
}
