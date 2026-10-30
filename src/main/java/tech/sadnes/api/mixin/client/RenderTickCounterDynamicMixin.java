package tech.sadnes.api.mixin.client;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.Sadnes;

@Mixin(Dynamic.class)
public class RenderTickCounterDynamicMixin {
    @Shadow
    private float dynamicDeltaTicks;
    @Shadow
    private float tickProgress;
    @Shadow
    private long lastTimeMillis;
    @Shadow
    @Final
    private float tickTime;
    @Shadow
    @Final
    private FloatUnaryOperator targetMillisPerTick;

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
    private void sadnes$applyGameSpeed(long l, CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        if (Sadnes.guiScale != 1.0F) {
            this.dynamicDeltaTicks = (float)(l - this.lastTimeMillis) / this.targetMillisPerTick.apply(this.tickTime) * Sadnes.guiScale;
            this.lastTimeMillis = l;
            this.tickProgress = this.tickProgress + this.dynamicDeltaTicks;
            int i = (int)this.tickProgress;
            this.tickProgress -= i;
            callbackInfoReturnable.setReturnValue(i);
        }
    }
}
