package tech.sadnes.api.mixin.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.api.duck.TransparentSmoothHolder;
import tech.sadnes.client.event.impl.JumpEvent;
import tech.sadnes.client.event.impl.UsingItemChangeEvent;
import tech.sadnes.client.feature.impl.misc.Test;
import tech.sadnes.client.feature.impl.movement.Speed;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.feature.impl.render.hand.HandOffset;
import tech.sadnes.client.util.IMinecraft;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements TransparentSmoothHolder {
    @Unique
    private float sadnes$transparentSmooth = -1.0F;
    @Unique
    private boolean sadnes$wasUsingItem = false;

    @Shadow
    public abstract boolean isUsingItem();

    @Override
    public float sadnes$transparentSmooth() {
        return this.sadnes$transparentSmooth;
    }

    @Override
    public void sadnes$setTransparentSmooth(float f) {
        this.sadnes$transparentSmooth = f;
    }

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeBadEffects(RegistryEntry<StatusEffect> registryEntry, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if ((Object)this instanceof PlayerEntity) {
            if (registryEntry == StatusEffects.WITHER && Removals.isRemoved("black_hearts")) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void sadnes$onJump(CallbackInfo callbackInfo) {
        if ((Object)this == MinecraftClient.getInstance().player) {
            JumpEvent jumpEvent = new JumpEvent();
            jumpEvent.post();
            if (jumpEvent.isCancelled()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void sadnes$speedTravelPost(Vec3d vec3d, CallbackInfo callbackInfo) {
        if ((Object)this == IMinecraft.mc.player) {
            Speed.stopFallFlyingIfNeeded();
        }
    }

    @Inject(method = "getJumpVelocity()F", at = @At("RETURN"), cancellable = true)
    private void sadnes$jumpStrength(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if ((Object)this == MinecraftClient.getInstance().player) {
            Test.getInstance();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sadnes$trackItemUsage(CallbackInfo callbackInfo) {
        boolean flag = this.isUsingItem();
        if (flag != this.sadnes$wasUsingItem) {
            this.sadnes$wasUsingItem = flag;
            new UsingItemChangeEvent((LivingEntity)(Object)this, flag).post();
        }
    }

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void sadnes$swingDuration(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        if ((Object)this == MinecraftClient.getInstance().player) {
            HandOffset handOffset = HandOffset.getInstance();
            if (handOffset != null) {
                callbackInfoReturnable.setReturnValue(handOffset.adjustSwingDuration(((LivingEntity)(Object)this).getMainHandStack(), (Integer)callbackInfoReturnable.getReturnValue()));
            }
        }
    }
}
