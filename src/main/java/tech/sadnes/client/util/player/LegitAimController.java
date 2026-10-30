package tech.sadnes.client.util.player;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.feature.component.FreeLookComponent;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.client.util.rotation.SensitivityUtil;

public final class LegitAimController {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private final Random random = new Random();
    private float speedGain;
    private float smoothing;
    private float flickStrength;
    private float flickThreshold;
    private float jitter;
    private int minFlickDelay;
    private int maxFlickDelay;
    private int profileTicks;
    private double lateralOffset;
    private double lateralTarget;
    private double verticalOffset = -0.2;
    private double verticalVelocity;
    private int offsetTicks;
    private int lateralSide = 1;
    private int sideTicks;
    private float yawVelocity;
    private float pitchVelocity;
    private float flickYaw;
    private float flickPitch;
    private int flickSteps;
    private int flickStep;
    private int flickDelay;
    private float yawOffset;
    private float pitchOffset;
    private float wobble;
    private float wobbleFactor;
    private float targetYaw;
    private float targetPitch;
    private boolean initialized;
    private int pauseTicks;
    private int pauseCooldown;
    private float cooldownThreshold = 0.66F;
    private boolean attackReady;
    private float lastYaw;
    private float lastPitch;
    private boolean hasLast;
    private boolean hadTarget;
    private int lastTargetId = Integer.MIN_VALUE;
    private int holdTicks;
    private int switchTicks;

    public LegitAimController() {
        this.randomizeProfile();
        this.randomizeOffset();
    }

    public void reset() {
        this.yawVelocity = 0.0F;
        this.pitchVelocity = 0.0F;
        this.flickYaw = 0.0F;
        this.flickPitch = 0.0F;
        this.flickSteps = 0;
        this.flickStep = 0;
        this.flickDelay = 0;
        this.pauseTicks = 0;
        this.pauseCooldown = 0;
        this.yawOffset = 0.0F;
        this.pitchOffset = 0.0F;
        this.wobble = 0.0F;
        this.initialized = false;
        this.hasLast = false;
        this.hadTarget = false;
        this.lastTargetId = Integer.MIN_VALUE;
        this.holdTicks = 0;
        this.switchTicks = 0;
        this.randomizeProfile();
        this.randomizeOffset();
    }

    public float[] computeRotationDelta(LivingEntity livingEntity, float f) {
        if (MC.player == null) {
            return new float[]{0.0F, 0.0F};
        }

        if (--this.profileTicks <= 0) {
            this.randomizeProfile();
        }

        boolean flag;
        if (livingEntity != null) {
            flag = livingEntity.isAlive();
        } else {
            flag = false;
        }

        if (flag != this.hadTarget) {
            this.hasLast = false;
            this.initialized = false;
            this.flickSteps = 0;
            this.flickStep = 0;
            this.flickDelay = 0;
            this.hadTarget = flag;
        }

        float f2 = 1.0F;
        float f3;
        float f4;
        if (flag) {
            int i = livingEntity.getId();
            boolean flag2 = i != this.lastTargetId;
            this.lastTargetId = i;
            if (flag2 && this.initialized) {
                this.holdTicks = 3 + this.random.nextInt(3);
                this.switchTicks = 9;
            }

            if (this.holdTicks > 0 && this.initialized) {
                this.holdTicks--;
                f3 = this.targetYaw;
                f4 = this.targetPitch;
            } else {
                Box box = livingEntity.getBoundingBox();
                float[] floats = RotationUtil.getRotationArrayTo(this.computeAimPoint(livingEntity, f));
                float[] floats2 = RotationUtil.getRotationArrayTo(this.getCenterPoint(livingEntity));
                this.yawOffset = this.yawOffset + MathHelper.clamp(MathHelper.wrapDegrees(floats[0] - floats2[0]) - this.yawOffset, -2.5F, 2.5F);
                this.pitchOffset = this.pitchOffset + MathHelper.clamp(floats[1] - floats2[1] - this.pitchOffset, -3.0F, 3.0F);
                float f5 = floats2[0] + this.yawOffset;
                float f6 = floats2[1] + this.pitchOffset;
                double d = this.getHorizontalDistance(livingEntity);
                f2 = (float)MathHelper.clamp(d / 1.1, 0.12, 1.0);
                float f7 = (float)MathHelper.clamp((AttackTimer.getCooldownProgress() - 0.4) / 0.5, 0.0, 1.0);
                float f8 = (float)MathHelper.clamp(d * 0.85 + 0.18, 0.18, 1.0);
                double d2 = (box.maxY - box.minY) / 2.0;
                float f9 = (float)MathHelper.clamp(Math.toDegrees(Math.atan(d2 / Math.max(d, 0.1))) / 14.0, 0.25, 1.0);
                float f10 = (float)MathHelper.clamp(d * 0.2 + 0.08, 0.08, 0.3);
                float f11 = (float)MathHelper.clamp(d * 0.45 + 0.14, 0.14, 0.7);
                float f12 = f11 - (f11 - f10) * (1.0F - f7) * f9;
                float f13 = (float)MathHelper.clamp(d * d * 18.0 + 7.0, 7.0, 300.0);
                if (!this.initialized) {
                    this.targetYaw = f5;
                    this.targetPitch = f6;
                    this.initialized = true;
                }

                float f14 = this.switchTicks > 0 ? Math.min(f13, 13.0F) : f13;
                if (this.switchTicks > 0) {
                    this.switchTicks--;
                }

                float f15 = MathHelper.wrapDegrees(f5 - this.targetYaw) * f8;
                this.targetYaw = this.targetYaw + MathHelper.clamp(f15, -f14, f14);
                float f16 = (f6 - this.targetPitch) * f12;
                this.targetPitch = this.targetPitch + MathHelper.clamp(f16, -f14 * 0.5F, f14 * 0.5F);
                this.wobble = MathHelper.clamp(this.wobble + (float)(this.random.nextGaussian() * 7.0 - this.wobble * 0.26), -22.0F, 22.0F);
                this.wobbleFactor = 1.0F - f7;
                double d3 = (box.maxX - box.minX) / 2.0 + 0.1;
                float f17 = (float)Math.toDegrees(Math.atan(d3 / Math.max(d, 0.1)));
                float f18 = MathHelper.clamp(f17 / 7.0F, 0.45F, 1.4F) * (float)MathHelper.clamp(d / 0.6, 0.4, 1.0);
                f3 = this.targetYaw + this.wobble * this.wobbleFactor * f18;
                f4 = this.targetPitch;
            }
        } else {
            this.wobbleFactor = 0.0F;
            this.holdTicks = 0;
            this.switchTicks = 0;
            if (!this.initialized) {
                this.targetYaw = MC.player.getYaw();
                this.targetPitch = MC.player.getPitch();
                this.initialized = true;
            }

            this.targetYaw = this.targetYaw + MathHelper.wrapDegrees(FreeLookComponent.getYaw() - this.targetYaw) * 0.14F;
            this.targetPitch = this.targetPitch + (FreeLookComponent.getPitch() - this.targetPitch) * 0.14F;
            f3 = this.targetYaw;
            f4 = this.targetPitch;
        }

        this.attackReady = AttackTimer.getCooldownProgress() >= this.cooldownThreshold;
        if (!flag || this.attackReady) {
            this.pauseTicks = 0;
        } else if (this.pauseTicks > 0) {
            this.pauseTicks--;
        } else if (--this.pauseCooldown <= 0) {
            this.pauseTicks = 2 + this.random.nextInt(7);
            this.pauseCooldown = 2 + this.random.nextInt(4);
            this.cooldownThreshold = 0.6F + this.random.nextFloat() * 0.12F;
        }

        float f19 = this.hasLast ? MathHelper.wrapDegrees(f3 - this.lastYaw) : 0.0F;
        float f20 = this.hasLast ? f4 - this.lastPitch : 0.0F;
        this.lastYaw = f3;
        this.lastPitch = f4;
        this.hasLast = true;
        float f21 = Math.max(f2, 1.0F - this.wobbleFactor);
        float f22 = this.pauseTicks > 0 ? 0.25F : 1.0F;
        this.yawVelocity = this.yawVelocity + (f19 * this.speedGain * f22 * f21 - this.yawVelocity) * (1.0F - this.smoothing);
        this.pitchVelocity = this.pitchVelocity + (f20 * this.speedGain * f22 * f21 - this.pitchVelocity) * (1.0F - this.smoothing);
        float f23 = MathHelper.wrapDegrees(f3 - MC.player.getYaw());
        float f24 = f4 - MC.player.getPitch();
        if (this.attackReady && this.flickSteps == 0) {
            this.flickDelay = 0;
        }

        if (this.pauseTicks == 0 && this.flickSteps == 0 && --this.flickDelay <= 0) {
            this.startFlick(livingEntity, f, f23, f24, f21);
        }

        float f25 = this.yawVelocity;
        float f26 = this.pitchVelocity;
        if (this.flickSteps > 0) {
            float f27 = stepProgress(this.flickStep, this.flickSteps);
            f25 += this.flickYaw * f27;
            f26 += this.flickPitch * f27;
            if (++this.flickStep >= this.flickSteps) {
                this.flickSteps = 0;
                this.flickStep = 0;
                this.flickDelay = this.nextFlickDelay();
            }
        }

        float f28 = f25 + (float)this.random.nextGaussian() * this.jitter;
        float f29 = f26 + (float)this.random.nextGaussian() * this.jitter * 0.65F;
        float f30 = SensitivityUtil.getGcd();
        float f31 = snapToGcd(f28, f30);
        float f32 = snapToGcd(f29, f30);
        if (flag) {
            float[] floats3 = this.getEdgeAngles(livingEntity);
            float f33 = MC.player.getYaw() + f31;
            if (Math.abs(MathHelper.wrapDegrees(f33 - floats3[0])) < 0.12F || Math.abs(MathHelper.wrapDegrees(f33 - floats3[2])) < 0.12F) {
                f31 += f30 * 3.0F;
            }

            if (Math.abs(MC.player.getPitch() + f32 - floats3[1]) < 0.12F) {
                f32 += f30 * 3.0F;
            }
        }

        return new float[]{f31, f32};
    }

    private void startFlick(LivingEntity livingEntity, float f, float f2, float f3, float f4) {
        float f5 = Math.max(Math.abs(f2), Math.abs(f3));
        if (f5 < this.flickThreshold) {
            this.flickDelay = this.nextFlickDelay();
        } else {
            float f6 = this.attackReady ? 1.45F : 1.0F;
            float f7 = this.flickStrength * f6 * (0.55F + 0.35F * f4 + (float)this.random.nextGaussian() * 0.05F);
            if (f5 > 30.0F && !this.attackReady && this.switchTicks <= 0) {
                f7 *= 1.05F + (float)Math.min(0.12, (f5 - 30.0F) / 400.0);
            }

            this.flickYaw = f2 * f7;
            this.flickPitch = f3 * f7;
            this.flickSteps = MathHelper.clamp((int)Math.round(1.4 + Math.log10(1.0F + f5) * 2.4 + this.random.nextGaussian() * 0.7), 1, 6);
            this.flickStep = 0;
        }
    }

    private Vec3d computeAimPoint(LivingEntity livingEntity, float f) {
        Box box = livingEntity.getBoundingBox();
        double d = CombatUtil.getDistance(livingEntity);
        Vec3d vec3d = MC.player.getEyePos();
        if (--this.offsetTicks <= 0) {
            this.randomizeOffset();
        }

        this.verticalVelocity = this.verticalVelocity + (this.random.nextGaussian() * 0.22 - this.verticalVelocity * 0.12);
        this.verticalOffset = MathHelper.clamp(this.verticalOffset + this.verticalVelocity, -0.5, 0.2);
        double d2 = (box.maxX - box.minX) / 2.0;
        double d3 = MathHelper.clamp(d / 2.0, 0.22, 1.0);
        this.lateralOffset = this.lateralOffset + (this.lateralTarget * d2 - this.lateralOffset) * d3;
        double d4 = (box.minX + box.maxX) / 2.0;
        double d5 = (box.minZ + box.maxZ) / 2.0;
        double d6 = Math.max(0.05, Math.hypot(d4 - vec3d.x, d5 - vec3d.z));
        double d7 = Math.tan(Math.toRadians(18.0)) * d6;
        double d8 = Math.tan(Math.toRadians(19.0)) * d6;
        double d9 = (box.maxX - box.minX) * 0.4;
        double d10 = (box.maxZ - box.minZ) * 0.4;
        double d11 = (box.maxY - box.minY) * 0.1;
        double d12 = this.random.nextGaussian() * d7 * 0.2;
        double d13 = MathHelper.clamp(this.lateralOffset + d12, -d7, d7);
        double d14 = (d4 - vec3d.x) / d6;
        double d15 = (d5 - vec3d.z) / d6;
        Vec3d vec3d2 = new Vec3d(
            d4 + MathHelper.clamp(-d15 * d13, -d9, d9),
            MathHelper.clamp(vec3d.y + MathHelper.clamp(this.verticalOffset, -d8, d8), box.minY + d11, box.maxY - d11),
            d5 + MathHelper.clamp(d14 * d13, -d10, d10)
        );
        double d16 = MathHelper.clamp((d - (f - 0.75F)) / 0.75, 0.0, 0.35);
        if (d16 > 0.0) {
            Vec3d vec3d3 = CombatUtil.clampToBox(vec3d, box);
            vec3d2 = new Vec3d(MathHelper.lerp(d16, vec3d2.x, vec3d3.x), MathHelper.lerp(d16, vec3d2.y, vec3d3.y), MathHelper.lerp(d16, vec3d2.z, vec3d3.z));
        }

        if (AimPointUtil.canSee(MC.player, vec3d2)) {
            return vec3d2;
        }

        this.lateralOffset *= 0.6;
        this.verticalOffset *= 0.6;
        this.verticalVelocity *= 0.5;
        return this.getCenterPoint(livingEntity);
    }

    private double getHorizontalDistance(LivingEntity livingEntity) {
        Box box = livingEntity.getBoundingBox();
        Vec3d vec3d = MC.player.getEyePos();
        double d = Math.max(0.0, Math.max(box.minX - vec3d.x, vec3d.x - box.maxX));
        double d2 = Math.max(0.0, Math.max(box.minZ - vec3d.z, vec3d.z - box.maxZ));
        return Math.hypot(d, d2);
    }

    private Vec3d getCenterPoint(LivingEntity livingEntity) {
        Box box = livingEntity.getBoundingBox();
        double d = (box.maxY - box.minY) * 0.1;
        return new Vec3d(
            (box.minX + box.maxX) / 2.0, MathHelper.clamp(MC.player.getEyeY(), box.minY + d, box.maxY - d), (box.minZ + box.maxZ) / 2.0
        );
    }

    private float[] getEdgeAngles(LivingEntity livingEntity) {
        Box box = livingEntity.getBoundingBox();
        Vec3d vec3d = MC.player.getEyePos();
        double d = Math.hypot(livingEntity.getX() - vec3d.x, livingEntity.getZ() - vec3d.z);
        float f = (float)(Math.toDegrees(Math.atan2(livingEntity.getZ() - vec3d.z, livingEntity.getX() - vec3d.x)) - 90.0);
        float f2 = (float)(-Math.toDegrees(Math.atan2(livingEntity.getY() - MC.player.getY(), d)));
        double d2 = MathHelper.clamp(vec3d.x, box.minX, box.maxX);
        double d3 = MathHelper.clamp(vec3d.z, box.minZ, box.maxZ);
        float f3 = (float)(Math.toDegrees(Math.atan2(d3 - vec3d.z, d2 - vec3d.x)) - 90.0);
        return new float[]{f, f2, f3};
    }

    private void randomizeProfile() {
        this.speedGain = 0.72F + this.random.nextFloat() * 0.23F;
        this.smoothing = 0.48F + this.random.nextFloat() * 0.26F;
        this.flickStrength = 0.85F + this.random.nextFloat() * 0.3F;
        this.flickThreshold = 0.25F + this.random.nextFloat() * 0.4F;
        this.jitter = 0.05F + this.random.nextFloat() * 0.1F;
        this.minFlickDelay = 1 + this.random.nextInt(3);
        this.maxFlickDelay = this.minFlickDelay + 2 + this.random.nextInt(5);
        this.profileTicks = 45 + this.random.nextInt(80);
    }

    private void randomizeOffset() {
        if (--this.sideTicks <= 0) {
            this.lateralSide = this.randomSign();
            this.sideTicks = 5 + this.random.nextInt(12);
        }

        this.lateralTarget = this.lateralSide * (0.44 + this.random.nextDouble() * 0.56);
        this.offsetTicks = 2 + this.random.nextInt(6);
    }

    private int nextFlickDelay() {
        return this.minFlickDelay + this.random.nextInt(this.maxFlickDelay - this.minFlickDelay + 1);
    }

    private int randomSign() {
        return this.random.nextBoolean() ? 1 : -1;
    }

    private static float stepProgress(int i, int i2) {
        return smootherStep((float)(i + 1) / i2) - smootherStep((float)i / i2);
    }

    private static float smootherStep(float f) {
        return f * f * f * (10.0F - 15.0F * f + 6.0F * f * f);
    }

    private static float snapToGcd(float f, float f2) {
        return f2 <= 0.0F ? f : Math.round(f / f2) * f2;
    }
}
