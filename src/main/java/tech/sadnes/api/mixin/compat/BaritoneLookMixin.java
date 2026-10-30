package tech.sadnes.api.mixin.compat;

import baritone.api.BaritoneAPI;
import baritone.api.utils.Rotation;
import baritone.behavior.LookBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.misc.AutoBuild;
import tech.sadnes.client.feature.impl.movement.Baritone;

@Mixin(value = LookBehavior.class, remap = false)
public class BaritoneLookMixin {
    @ModifyVariable(method = "updateTarget", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Rotation sadnes$easePathRotation(Rotation rotation, Rotation rotation2, boolean flag) {
        Baritone baritone = Baritone.getInstance();
        return baritone == null ? rotation : baritone.modifyRotation(BaritoneAPI.getProvider().getPrimaryBaritone(), rotation, flag);
    }

    @Inject(method = "updateTarget", at = @At("HEAD"), cancellable = true, require = 0)
    private void sadnes$buildAim(Rotation rotation, boolean flag, CallbackInfo callbackInfo) {
        if (flag && AutoBuild.handleLook(rotation.getYaw(), rotation.getPitch())) {
            callbackInfo.cancel();
        }
    }
}
