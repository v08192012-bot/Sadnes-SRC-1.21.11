package tech.sadnes.api.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.event.impl.SprintResetEvent;
import tech.sadnes.client.feature.impl.movement.NoPush;
import tech.sadnes.client.feature.impl.movement.NoSlow;
import tech.sadnes.client.feature.impl.movement.Sprint;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "canSprintOrFly", at = @At("RETURN"), cancellable = true)
    private void sadnes$allowSprintInHungry(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (!(Boolean)callbackInfoReturnable.getReturnValue() && MinecraftClient.getInstance().player == (Object)this) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent(SprintResetEvent.Reason.HUNGER);
            sprintResetEvent.post();
            if (sprintResetEvent.isCancelled()) {
                callbackInfoReturnable.setReturnValue(true);
            }
        }
    }

    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    private void sadnes$noWaterPush(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (NoPush.getInstance() != null
            && NoPush.getInstance().isEnabled()
            && NoPush.getInstance().pushes.has("water")
            && MinecraftClient.getInstance().player == (Object)this) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void sadnes$allowFallWhileSneaking(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (MinecraftClient.getInstance().player == (Object)this) {
            NoSlow noSlow = NoSlow.getInstance();
            if (noSlow != null && noSlow.shouldAllowFall()) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }

    @Redirect(method = "knockbackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d sadnes$keepVelocityOnAttack(Vec3d vec3d, double d, double d2, double d3) {
        if (MinecraftClient.getInstance().player != (Object)this) {
            return vec3d.multiply(d, d2, d3);
        } else {
            SprintResetEvent sprintResetEvent = new SprintResetEvent(SprintResetEvent.Reason.ATTACK);
            sprintResetEvent.post();
            if (sprintResetEvent.isCancelled()) {
                Sprint sprint = Sprint.getInstance();
                return sprint != null && sprint.isEnabled() && sprint.isGrimMode() ? vec3d.multiply(d, d2, d3) : vec3d;
            } else {
                return vec3d.multiply(d, d2, d3);
            }
        }
    }

    @Redirect(method = "knockbackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"))
    private void sadnes$keepSprintingOnAttack(PlayerEntity playerEntity, boolean flag) {
        if (MinecraftClient.getInstance().player != playerEntity) {
            playerEntity.setSprinting(flag);
        } else {
            SprintResetEvent sprintResetEvent = new SprintResetEvent(SprintResetEvent.Reason.ATTACK);
            sprintResetEvent.post();
            if (!sprintResetEvent.isCancelled()) {
                playerEntity.setSprinting(flag);
            }
        }
    }
}
