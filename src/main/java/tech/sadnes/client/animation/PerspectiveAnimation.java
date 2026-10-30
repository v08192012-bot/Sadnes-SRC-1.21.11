package tech.sadnes.client.animation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.render.Animations;

public final class PerspectiveAnimation {
    public static final PerspectiveAnimation INSTANCE = new PerspectiveAnimation();
    private final Animation transitionAnimation = new Animation();
    private Perspective perspective;
    private float startDistance;
    private float startYaw;
    private float targetDistance;
    private float targetYaw;
    private float distanceFactor;
    private float yawOffset;
    private boolean animating;
    private float cameraYaw;
    private float cameraPitch;
    private float maxDistance = 4.0F;
    private boolean rotationCaptured;
    private long lastHurtTime;
    private int lastHurtTicks;

    private PerspectiveAnimation() {
    }

    public boolean update(Perspective perspective2) {
        this.trackHurt();
        Animations animations = Animations.getInstance();
        boolean flag;
        if (animations != null) {
            flag = animations.isEnabled("perspective");
        } else {
            flag = false;
        }

        if (this.perspective != null && flag) {
            if (perspective2 != this.perspective) {
                this.startDistance = this.distanceFactor;
                this.startYaw = this.yawOffset;
                this.perspective = perspective2;
                this.targetDistance = this.getDistanceFor(perspective2);
                this.targetYaw = this.getYawFor(perspective2);
                this.transitionAnimation.forceValue(0.0);
                boolean flag2 = this.startYaw != this.targetYaw;
                boolean flag3 = perspective2.isFirstPerson();
                double d;
                if (this.isInCombat()) {
                    d = flag2 ? 0.11 : (flag3 ? 0.07 : 0.09);
                } else {
                    d = flag2 ? 0.32 : (flag3 ? 0.17 : 0.24);
                }

                this.transitionAnimation.animate(1.0, d, Easings.SINE_IN_OUT);
            }

            this.transitionAnimation.update();
            float f = this.transitionAnimation.get();
            if (this.startYaw != this.targetYaw && this.startDistance != this.targetDistance) {
                float f2 = (float)Math.min(this.transitionAnimation.getProgress(), 1.0);
                boolean flag4 = this.targetDistance > this.startDistance;
                float f3 = (float)Easings.SINE_IN_OUT.ease(Math.min(f2 / 0.65F, 1.0F));
                float f4 = (float)Easings.SINE_IN_OUT.ease(Math.max((f2 - 0.35F) / 0.65F, 0.0F));
                this.yawOffset = this.startYaw + (this.targetYaw - this.startYaw) * (flag4 ? f4 : f3);
                this.distanceFactor = this.startDistance + (this.targetDistance - this.startDistance) * (flag4 ? f3 : f4);
            } else {
                this.distanceFactor = this.startDistance + (this.targetDistance - this.startDistance) * f;
                this.yawOffset = this.startYaw + (this.targetYaw - this.startYaw) * f;
            }

            if (!this.transitionAnimation.isFinished()) {
                this.animating = true;
                return this.animating;
            } else {
                this.animating = false;
                return this.animating;
            }
        } else {
            this.snapTo(perspective2);
            return false;
        }
    }

    public float getDistanceFactor() {
        return this.distanceFactor;
    }

    public float getYawOffset() {
        return this.yawOffset;
    }

    public boolean isAnimating() {
        return this.animating;
    }

    public boolean isThirdPerson() {
        return this.yawOffset != 0.0F || !(this.distanceFactor * this.maxDistance <= 0.8F);
    }

    public void setMaxDistance(float f) {
        this.maxDistance = f;
    }

    public void setCameraRotation(float f, float f2) {
        this.cameraYaw = f;
        this.cameraPitch = f2;
        this.rotationCaptured = true;
    }

    public boolean hasCameraRotation() {
        return this.rotationCaptured;
    }

    public float getCameraYaw() {
        return this.cameraYaw;
    }

    public float getCameraPitch() {
        return this.cameraPitch;
    }

    private void trackHurt() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.player == null) {
            this.lastHurtTicks = 0;
        } else {
            int i = minecraftClient.player.hurtTime;
            if (i > this.lastHurtTicks) {
                this.lastHurtTime = System.currentTimeMillis();
            }

            this.lastHurtTicks = i;
        }
    }

    private boolean isInCombat() {
        AttackAura attackAura = AttackAura.getInstance();
        return attackAura != null && attackAura.isEnabled() && attackAura.getCurrentTarget() != null
            ? true
            : this.lastHurtTime != 0L && System.currentTimeMillis() - this.lastHurtTime < 4000L;
    }

    private void snapTo(Perspective perspective2) {
        this.perspective = perspective2;
        this.distanceFactor = this.getDistanceFor(perspective2);
        this.yawOffset = this.getYawFor(perspective2);
        this.startDistance = this.distanceFactor;
        this.startYaw = this.yawOffset;
        this.targetDistance = this.distanceFactor;
        this.targetYaw = this.yawOffset;
        this.transitionAnimation.forceValue(1.0);
        this.animating = false;
    }

    private float getDistanceFor(Perspective perspective2) {
        return perspective2.isFirstPerson() ? 0.0F : 1.0F;
    }

    private float getYawFor(Perspective perspective2) {
        return perspective2.isFrontView() ? 180.0F : 0.0F;
    }
}
