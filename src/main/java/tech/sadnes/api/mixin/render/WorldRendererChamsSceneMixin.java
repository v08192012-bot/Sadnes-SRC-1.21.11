package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.render.ModelChamsRenderer;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererChamsSceneMixin {
    @Inject(method = "pushEntityRenders", at = @At("HEAD"))
    private void sadnes$capturePreEntityScene(CallbackInfo callbackInfo) {
        ModelChamsRenderer.INSTANCE.capturePreScene();
    }
}
