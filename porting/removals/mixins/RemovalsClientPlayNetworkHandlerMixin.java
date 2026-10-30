package tech.sadnes.api.mixin.network;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.render.Removals;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class RemovalsClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeTotemAnimation(EntityStatusS2CPacket entityStatusS2CPacket, CallbackInfo callbackInfo) {
        if (entityStatusS2CPacket.getStatus() == 35 && Removals.isRemoved("totem_anim")) {
            callbackInfo.cancel();
        }
    }
}
