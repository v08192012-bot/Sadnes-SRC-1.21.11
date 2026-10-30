package tech.sadnes.api.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.render.world.FoliageSwayManager;

@Pseudo
@Mixin(targets = "net/caffeinemc/mods/sodium/client/render/chunk/shader/DefaultShaderInterface", remap = false)
public abstract class SodiumChunkShaderMixin {
    @Inject(method = "setupState", at = @At("TAIL"), remap = false, require = 0)
    private void sadnes$foliageWind(CallbackInfo callbackInfo) {
        FoliageSwayManager.INSTANCE.uploadSodiumUniforms();
    }
}
