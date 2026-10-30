package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.event.impl.FogColorEvent;
import tech.sadnes.client.event.impl.FogDistanceEvent;
import tech.sadnes.client.render.shader.AmbienceRenderer;

@Mixin(AtmosphericFogModifier.class)
public abstract class AtmosphericFogModifierMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void sadnes$ambienceFogColor(ClientWorld clientWorld, Camera camera, int i, float f, CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        FogColorEvent fogColorEvent = new FogColorEvent(clientWorld, camera, i, f, (Integer)callbackInfoReturnable.getReturnValue());
        fogColorEvent.post();
        callbackInfoReturnable.setReturnValue(fogColorEvent.getColor());
        AmbienceRenderer.setHorizonColor(fogColorEvent.getColor());
    }

    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void sadnes$ambienceFogDistance(FogData fogData, Camera camera, ClientWorld clientWorld, float f, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        FogDistanceEvent fogDistanceEvent = new FogDistanceEvent(
            clientWorld, camera, fogData.environmentalStart, fogData.environmentalEnd, fogData.renderDistanceStart, fogData.renderDistanceEnd, fogData.skyEnd, fogData.cloudEnd
        );
        fogDistanceEvent.post();
        fogData.environmentalStart = fogDistanceEvent.getEnvironmentalStart();
        fogData.environmentalEnd = fogDistanceEvent.getRenderDistanceStart();
        fogData.renderDistanceStart = fogDistanceEvent.getEnvironmentalEnd();
        fogData.renderDistanceEnd = fogDistanceEvent.getRenderDistanceEnd();
        fogData.skyEnd = fogDistanceEvent.getSkyEnd();
        fogData.cloudEnd = fogDistanceEvent.getCloudEnd();
    }
}
