package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.render.Removals;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererRemovalsMixin {
    @Shadow
    private ClientWorld world;

    @Inject(method = "fillEntityOutlineRenderStates", at = @At("RETURN"))
    private void sadnes$hideRemovedBlockOutline(Camera camera, WorldRenderState worldRenderState, CallbackInfo callbackInfo) {
        if (worldRenderState.outlineRenderState != null && Removals.isBlockHidden(this.world.getBlockState(worldRenderState.outlineRenderState.pos()).getBlock())) {
            worldRenderState.outlineRenderState = null;
        }
    }
}
