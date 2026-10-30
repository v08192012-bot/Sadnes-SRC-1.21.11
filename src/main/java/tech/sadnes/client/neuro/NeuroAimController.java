package tech.sadnes.client.neuro;

import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.rotation.SensitivityUtil;

public final class NeuroAimController {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final NeuroDebug debug;
    private static final float SIGMA_SCALE = 1.3F;
    private static final float MAX_SIGMA_YAW = 0.3F;
    private static final float NOISE_DECAY = 0.2F;
    private static final float OUTPUT_SCALE = 10.0F;
    private static final float MAX_YAW_ACCEL = 50.0F;
    private static final float MAX_PITCH_ACCEL = 18.0F;
    private static final float MAX_YAW_STEP = 58.0F;
    private static final float MAX_PITCH_STEP = 22.0F;
    private static final float HITBOX_MARGIN = 3.0F;
    private static final float OVERSHOOT_FACTOR = 1.1F;
    private static final float CATCHUP_START_ANGLE = 16.0F;
    private static final float CATCHUP_FULL_ANGLE = 45.0F;
    private static final float PITCH_SCALE = 0.5F;
    private static final float MIN_PITCH_DEADZONE = 4.0F;
    private static final float PITCH_CATCHUP_ANGLE = 30.0F;
    private static final float MAX_SIGMA_PITCH = 0.18F;
    private static final float PAUSE_CHANCE = 0.12F;
    private static final float PAUSE_CHANCE_STILL = 0.3F;
    private static final float REVERSE_YAW_THRESHOLD = 10.0F;
    private static final float REVERSE_PITCH_THRESHOLD = 6.0F;
    private static final float REVERSE_CLAMP = 1.8F;
    private final float[][] history = new float[5][];
    private int historyCount;
    private float lastDeltaYaw;
    private float lastDeltaPitch;
    private float targetYaw;
    private float targetPitch;
    private boolean needsUpdate;
    private boolean hasRotation;
    private Vec3d lastEyePos;
    private Vec3d lastTargetCenter;
    private int tickIndex;
    private int pauseTicks;
    private int ticksSincePause;
    private float noiseYaw;
    private float noisePitch;
    private float pitchCoupling;
    private float yawGain = 1.0F;
    private float pitchGain = 1.0F;
    private float targetYawVelocity;
    private LivingEntity target;
    private long lastUseTime;
    private final Random random = new Random();

    public NeuroAimController(NeuroDebug neuroDebug) {
        this.debug = neuroDebug;
    }

    public void tick() {
        this.needsUpdate = true;
        if (this.target != null && System.currentTimeMillis() - this.lastUseTime > 500L) {
            this.reset();
            this.target = null;
            this.debug.endEpisode();
        }
    }

    public float[] computeRotation(MixtureDensityNetwork mixtureDensityNetwork, LivingEntity livingEntity, float f) {
        return mixtureDensityNetwork != null && mc.player != null && livingEntity != null && livingEntity.isAlive()
            ? this.compute(mixtureDensityNetwork, livingEntity, livingEntity.getBoundingBox(), mc.player.getYaw(), mc.player.getPitch(), f)
            : null;
    }

    public float[] computeRotationForBox(MixtureDensityNetwork mixtureDensityNetwork, Box box, float f, float f2) {
        return mixtureDensityNetwork != null && mc.player != null ? this.compute(mixtureDensityNetwork, null, box, f, f2, 1.0F) : null;
    }

    private float[] compute(MixtureDensityNetwork mixtureDensityNetwork, LivingEntity livingEntity, Box box, float f, float f2, float f3) {
        long l = System.currentTimeMillis();
        if (livingEntity != this.target || l - this.lastUseTime > 500L) {
            this.reset();
        }

        this.target = livingEntity;
        this.lastUseTime = l;
        if (!this.needsUpdate) {
            return !this.hasRotation ? null : new float[]{MathHelper.wrapDegrees(this.targetYaw - f), this.targetPitch - f2};
        }

        this.needsUpdate = false;
        Vec3d vec3d = mc.player.getEyePos();
        Vec3d vec3d2 = box.getCenter();
        Vec3d vec3d3 = this.lastEyePos != null ? this.lastEyePos : vec3d;
        Vec3d vec3d4 = this.lastTargetCenter != null ? this.lastTargetCenter : vec3d2;
        float[] floats = NeuroFeatures.buildFeatures(
            vec3d,
            vec3d3,
            vec3d2,
            vec3d4,
            (box.maxX - box.minX) * 0.5,
            (box.maxY - box.minY) * 0.5,
            f,
            f2,
            this.lastDeltaYaw,
            this.lastDeltaPitch,
            this.tickIndex
        );
        this.pushFeatures(floats);
        float[] floats2 = this.buildInput();
        Vec3d vec3d5 = vec3d2;
        boolean flag = box.raycast(vec3d, vec3d.add(Vec3d.fromPolar(f2, f).multiply(8.0))).isEmpty();
        if (livingEntity != null && flag) {
            List<Vec3d> vec3ds = AimPointUtil.getVisiblePoints(livingEntity, box);
            if (vec3ds != null && !vec3ds.isEmpty()) {
                float f4 = Float.MAX_VALUE;

                for (Vec3d vec3d6 : vec3ds) {
                    float[] floats3 = NeuroFeatures.calculateRotation(vec3d, vec3d6);
                    float f5 = Math.abs(MathHelper.wrapDegrees(floats3[0] - f));
                    float f6 = Math.abs(floats3[1] - f2);
                    float f7 = f5 * f5 + f6 * f6;
                    if (f7 < f4) {
                        f4 = f7;
                        vec3d5 = vec3d6;
                    }
                }
            }
        }

        float[] floats4 = NeuroFeatures.calculateRotation(vec3d, vec3d5);
        float f8 = MathHelper.wrapDegrees(floats4[0] - f);
        float f9 = floats4[1] - f2;
        float f10 = (float)Math.hypot(f8, f9);
        double d = vec3d2.x - vec3d.x;
        double d2 = vec3d2.y - vec3d.y;
        double d3 = vec3d2.z - vec3d.z;
        double d4 = Math.max(1.0E-4, Math.sqrt(d * d + d3 * d3));
        double d5 = Math.max(1.0E-4, Math.sqrt(d * d + d2 * d2 + d3 * d3));
        float f11 = (float)(Math.toDegrees(Math.atan2((box.maxX - box.minX) * 0.5, d4)) + 3.0);
        float f12 = (float)(Math.toDegrees(Math.atan2((box.maxY - box.minY) * 0.5, d5)) + 3.0);
        float[] floats5 = NeuroFeatures.calculateRotation(vec3d, vec3d2);
        float[] floats6 = NeuroFeatures.calculateRotation(vec3d3, vec3d4);
        this.targetYawVelocity = 0.6F * this.targetYawVelocity + 0.4F * MathHelper.wrapDegrees(floats5[0] - floats6[0]);
        boolean flag2 = this.tickIndex == 0;
        boolean flag3 = Math.abs(f8) < f11 - 3.0F && Math.abs(f9) < (f12 - 3.0F) * 0.7F;
        boolean flag4 = Math.abs(this.lastDeltaYaw) < 6.0F && Math.abs(this.lastDeltaPitch) < 4.0F;
        boolean flag5;
        float f13;
        if (Math.abs(this.targetYawVelocity) < 3.0F) {
            flag5 = true;
            f13 = 0.3F;
        } else {
            flag5 = false;
            f13 = 0.12F;
        }

        float f14 = f13;
        if (!flag3) {
            this.pauseTicks = 0;
        } else if (this.pauseTicks == 0 && this.ticksSincePause > 2 && flag4 && this.random.nextFloat() < f14) {
            this.pauseTicks = 1 + this.random.nextInt(flag5 ? 3 : 2);
        }

        if (this.pauseTicks > 0) {
            this.pauseTicks--;
            this.ticksSincePause = 0;
            this.lastDeltaYaw = 0.0F;
            this.lastDeltaPitch = 0.0F;
            this.lastEyePos = vec3d;
            this.lastTargetCenter = vec3d2;
            this.tickIndex++;
            this.targetYaw = f;
            this.targetPitch = f2;
            this.hasRotation = true;
            if (livingEntity != null) {
                this.debug
                    .record(
                        livingEntity,
                        vec3d,
                        box,
                        vec3d5,
                        f,
                        f2,
                        f8,
                        f9,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        this.targetYaw,
                        this.targetPitch,
                        f10,
                        (float)d5,
                        0.0F,
                        f3,
                        0,
                        flag2
                    );
            }

            return new float[]{0.0F, 0.0F};
        } else {
            this.ticksSincePause++;
            MixtureDensityNetwork.Prediction prediction = mixtureDensityNetwork.predict(floats2);
            float[] floats7 = mixtureDensityNetwork.mixtureMean(prediction);
            float[] floats8 = mixtureDensityNetwork.dominantSigma(prediction);
            float f15 = 0.0F;

            for (float f16 : prediction.weights) {
                if (f16 > f15) {
                    f15 = f16;
                }
            }

            float f17 = MathHelper.clamp(f10 / 6.0F, 0.55F, 1.0F);
            float f18 = MathHelper.clamp(f10 / 6.0F, 0.65F, 1.0F);
            float f19 = Math.min(floats8[0], 0.3F) * 1.3F * f17;
            float f20 = Math.min(floats8[1], 0.18F) * 1.3F * f18;
            float f21 = (float)Math.sqrt(0.96F);
            this.noiseYaw = 0.2F * this.noiseYaw + f21 * (float)this.random.nextGaussian();
            this.noisePitch = 0.2F * this.noisePitch + f21 * (float)this.random.nextGaussian();
            float f22 = (floats7[0] + this.noiseYaw * f19) * 10.0F;
            float f23 = (floats7[1] + this.noisePitch * f20) * 10.0F * 0.5F;
            float f24 = MathHelper.clamp((f10 - 16.0F) / 29.0F, 0.0F, 1.0F);
            float f25 = MathHelper.clamp((f12 - 3.0F) * 0.55F, 4.0F, 25.0F);
            float f26 = MathHelper.clamp((Math.abs(f9) - f25) / (30.0F - f25), 0.0F, 1.0F);
            float f27 = Math.max((f3 - 1.0F) * 0.5F, 0.12F);
            float f28 = f22 + f8 * f27 * f24;
            float f29 = f23 + f9 * Math.max(f27, 0.3F) * f26;
            this.yawGain = 0.5F * this.yawGain + 0.5F * (1.0F + 0.61F * (float)this.random.nextGaussian());
            float f30 = f28 * MathHelper.clamp(this.yawGain, 0.7F, 1.85F);
            this.pitchCoupling = 0.9F * this.pitchCoupling + 0.1F * (float)this.random.nextGaussian();
            float f31 = f29 + f30 * 0.3F * this.pitchCoupling;
            this.pitchGain = 0.5F * this.pitchGain + 0.5F * (1.0F + 0.5F * (float)this.random.nextGaussian());
            float f32 = f31 * MathHelper.clamp(this.pitchGain, 0.6F, 1.6F);
            byte b = 0;
            float f33 = f30;
            float f34 = f32;
            float f35 = 1.8F * (0.4F + 0.6F * this.random.nextFloat());
            float f36 = 1.8F * (0.4F + 0.6F * this.random.nextFloat());
            if (Math.abs(f8) > 10.0F && f30 * f8 < 0.0F) {
                f30 = MathHelper.clamp(f30, -f35, f35);
            }

            if (Math.abs(f9) > 6.0F && f32 * f9 < 0.0F) {
                f32 = MathHelper.clamp(f32, -f36, f36);
            }

            if (f30 != f33) {
                b = 1;
            }

            if (f32 != f34) {
                b |= 2;
            }

            float f37 = f8 * 1.1F;
            float f38 = f9 * 1.1F;
            float f39 = Math.min(f11, 3.0F + 0.1F * Math.abs(f8));
            float f40 = Math.min(f12, 2.0F + 0.1F * Math.abs(f9));
            float f41 = MathHelper.clamp(f30, Math.min(-f11, f37 - f39), Math.max(f11, f37 + f39));
            float f42 = MathHelper.clamp(f32, Math.min(-f12, f38 - f40), Math.max(f12, f38 + f40));
            if (f41 != f30) {
                b |= 4;
            }

            if (f42 != f32) {
                b |= 8;
            }

            float f43 = 50.0F * (0.8F + 0.2F * this.random.nextFloat());
            float f44 = 18.0F * (0.8F + 0.2F * this.random.nextFloat());
            float f45 = 58.0F * (0.8F + 0.2F * this.random.nextFloat());
            float f46 = 22.0F * (0.8F + 0.2F * this.random.nextFloat());
            float f47 = MathHelper.clamp(f41, this.lastDeltaYaw - f43, this.lastDeltaYaw + f43);
            float f48 = MathHelper.clamp(f42, this.lastDeltaPitch - f44, this.lastDeltaPitch + f44);
            float f49 = MathHelper.clamp(f47, -f45, f45);
            float f50 = MathHelper.clamp(f48, -f46, f46);
            if (Math.abs(this.lastDeltaYaw) > 35.0F) {
                float f51 = 10.0F + 8.0F * this.random.nextFloat();
                f49 = MathHelper.clamp(f49, -f51, f51);
            }

            float f52 = 26.0F + 4.0F * this.random.nextFloat();
            if (Math.abs(f49) > f52) {
                f49 = Math.copySign(f52 + (Math.abs(f49) - f52) * 0.4F, f49);
            }

            float f53 = 7.0F + 2.0F * this.random.nextFloat();
            if (Math.abs(f50) > f53) {
                f50 = Math.copySign(f53 + (Math.abs(f50) - f53) * 0.4F, f50);
            }

            if (f49 != f41) {
                b |= 16;
            }

            if (f50 != f42) {
                b |= 32;
            }

            float f54 = SensitivityUtil.snapDelta(f49);
            float f55 = SensitivityUtil.snapDelta(f50);
            float f56 = 74.0F + 6.0F * this.random.nextFloat();
            float f57 = MathHelper.clamp(f2 + f55, -f56, f56);
            float f58 = f57 - f2;
            this.lastDeltaYaw = f54;
            this.lastDeltaPitch = f58;
            this.lastEyePos = vec3d;
            this.lastTargetCenter = vec3d2;
            this.tickIndex++;
            this.targetYaw = f + f54;
            this.targetPitch = f57;
            this.hasRotation = true;
            if (livingEntity != null) {
                this.debug
                    .record(
                        livingEntity,
                        vec3d,
                        box,
                        vec3d5,
                        f,
                        f2,
                        f8,
                        f9,
                        floats7[0] * 10.0F,
                        floats7[1] * 10.0F,
                        f19 * 10.0F,
                        f20 * 10.0F,
                        f54,
                        f57 - f2,
                        this.targetYaw,
                        this.targetPitch,
                        f10,
                        (float)d5,
                        f15,
                        f3,
                        b,
                        flag2
                    );
            }

            return new float[]{f54, f57 - f2};
        }
    }

    public void reset() {
        this.historyCount = 0;
        this.lastDeltaYaw = 0.0F;
        this.lastDeltaPitch = 0.0F;
        this.lastEyePos = null;
        this.lastTargetCenter = null;
        this.tickIndex = 0;
        this.pauseTicks = 0;
        this.ticksSincePause = 0;
        this.noiseYaw = 0.0F;
        this.noisePitch = 0.0F;
        this.yawGain = 1.0F;
        this.pitchGain = 1.0F;
        this.targetYawVelocity = 0.0F;
        this.hasRotation = false;
    }

    private void pushFeatures(float[] floats) {
        if (this.historyCount == 0) {
            for (int i = 0; i < 5; i++) {
                this.history[i] = (float[])floats.clone();
            }
        } else {
            for (int i2 = 0; i2 < 4; i2++) {
                this.history[i2] = this.history[i2 + 1];
            }

            this.history[4] = (float[])floats.clone();
        }

        this.historyCount++;
    }

    private float[] buildInput() {
        float[] floats = new float[55];

        for (int i = 0; i < 5; i++) {
            System.arraycopy(this.history[i], 0, floats, i * 11, 11);
        }

        return floats;
    }
}
