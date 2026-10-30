package tech.sadnes.api.mixin.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.movement.NoPush;

@Mixin(Entity.class)
public abstract class NoPushEntityMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void sadnes$noMechanismPush(MovementType movementType, Vec3d vec3d, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if ((movementType == MovementType.PISTON || movementType == MovementType.SHULKER_BOX || movementType == MovementType.SHULKER)
            && NoPush.getInstance() != null
            && NoPush.getInstance().isEnabled()
            && NoPush.getInstance().pushes.has("mechanisms")
            && minecraftClient.player == (Object)this) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void sadnes$noEntityPush(Entity entity, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("entities") && minecraftClient.player != null) {
            Entity entity2 = (Entity)(Object)this;
            if (entity2 == minecraftClient.player || entity == minecraftClient.player) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    private void sadnes$noWaterPush(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("water") && minecraftClient.player == (Object)this) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "onBubbleColumnCollision", at = @At("HEAD"), cancellable = true)
    private void sadnes$noBubbleColumnPush(boolean flag, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("bubbles") && minecraftClient.player == (Object)this) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onBubbleColumnSurfaceCollision", at = @At("HEAD"), cancellable = true)
    private void sadnes$noBubbleColumnSurfacePush(boolean flag, BlockPos blockPos, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("bubbles") && minecraftClient.player == (Object)this) {
            callbackInfo.cancel();
        }
    }
}
