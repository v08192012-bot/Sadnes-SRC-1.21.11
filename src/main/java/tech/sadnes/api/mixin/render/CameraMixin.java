package tech.sadnes.api.mixin.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.api.mixin.accessor.CameraAccessor;
import tech.sadnes.client.animation.PerspectiveAnimation;
import tech.sadnes.client.event.impl.CameraRotationEvent;
import tech.sadnes.client.feature.impl.misc.FreeCam;
import tech.sadnes.client.feature.impl.render.CameraTweaks;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;
    @Unique
    private float sadnes$tickProgress;
    @Unique
    private boolean sadnes$rotationApplied;
    @Unique
    private float sadnes$cameraDistance = 4.0F;
    @Unique
    private float sadnes$desiredDistance = 4.0F;
    @Unique
    private boolean sadnes$clippingArc;
    @Unique
    private float sadnes$clipDistance = -1.0F;
    @Unique
    private long sadnes$clipTime;

    @Shadow
    protected abstract void setRotation(float f, float f2);

    @Shadow
    protected abstract void setPos(Vec3d vec3d);

    @Shadow
    protected abstract void moveBy(float f, float f2, float f3);

    @Shadow
    private float clipToSpace(float f) {
        throw new AssertionError();
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void sadnes$onUpdateHead(World world, Entity entity, boolean flag, boolean flag2, float f, CallbackInfo callbackInfo) {
        this.sadnes$tickProgress = f;
        this.sadnes$rotationApplied = false;
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = Shift.AFTER))
    private void sadnes$onAfterSetPos(World world, Entity entity, boolean flag, boolean flag2, float f, CallbackInfo callbackInfo) {
        this.sadnes$applyRotationOverride();
    }

    @Inject(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(Lnet/minecraft/util/math/Vec3d;)V", ordinal = 0, shift = Shift.AFTER)
    )
    private void sadnes$onAfterSetPosVec(World world, Entity entity, boolean flag, boolean flag2, float f, CallbackInfo callbackInfo) {
        this.sadnes$applyRotationOverride();
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void sadnes$cameraTweaksDistance(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (!this.sadnes$clippingArc) {
            this.sadnes$desiredDistance = f;
        }

        CameraTweaks cameraTweaks = CameraTweaks.getInstance();
        if (cameraTweaks != null && cameraTweaks.isEnabled() && cameraTweaks.disableCollisions.getValue()) {
            if (this.sadnes$clippingArc) {
                callbackInfoReturnable.setReturnValue(f);
            } else {
                this.sadnes$cameraDistance = cameraTweaks.distance.getValue();
                this.sadnes$desiredDistance = this.sadnes$cameraDistance;
                callbackInfoReturnable.setReturnValue(this.sadnes$cameraDistance);
            }
        }
    }

    @Inject(method = "clipToSpace", at = @At("RETURN"))
    private void sadnes$captureCameraDistance(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        this.sadnes$cameraDistance = (Float)callbackInfoReturnable.getReturnValue();
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void sadnes$animatePerspective(World world, Entity entity, boolean flag, boolean flag2, float f, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.options != null) {
            PerspectiveAnimation perspectiveAnimation = PerspectiveAnimation.INSTANCE;
            Perspective perspective = minecraftClient.options.getPerspective();
            float f2 = perspective.isFrontView() ? this.yaw - 180.0F : this.yaw;
            float f3 = perspective.isFrontView() ? -this.pitch : this.pitch;
            perspectiveAnimation.setCameraRotation(f2, f3);
            perspectiveAnimation.setMaxDistance(this.sadnes$desiredDistance);
            if (!perspectiveAnimation.update(perspective)) {
                this.sadnes$clipDistance = -1.0F;
                this.sadnes$clipTime = 0L;
            } else {
                FreeCam freeCam = FreeCam.getInstance();
                if (freeCam == null || !freeCam.isEnabled()) {
                    if (!(entity instanceof LivingEntity) || !((LivingEntity)entity).isSleeping()) {
                        if (!perspective.isFirstPerson()) {
                            this.moveBy(this.sadnes$cameraDistance, 0.0F, 0.0F);
                        }

                        float f4 = f3 - perspectiveAnimation.getYawOffset();
                        CameraMixin cameraMixin;
                        float f5;
                        float f6;
                        if (f4 < -90.0F) {
                            cameraMixin = this;
                            f5 = f2 + 180.0F;
                            f6 = -180.0F - f4;
                        } else {
                            cameraMixin = this;
                            f5 = f2;
                            f6 = f4;
                        }

                        cameraMixin.setRotation(f5, f6);
                        ((CameraAccessor)this).sadnes$setThirdPerson(perspectiveAnimation.isThirdPerson());
                        float f7 = perspectiveAnimation.getDistanceFactor() * this.sadnes$desiredDistance;
                        if (!(f7 <= 0.05F)) {
                            this.sadnes$clippingArc = true;

                            float f8;
                            try {
                                f8 = this.clipToSpace(f7);
                            } finally {
                                this.sadnes$clippingArc = false;
                            }

                            this.moveBy(-this.sadnes$smoothClip(f8), 0.0F, 0.0F);
                        }
                    }
                }
            }
        }
    }

    @Unique
    private float sadnes$smoothClip(float f) {
        long l = System.currentTimeMillis();
        float f2 = this.sadnes$clipTime == 0L ? 0.0F : Math.min((float)(l - this.sadnes$clipTime) / 1000.0F, 0.1F);
        this.sadnes$clipTime = l;
        if (!(this.sadnes$clipDistance < 0.0F) && !(f <= this.sadnes$clipDistance)) {
            this.sadnes$clipDistance = this.sadnes$clipDistance + (f - this.sadnes$clipDistance) * (1.0F - (float)Math.exp(-f2 * 14.0F));
            return this.sadnes$clipDistance;
        } else {
            this.sadnes$clipDistance = f;
            return this.sadnes$clipDistance;
        }
    }

    @Unique
    private void sadnes$applyRotationOverride() {
        if (!this.sadnes$rotationApplied) {
            this.sadnes$rotationApplied = true;
            FreeCam freeCam = FreeCam.getInstance();
            if (freeCam != null && freeCam.isEnabled()) {
                this.setPos(freeCam.getInterpolatedPosition(this.sadnes$tickProgress));
                ((CameraAccessor)this).sadnes$setThirdPerson(true);
            }

            CameraRotationEvent cameraRotationEvent = new CameraRotationEvent(this.yaw, this.pitch, this.sadnes$tickProgress);
            cameraRotationEvent.post();
            this.setRotation(cameraRotationEvent.getYaw(), cameraRotationEvent.getPitch());
        }
    }
}
