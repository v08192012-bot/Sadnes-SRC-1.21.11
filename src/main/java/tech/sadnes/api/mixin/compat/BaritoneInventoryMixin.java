package tech.sadnes.api.mixin.compat;

import baritone.behavior.InventoryBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.util.player.ServerInputTracker;

@Mixin(value = InventoryBehavior.class, remap = false)
public class BaritoneInventoryMixin {
    @Inject(method = "requestSwapWithHotBar", at = @At("HEAD"), cancellable = true, require = 0)
    private void sadnes$swapOnlyWhileServerSeesStanding(int i, int i2, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (!ServerInputTracker.isStandingStill()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "requestSwapWithHotBar", at = @At("RETURN"), require = 0)
    private void sadnes$closeInventoryAfterSwap(int i, int i2, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        boolean flag = (Boolean)callbackInfoReturnable.getReturnValue() && clientPlayerEntity != null;
        if (flag) {
            clientPlayerEntity.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(clientPlayerEntity.playerScreenHandler.syncId));
        }
    }
}
