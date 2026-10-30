package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.util.player.ServerRotationTracker;

@Mixin(GameRenderer.class)
public abstract class GameRendererCrosshairMixin {
    @Inject(method = "updateCrosshairTarget", at = @At("HEAD"))
    private void sadnes$crosshairTraceStart(float f, CallbackInfo callbackInfo) {
        ServerRotationTracker.enable();
    }

    @Inject(method = "updateCrosshairTarget", at = @At("RETURN"))
    private void sadnes$crosshairTraceEnd(float f, CallbackInfo callbackInfo) {
        ServerRotationTracker.disable();
    }
}
