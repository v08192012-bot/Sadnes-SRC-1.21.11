package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tech.sadnes.client.feature.impl.misc.FreeCam;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererCullingMixin {
    @ModifyVariable(method = "updateCamera", at = @At("HEAD"), argsOnly = true, index = 3)
    private boolean sadnes$freeCamSeesThroughTerrain(boolean flag) {
        if (flag) {
            return true;
        }

        FreeCam freeCam = FreeCam.getInstance();
        return freeCam != null && freeCam.isEnabled();
    }
}
