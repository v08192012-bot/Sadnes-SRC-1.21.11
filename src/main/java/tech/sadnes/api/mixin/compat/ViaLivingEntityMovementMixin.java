package tech.sadnes.api.mixin.compat;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.compat.ProtocolUtil;

@Mixin(LivingEntity.class)
public abstract class ViaLivingEntityMovementMixin {
    @Inject(method = "getMovementSpeed(F)F", at = @At("HEAD"), cancellable = true)
    private void sadnes$use26_2MinimumFriction(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (IMinecraft.mc.player != null) {
            LivingEntity livingEntity = (LivingEntity)(Object)this;
            boolean flag = livingEntity == IMinecraft.mc.player || livingEntity.getControllingPassenger() == IMinecraft.mc.player;
            if (flag && !ProtocolUtil.isOlderThan("26.2") && livingEntity.isOnGround() && !(f > 0.6)) {
                callbackInfoReturnable.setReturnValue(livingEntity.getMovementSpeed());
            }
        }
    }
}
