package tech.sadnes.api.mixin.compat;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.render.world.FoliageSwayManager;

@Pseudo
@Mixin(targets = "net/caffeinemc/mods/sodium/client/gl/shader/ShaderLoader", remap = false)
public abstract class SodiumShaderLoaderMixin {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void sadnes$foliageSway(Identifier identifier, CallbackInfoReturnable<String> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(FoliageSwayManager.patchSodiumShader(identifier, (String)callbackInfoReturnable.getReturnValue()));
    }
}
