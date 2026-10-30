package tech.sadnes.api.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.impl.EntityBoundingBoxEvent;
import tech.sadnes.client.event.impl.TargetingMarginEvent;
import tech.sadnes.client.feature.component.WindowClickTrackerComponent;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.player.BubbleColumnUtil;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private Box boundingBox;

    @Inject(method = "move", at = @At("TAIL"))
    private void sadnes$legacyBubbleColumns(MovementType movementType, Vec3d vec3d, CallbackInfo callbackInfo) {
        Entity entity = (Entity)(Object)this;
        if (entity == MinecraftClient.getInstance().player && ProtocolUtil.isLegacyServer()) {
            BubbleColumnUtil.applyBubbleColumns(entity);
        }
    }

    @ModifyVariable(method = "updateSubmergedInWaterState", at = @At("STORE"), ordinal = 0)
    private double sadnes$legacyEyeInWaterOffset(double d) {
        Entity entity = (Entity)(Object)this;
        return entity == MinecraftClient.getInstance().player && ProtocolUtil.isLegacyServer() ? d - 0.11111111F : d;
    }

    @ModifyReturnValue(method = "getBoundingBox", at = @At("RETURN"))
    private Box sadnes$controlBoundingBox(Box box) {
        if (!Sadnes.getEventBus().hasListeners(EntityBoundingBoxEvent.class)) {
            return box;
        }

        Entity entity = (Entity)(Object)this;
        World world = entity.getEntityWorld();
        return world != null && world.isClient() ? EntityBoundingBoxEvent.fire(box, entity) : box;
    }

    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void sadnes$onTargetingMargin(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        TargetingMarginEvent targetingMarginEvent = new TargetingMarginEvent((Entity)(Object)this, callbackInfoReturnable.getReturnValueF());
        targetingMarginEvent.post();
        callbackInfoReturnable.setReturnValue(targetingMarginEvent.getMargin());
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void sadnes$removePlayerGlow(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if ((Object)this instanceof PlayerEntity && Removals.isRemoved("player_glow")) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "setYaw", at = @At("HEAD"), cancellable = true)
    private void sadnes$preventYawInContainer(float f, CallbackInfo callbackInfo) {
        if ((Object)this == MinecraftClient.getInstance().player && WindowClickTrackerComponent.getInstance().isInteracting()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "setPitch", at = @At("HEAD"), cancellable = true)
    private void sadnes$preventPitchInContainer(float f, CallbackInfo callbackInfo) {
        if ((Object)this == MinecraftClient.getInstance().player && WindowClickTrackerComponent.getInstance().isInteracting()) {
            callbackInfo.cancel();
        }
    }
}
