package tech.sadnes.api.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.InactivityFpsLimiter;
import net.minecraft.client.option.InactivityFpsLimiter.LimitReason;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InactivityFpsLimiter.class)
public abstract class InactivityFpsLimiterMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private long lastInputTime;

    @Inject(method = "getLimitReason", at = @At("HEAD"), cancellable = true)
    private void sadnes$limitReason(CallbackInfoReturnable<LimitReason> callbackInfoReturnable) {
        Object object = this.client.options.getInactivityFpsLimit().getValue();
        String string = ((StringIdentifiable)object).asString();
        if (string.equals("never")) {
            callbackInfoReturnable.setReturnValue(LimitReason.NONE);
        } else if (this.client.getWindow().isMinimized()) {
            callbackInfoReturnable.setReturnValue(LimitReason.WINDOW_ICONIFIED);
        } else {
            if (string.equals("afk")) {
                long l = Util.getMeasuringTimeMs() - this.lastInputTime;
                if (l > 600000L) {
                    callbackInfoReturnable.setReturnValue(LimitReason.LONG_AFK);
                    return;
                }

                if (l > 60000L) {
                    callbackInfoReturnable.setReturnValue(LimitReason.SHORT_AFK);
                    return;
                }
            }

            callbackInfoReturnable.setReturnValue(LimitReason.NONE);
        }
    }
}
