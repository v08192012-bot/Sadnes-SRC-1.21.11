package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;

@Mixin(SkyRendering.class)
public abstract class SkyRenderingHazeMixin {
    @Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true)
    private void sadnes$disableSkyHaze(CallbackInfo callbackInfo) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isEnabled() && ambience.getActiveProfile().atmosphere.isEnabled()) {
            callbackInfo.cancel();
        }
    }
}
