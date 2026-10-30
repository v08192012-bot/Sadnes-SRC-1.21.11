package tech.sadnes.api.mixin.entity;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.component.ThrowComponent;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void sadnes$lockSelectedSlotDuringSwap(int i, CallbackInfo callbackInfo) {
        ThrowComponent throwComponent = ThrowComponent.getInstance();
        if (throwComponent != null && throwComponent.isSwapping()) {
            callbackInfo.cancel();
        }
    }
}
