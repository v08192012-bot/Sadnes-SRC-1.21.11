package tech.sadnes.api.mixin.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Precipitation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.event.impl.WeatherEvent;
import tech.sadnes.client.event.impl.WorldTimeEvent;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;

@Mixin(World.class)
public abstract class WorldAmbienceMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void sadnes$ambienceTime(CallbackInfoReturnable<Long> callbackInfoReturnable) {
        if ((Object)this instanceof ClientWorld) {
            WorldTimeEvent worldTimeEvent = new WorldTimeEvent((World)(Object)this, (Long)callbackInfoReturnable.getReturnValue());
            worldTimeEvent.post();
            callbackInfoReturnable.setReturnValue(worldTimeEvent.getTime());
        }
    }

    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void sadnes$ambienceRain(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if ((Object)this instanceof ClientWorld) {
            WeatherEvent weatherEvent = new WeatherEvent((World)(Object)this, WeatherEvent.Type.RAIN, (Float)callbackInfoReturnable.getReturnValue());
            weatherEvent.post();
            callbackInfoReturnable.setReturnValue(weatherEvent.getGradient());
        }
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void sadnes$ambienceThunder(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if ((Object)this instanceof ClientWorld) {
            WeatherEvent weatherEvent = new WeatherEvent((World)(Object)this, WeatherEvent.Type.THUNDER, (Float)callbackInfoReturnable.getReturnValue());
            weatherEvent.post();
            callbackInfoReturnable.setReturnValue(weatherEvent.getGradient());
        }
    }

    @Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true)
    private void sadnes$forceSnow(BlockPos blockPos, CallbackInfoReturnable<Precipitation> callbackInfoReturnable) {
        if ((Object)this instanceof ClientWorld) {
            Ambience ambience = Ambience.getInstance();
            if (ambience != null && ambience.isSnowing()) {
                callbackInfoReturnable.setReturnValue(Precipitation.SNOW);
            }
        }
    }
}
