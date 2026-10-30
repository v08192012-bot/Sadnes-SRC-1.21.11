package tech.sadnes.api.mixin.accessor;

import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.RenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderDispatcher.class)
public interface RenderDispatcherAccessor {
    @Accessor("vertexConsumers")
    Immediate sadnes$getVertexConsumers();
}
