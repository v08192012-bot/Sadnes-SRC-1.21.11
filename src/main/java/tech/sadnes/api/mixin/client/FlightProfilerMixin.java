package tech.sadnes.api.mixin.client;

import java.util.Optional;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FlightProfiler.class)
public interface FlightProfilerMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/Optional;isPresent()Z"))
    private static boolean sadnes$disableJfr(Optional<?> optional) {
        return false;
    }
}
