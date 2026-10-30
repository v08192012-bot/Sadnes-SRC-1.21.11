package tech.sadnes.api.mixin.render;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.render.Removals;

@Mixin(StatusEffectFogModifier.class)
public abstract class StatusEffectFogMixin {
    @Inject(method = "shouldApply", at = @At("HEAD"), cancellable = true)
    private void sadnes$skipBadEffectFog(CameraSubmersionType cameraSubmersionType, Entity entity, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Removals.isRemoved("bad_effects")) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }
}
