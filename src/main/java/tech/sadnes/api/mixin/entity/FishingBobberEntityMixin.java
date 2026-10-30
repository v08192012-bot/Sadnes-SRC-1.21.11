package tech.sadnes.api.mixin.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.movement.NoPush;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @Inject(method = "pullHookedEntity", at = @At("HEAD"), cancellable = true)
    private void sadnes$noFishingRodPush(Entity entity, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("fishing_rod") && entity == minecraftClient.player) {
            callbackInfo.cancel();
        }
    }
}
