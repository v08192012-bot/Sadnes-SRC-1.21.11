package tech.sadnes.api.mixin.network;

import java.util.UUID;
import net.minecraft.client.network.SocialInteractionsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SocialInteractionsManager.class)
public class SocialInteractionsManagerMixin {
    @Inject(method = "isPlayerBlocked", at = @At("HEAD"), cancellable = true)
    private void sadnes$skipBlockingBlockListFetch(UUID uuid, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(false);
    }
}
