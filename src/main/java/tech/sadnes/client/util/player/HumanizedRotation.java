package tech.sadnes.client.util.player;

import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.rotation.SensitivityUtil;

public final class HumanizedRotation {
    private final Random random = new Random();
    private float yawVelocity;
    private float pitchVelocity;
    private float lastTargetYaw;
    private float lastTargetPitch;
    private boolean hasLastTarget;
    private float gain = 0.9F;
    private float saccadeYaw;
    private float saccadePitch;
    private float saccadeDuration;
    private float saccadeElapsed;
    private boolean saccadeActive;
    private boolean forcedSaccade;
    private float saccadeCooldown;
    private float mouseDeltaX;
    private float mouseDeltaY;
    private float avgMouseSpeedX;
    private float avgMouseSpeedY;
    private float yawRemainder;
    private float pitchRemainder;
    private float noiseYaw;
    private float noisePitch;
    private float pauseTimer;
    private float hesitationTimer;
    private float hesitationFactor;
    private float speedFactor = 1.0F;
    private float aimOffsetX = 0.5F;
    private float aimOffsetY = 0.7F;
    private float aimOffsetZ = 0.5F;
    private float targetOffsetX = 0.5F;
    private float targetOffsetY = 0.7F;
    private float targetOffsetZ = 0.5F;
    private float offsetChangeTimer;

    public void addMouseDelta(float f, float f2) {
        this.mouseDeltaX += f;
        this.mouseDeltaY += f2;
    }

    public void resetVelocity() {
        this.hasLastTarget = false;
        this.yawVelocity = 0.0F;
        this.pitchVelocity = 0.0F;
        this.offsetChangeTimer = 0.0F;
    }

    public void reset() {
        this.resetVelocity();
        this.saccadeActive = false;
        this.forcedSaccade = false;
        this.saccadeCooldown = 0.0F;
        this.mouseDeltaX = this.mouseDeltaY = 0.0F;
        this.avgMouseSpeedX = this.avgMouseSpeedY = 0.0F;
        this.yawRemainder = this.pitchRemainder = 0.0F;
        this.noiseYaw = this.noisePitch = 0.0F;
        this.pauseTimer = 0.0F;
        this.hesitationTimer = 0.0F;
        this.speedFactor = 1.0F;
    }

    public void triggerFlick() {
        float f = Math.signum(this.yawVelocity) * (this.random.nextFloat() < 0.65F ? -1.0F : 1.0F);
        if (f == 0.0F) {
            f = this.random.nextBoolean() ? 1.0F : -1.0F;
        }

        this.saccadeYaw = f * MathUtil.randomBetween(1.2F, 3.8F);
        this.saccadePitch = this.random.nextFloat() < 0.7F ? MathUtil.randomBetween(0.4F, 2.4F) : -MathUtil.randomBetween(0.3F, 1.2F);
        this.saccadeDuration = MathUtil.randomBetween(0.06F, 0.11F);
        this.saccadeElapsed = 0.0F;
        this.saccadeActive = true;
        this.forcedSaccade = true;
        this.hesitationTimer = MathUtil.randomBetween(0.12F, 0.26F);
        this.hesitationFactor = MathUtil.randomBetween(0.03F, 0.18F);
        this.offsetChangeTimer = 0.0F;
    }

    public boolean isSaccadeActive() {
        return this.saccadeActive;
    }

    public Vec3d getAimPoint(Entity entity, float f, float f2) {
        this.offsetChangeTimer -= f2;
        if (this.offsetChangeTimer <= 0.0F) {
            this.targetOffsetX = MathHelper.clamp(0.5F + (float)this.random.nextGaussian() * 0.18F, 0.2F, 0.8F);
            this.targetOffsetZ = MathHelper.clamp(0.5F + (float)this.random.nextGaussian() * 0.18F, 0.2F, 0.8F);
            this.targetOffsetY = MathUtil.randomBetween(0.65F, 0.8F);
            this.offsetChangeTimer = MathUtil.randomBetween(0.8F, 2.0F);
        }

        float f3 = 1.0F - (float)Math.exp(-f2 / 0.6F);
        this.aimOffsetX = this.aimOffsetX + (this.targetOffsetX - this.aimOffsetX) * f3;
        this.aimOffsetY = this.aimOffsetY + (this.targetOffsetY - this.aimOffsetY) * f3;
        this.aimOffsetZ = this.aimOffsetZ + (this.targetOffsetZ - this.aimOffsetZ) * f3;
        Vec3d vec3d = entity.getLerpedPos(f);
        Box box = entity.getBoundingBox();
        return new Vec3d(
            vec3d.x + (this.aimOffsetX - 0.5F) * box.getLengthX(),
            vec3d.y + this.aimOffsetY * box.getLengthY(),
            vec3d.z + (this.aimOffsetZ - 0.5F) * box.getLengthZ()
        );
    }

    public float[] computeRotationDelta(Rotation rotation, Rotation rotation2, float f, float f2, float f3) {
        if (this.hasLastTarget && f3 > 0.0F) {
            float f4 = 1.0F - (float)Math.exp(-f3 / 0.1F);
            float f5 = 1.0F - (float)Math.exp(-f3 / 0.25F);
            float f6 = MathHelper.clamp(MathHelper.wrapDegrees(rotation2.getYaw() - this.lastTargetYaw) / f3, -360.0F, 360.0F);
            float f7 = MathHelper.clamp((rotation2.getPitch() - this.lastTargetPitch) / f3, -90.0F, 90.0F);
            this.yawVelocity = this.yawVelocity + (f6 - this.yawVelocity) * f4;
            this.pitchVelocity = this.pitchVelocity + (f7 - this.pitchVelocity) * f5;
        }

        this.lastTargetYaw = rotation2.getYaw();
        this.lastTargetPitch = rotation2.getPitch();
        this.hasLastTarget = true;
        float f8 = MathHelper.wrapDegrees(rotation2.getYaw() - rotation.getYaw());
        float f9 = rotation2.getPitch() - rotation.getPitch();
        float f10 = (float)Math.exp(-f3 / 0.04F);
        boolean flag = this.yawVelocity * f8 < 0.0F && Math.abs(f8) > f;
        boolean flag2 = this.pitchVelocity * f9 < 0.0F && Math.abs(f9) > f2;
        if (flag) {
            this.yawVelocity *= f10;
        }

        if (flag2) {
            this.pitchVelocity *= f10;
        }

        if (f3 > 0.0F) {
            float f11 = (float)Math.exp(-f3 / 0.3F);
            float f12 = (float)Math.sqrt(1.0F - f11 * f11);
            this.noiseYaw = this.noiseYaw * f11 + (float)this.random.nextGaussian() * 4.0F * f12;
            this.noisePitch = this.noisePitch * f11 + (float)this.random.nextGaussian() * 1.8F * f12;
        }

        this.pauseTimer -= f3;
        this.hesitationTimer -= f3;
        boolean flag3 = this.pauseTimer <= 0.0F && this.random.nextFloat() < 0.25F * f3;
        if (flag3) {
            this.pauseTimer = MathUtil.randomBetween(0.15F, 0.4F);
        }

        float f13 = this.hesitationTimer > 0.0F ? this.hesitationFactor : (this.pauseTimer > 0.0F ? 0.3F : 1.0F);
        float f14 = 1.0F - (float)Math.exp(-f3 / (f13 < this.speedFactor ? 0.04F : 0.09F));
        this.speedFactor = this.speedFactor + (f13 - this.speedFactor) * f14;
        float f15 = this.gain * this.speedFactor;
        float f16 = (this.yawVelocity * f15 + this.noiseYaw) * f3;
        float f17 = (this.pitchVelocity * f15 * 0.6F + this.noisePitch) * f3;
        if (this.saccadeActive && !this.forcedSaccade) {
            float f18 = 1.0F - minJerk(this.saccadeElapsed / this.saccadeDuration);
            boolean flag4 = (this.saccadeYaw * f8 + this.saccadePitch * f9) * f18 < 0.0F;
            if (flag4) {
                this.saccadeActive = false;
            }
        }

        this.saccadeCooldown -= f3;
        double d = Math.hypot(f8 / f, f9 / f2);
        boolean flag5 = !this.saccadeActive && this.saccadeCooldown <= 0.0F && this.hesitationTimer <= 0.0F && d > 1.0;
        if (flag5) {
            this.startSaccade(f8, f9, f);
        }

        if (this.saccadeActive) {
            float f19 = minJerk(this.saccadeElapsed / this.saccadeDuration);
            this.saccadeElapsed = Math.min(this.saccadeElapsed + f3, this.saccadeDuration);
            float f20 = (minJerk(this.saccadeElapsed / this.saccadeDuration) - f19) * (1.0F + (float)this.random.nextGaussian() * 0.06F);
            f16 += this.saccadeYaw * f20;
            f17 += this.saccadePitch * f20;
            if (this.saccadeElapsed >= this.saccadeDuration) {
                this.saccadeActive = false;
                this.forcedSaccade = false;
                this.saccadeCooldown = MathUtil.randomBetween(0.04F, 0.12F);
                this.gain = MathUtil.randomBetween(0.8F, 0.97F);
            }
        }

        if (f3 > 0.0F) {
            float f21 = 1.0F - (float)Math.exp(-f3 / 0.06F);
            this.avgMouseSpeedX = this.avgMouseSpeedX + (this.mouseDeltaX / f3 - this.avgMouseSpeedX) * f21;
            this.avgMouseSpeedY = this.avgMouseSpeedY + (this.mouseDeltaY / f3 - this.avgMouseSpeedY) * f21;
            if (!(Math.hypot(this.mouseDeltaX, this.mouseDeltaY) > 2.0)) {
                f16 += MathHelper.clamp((this.mouseDeltaX - this.avgMouseSpeedX * f3) * 0.4F, -0.6F, 0.6F);
                f17 += MathHelper.clamp((this.mouseDeltaY - this.avgMouseSpeedY * f3) * 0.25F, -0.3F, 0.3F);
            }
        }

        this.mouseDeltaX = this.mouseDeltaY = 0.0F;
        return this.applyGcd(f16, f17);
    }

    private void startSaccade(float f, float f2, float f3) {
        float f4 = (float)Math.hypot(f, f2);
        float f5 = Math.max(f3 * 2.0F, 1.0F);
        this.saccadeDuration = (0.08F + 0.055F * (float)(Math.log(1.0F + f4 / f5) / Math.log(2.0))) * MathUtil.randomBetween(0.9F, 1.15F);
        boolean flag = this.random.nextFloat() < 0.25F;
        float f6 = f4 > 20.0F ? MathUtil.randomBetween(0.88F, 0.97F) : (flag ? MathUtil.randomBetween(1.04F, 1.15F) : MathUtil.randomBetween(0.95F, 1.03F));
        float f7 = f6 + (float)this.random.nextGaussian() * 0.04F;
        float f8 = (float)this.random.nextGaussian() * 0.025F;
        float f9 = f / f4;
        float f10 = f2 / f4;
        this.saccadeYaw = (f9 * f7 - f10 * f8) * f4;
        this.saccadePitch = (f10 * f7 + f9 * f8) * f4;
        this.saccadeElapsed = 0.0F;
        this.saccadeActive = true;
        this.forcedSaccade = false;
    }

    private static float minJerk(float f) {
        float f2 = (float)Math.pow(MathHelper.clamp(f, 0.0F, 1.0F), 0.8);
        return f2 * f2 * f2 * (10.0F + f2 * (-15.0F + 6.0F * f2));
    }

    private float[] applyGcd(float f, float f2) {
        float f3 = SensitivityUtil.getGcd();
        if (f3 == 0.0F) {
            return new float[]{f, f2};
        }

        this.yawRemainder += f;
        this.pitchRemainder += f2;
        float f4 = Math.round(this.yawRemainder / f3) * f3;
        float f5 = Math.round(this.pitchRemainder / f3) * f3;
        this.yawRemainder -= f4;
        this.pitchRemainder -= f5;
        return new float[]{f4, f5};
    }
}
