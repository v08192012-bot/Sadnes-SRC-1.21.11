package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.event.impl.SkyColorEvent;
import tech.sadnes.client.render.shader.AmbienceRenderer;

@Mixin(SkyRendering.class)
public abstract class SkyRenderingAmbienceMixin {
    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void sadnes$ambienceSkyColor(ClientWorld clientWorld, float f, Camera camera, SkyRenderState skyRenderState, CallbackInfo callbackInfo) {
        SkyColorEvent skyColorEvent = new SkyColorEvent(clientWorld, camera, f, skyRenderState.skyColor);
        skyColorEvent.post();
        skyRenderState.skyColor = skyColorEvent.getColor();
        AmbienceRenderer.setSkyColor(skyRenderState.skyColor, skyRenderState.sunAngle);
    }
}
