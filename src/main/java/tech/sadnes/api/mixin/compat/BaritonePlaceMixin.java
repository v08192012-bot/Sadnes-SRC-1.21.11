package tech.sadnes.api.mixin.compat;

import baritone.utils.BlockPlaceHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.misc.AutoBuild;

@Mixin(value = BlockPlaceHelper.class, remap = false)
public class BaritonePlaceMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void sadnes$clickOnlyAfterHeadArrives(boolean flag, CallbackInfo callbackInfo) {
        if (flag && AutoBuild.isLookDiverged()) {
            callbackInfo.cancel();
        }
    }
}
