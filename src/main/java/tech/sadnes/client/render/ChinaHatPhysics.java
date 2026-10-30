package tech.sadnes.client.render;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.animation.Easings;

public final class ChinaHatPhysics {
    private static final float FOUR_PI = (float) (Math.PI * 4);
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float TILT_STIFFNESS = 115.0F;
    private static final float TILT_DAMPING = 11.5F;
    private static final float TILT_VELOCITY_SCALE = 2.6F;
    private static final float MAX_TILT = 20.0F;
    private static final float TILT_LIFT = 0.28F;
    private static final float BOB_STIFFNESS = 240.0F;
    private static final float BOB_DAMPING = 17.0F;
    private static final float BOB_VELOCITY_SCALE = 0.009F;
    private static final float MAX_BOB = 0.07F;
    private static final float SUBSTEP = 0.008F;
    private static final float MAX_DELTA = 0.05F;
    private static final float RESET_DELTA = 0.25F;
    private static final float APPEAR_DURATION = 0.35F;
    private static final float MAX_SPEED = 6.0F;
    private float tiltX;
    private float tiltZ;
    private float tiltVelocityX;
    private float tiltVelocityZ;
    private float pitch;
    private float roll;
    private float bob;
    private float bobVelocity;
    private float lift;
    private float wavePhase;
    private float waveAmplitude;
    private float speedFactor;
    private float appearProgress;
    private long lastUpdateNanos;

    public void update(LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        long l = System.nanoTime();
        float f5 = this.lastUpdateNanos == 0L ? 0.0F : (float)(l - this.lastUpdateNanos) / 1.0E9F;
        this.lastUpdateNanos = l;
        if (!(f5 < 5.0E-4F)) {
            float f6 = (float)((livingEntity.getX() - livingEntity.lastX) * 20.0);
            float f7 = (float)((livingEntity.getY() - livingEntity.lastY) * 20.0);
            float f8 = (float)((livingEntity.getZ() - livingEntity.lastZ) * 20.0);
            this.speedFactor = Math.min(1.0F, (float)Math.sqrt(f6 * f6 + f8 * f8) / 6.0F);
            float f9 = MathHelper.clamp(-f6 * 2.6F * f2, -20.0F, 20.0F);
            float f10 = MathHelper.clamp(-f8 * 2.6F * f2, -20.0F, 20.0F);
            float f11 = MathHelper.clamp(-f7 * 0.009F * f2, -0.07F, 0.07F);
            float f12 = Math.min(f5, 0.05F);
            boolean flag = f5 > 0.25F;
            if (flag) {
                this.tiltX = f9;
                this.tiltZ = f10;
                this.bob = f11;
                this.tiltVelocityX = this.tiltVelocityZ = this.bobVelocity = 0.0F;
            } else {
                int i = Math.max(1, Math.round(f12 / 0.008F));
                float f13 = f12 / i;

                for (int i2 = 0; i2 < i; i2++) {
                    this.tiltVelocityX = this.tiltVelocityX + ((f9 - this.tiltX) * 115.0F - this.tiltVelocityX * 11.5F) * f13;
                    this.tiltVelocityZ = this.tiltVelocityZ + ((f10 - this.tiltZ) * 115.0F - this.tiltVelocityZ * 11.5F) * f13;
                    this.bobVelocity = this.bobVelocity + ((f11 - this.bob) * 240.0F - this.bobVelocity * 17.0F) * f13;
                    this.tiltX = this.tiltX + this.tiltVelocityX * f13;
                    this.tiltZ = this.tiltZ + this.tiltVelocityZ * f13;
                    this.bob = this.bob + this.bobVelocity * f13;
                }
            }

            float f14 = Math.min(20.0F, (float)Math.sqrt(this.tiltX * this.tiltX + this.tiltZ * this.tiltZ));
            this.lift = 0.28F * MathHelper.sin(f14 * (float) (Math.PI / 180.0));
            this.wavePhase = this.wavePhase + f12 * (2.4F + this.speedFactor * 3.2F) * f4;
            if (this.wavePhase >= (float) (Math.PI * 4)) {
                this.wavePhase %= (float) (Math.PI * 4);
            }

            this.waveAmplitude = (0.01F + this.speedFactor * 0.02F) * f3;
            this.appearProgress = Math.min(1.0F, this.appearProgress + f12 / 0.35F);
            float f15 = f * (float) (Math.PI / 180.0);
            float f16 = -MathHelper.sin(f15);
            float f17 = -MathHelper.cos(f15);
            this.pitch = this.tiltX * f16 - this.tiltZ * f17;
            this.roll = this.tiltX * f17 + this.tiltZ * f16;
        }
    }

    public float getScale() {
        return (float)Easings.BACK_OUT.ease(this.appearProgress);
    }

    public float getAlpha() {
        return Math.min(1.0F, this.appearProgress * 1.6F);
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getRoll() {
        return this.roll;
    }

    public float getYOffset() {
        return this.bob + this.lift;
    }

    public float getWavePhase() {
        return this.wavePhase;
    }

    public float getWaveAmplitude() {
        return this.waveAmplitude;
    }
}
