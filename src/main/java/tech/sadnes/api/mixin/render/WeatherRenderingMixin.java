package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Precipitation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;

@Mixin(WeatherRendering.class)
public abstract class WeatherRenderingMixin {
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void sadnes$forcedPrecipitation(World world, BlockPos blockPos, CallbackInfoReturnable<Precipitation> callbackInfoReturnable) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null) {
            Precipitation precipitation = ambience.getPrecipitation();
            if (precipitation != null) {
                callbackInfoReturnable.setReturnValue(precipitation);
            }
        }
    }

    @Inject(method = "buildPrecipitationPieces", at = @At("HEAD"), cancellable = true)
    private void sadnes$shaderPrecipitation(World world, int i, float f, Vec3d vec3d, WeatherRenderState weatherRenderState, CallbackInfo callbackInfo) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isWeatherEnabled()) {
            callbackInfo.cancel();
        }
    }

    @WrapOperation(
        method = "addParticlesAndSound",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V")
    )
    private void sadnes$shaderRainSplashes(
        ClientWorld clientWorld, ParticleEffect particleEffect, double d, double d2, double d3, double d4, double d5, double d6, Operation<Void> operation
    ) {
        Ambience ambience = Ambience.getInstance();
        if (particleEffect != ParticleTypes.RAIN || ambience == null || !ambience.isWeatherEnabled()) {
            operation.call(clientWorld, particleEffect, d, d2, d3, d4, d5, d6);
        }
    }
}
