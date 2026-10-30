package tech.sadnes.client.feature.impl.combat.rotation;

import java.lang.reflect.Array;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.FreeLookComponent;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.aura.rotation.SnapAttackHelper;
import tech.sadnes.client.setting.RangeSetting;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

public final class HumanizedAimHandler extends TaskHandler {
    private final RotationComponent.RotationSequence rotationProvider = this::nextRotationStep;
    private final SnapAttackHelper attackTimer = new SnapAttackHelper(this.feature);
    private Rotation overshootOffset = new Rotation(0.0F, 0.0F);
    private Rotation lastRotation;
    private LivingEntity target;
    private Box predictedBox;
    private double[] aimPointOffsets;
    private int aimPointTick = -1;
    private int aimPointTargetId = -1;
    private float jitterYaw;
    private float jitterPitch;
    private float jitterYawGoal;
    private float jitterPitchGoal;
    private int jitterYawStepsLeft;
    private int jitterPitchStepsLeft;
    private int jitterYawStreak;
    private int jitterPitchStreak;
    private int holdTicks;
    private int lastHoldTicks;
    private boolean locked;
    private int lockTick;
    private int attackCount;
    private float yawSideBias = Float.NaN;
    private boolean rotating;
    private boolean tracking;
    private boolean returning;
    private boolean returnStarted;
    private long returnStartTime;
    private int returnTicks;
    private int stopCount;
    private long lastStopTime;

    public HumanizedAimHandler(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public TaskHandler.Phase getPhase() {
        return TaskHandler.Phase.TICK;
    }

    @Override
    public void update(LivingEntity livingEntity) {
        label50: {
            boolean flag;
            boolean flag2;
            boolean flag3;
            boolean flag4;
            LivingEntity livingEntity2;
            Rotation rotation;
            label49: {
                int i;
                label55: {
                    this.attackTimer.tryAttack(livingEntity);
                    this.checkProviderOwnership();
                    double d = CombatUtil.getDistance(livingEntity);
                    double d2 = this.feature.getAttackRange();
                    LivingEntity livingEntity3 = this.target;
                    Rotation rotation2 = this.lastRotation;
                    boolean flag5 = this.returning;
                    boolean flag6 = this.rotating;
                    flag = this.tracking;
                    Rotation rotation3;
                    LivingEntity livingEntity4;
                    if (!(d2 >= d)) {
                        flag2 = flag6;
                        flag3 = flag5;
                        rotation3 = rotation2;
                        livingEntity4 = livingEntity3;
                        if (flag || !flag6) {
                            break label50;
                        }
                    } else {
                        label54: {
                            boolean flag7 = this.feature.willAttackInTicks(2);
                            boolean flag8 = this.locked;
                            LivingEntity livingEntity5 = this.target;
                            i = this.lockTick;
                            if (!flag7) {
                                boolean flag9 = this.isLockedOn(livingEntity);
                                boolean flag10 = this.locked;
                                LivingEntity livingEntity6 = this.target;
                                i = this.lockTick;
                                Rotation rotation4 = this.lastRotation;
                                boolean flag11 = this.returning;
                                boolean flag12 = this.rotating;
                                boolean flag13 = this.tracking;
                                if (!flag9) {
                                    flag2 = flag12;
                                    flag = flag13;
                                    flag3 = flag11;
                                    rotation3 = rotation4;
                                    livingEntity4 = livingEntity6;
                                    if (flag13 || !flag12) {
                                        break label50;
                                    }
                                    break label54;
                                }

                                if (flag10 && livingEntity6 == livingEntity) {
                                    break label55;
                                }
                            } else if (flag8 && livingEntity5 == livingEntity) {
                                break label55;
                            }

                            this.locked = true;
                            i = mc.player.age;
                            break label55;
                        }
                    }

                    if (rotation3 == null) {
                        break label50;
                    }

                    flag4 = false;
                    livingEntity2 = livingEntity4;
                    rotation = rotation3;
                    break label49;
                }

                this.lockTick = i;
                rotation = this.computeTargetRotation(livingEntity);
                flag4 = this.locked;
                flag2 = true;
                flag = false;
                flag3 = true;
                livingEntity2 = livingEntity;
            }

            this.locked = flag4;
            this.target = livingEntity2;
            this.lastRotation = rotation;
            this.returning = flag3;
            this.rotating = flag2;
            this.tracking = flag;
            this.tickReturn();
            return;
        }

        this.locked = false;
        this.startTracking(livingEntity);
    }

    @Override
    public void onStart() {
        this.attackTimer.tryAttack(null);
        this.checkProviderOwnership();
        if (this.tracking) {
            this.locked = false;
            this.stopTracking();
        } else {
            this.locked = false;
            this.tickReturn();
        }
    }

    @Override
    public void restart() {
        if (this.tracking) {
            this.stopTracking();
        }
    }

    @Override
    public void reset() {
        this.attackTimer.reset();
    }

    @Override
    public void onStop() {
        this.attackTimer.onStop();
        this.stopCount++;
        this.lastStopTime = System.currentTimeMillis();
        this.locked = false;
        this.holdTicks = this.rollHoldTicks();
        this.attackCount++;
        this.yawSideBias = this.rollYawSideBias();
    }

    @Override
    public boolean isActive() {
        return this.attackTimer.isActive();
    }

    private void checkProviderOwnership() {
        if (this.rotating && RotationComponent.getInstance().getSequence() != this.rotationProvider) {
            this.clearState();
        }
    }

    private boolean isLockedOn(LivingEntity livingEntity) {
        return this.locked && this.target == livingEntity && livingEntity.isAlive() && !livingEntity.isRemoved() && mc.player.age - this.lockTick <= 40;
    }

    private void startTracking(LivingEntity livingEntity) {
        this.rotating = true;
        this.tracking = true;
        this.target = livingEntity;
        this.lastRotation = null;
        this.returning = false;
        this.returnTicks = 0;
        this.returnStarted = false;
        this.holdTicks = 0;
        this.updateJitter();
        this.applyRotation(addOffset(getServerRotation(), this.jitterYaw, this.jitterPitch));
    }

    private void stopTracking() {
        this.tracking = false;
        this.jitterYaw = 0.0F;
        this.jitterPitch = 0.0F;
        if (mc.player != null) {
            this.applyRotation(getServerRotation());
        }

        this.release();
    }

    private void tickReturn() {
        boolean flag = this.rotating;
        Rotation rotation = this.lastRotation;
        boolean flag2 = this.returning;
        boolean flag3;
        if (flag) {
            label37:
            if (rotation != null) {
                if (!flag2) {
                    RotationComponent.State state = RotationComponent.getInstance().getState();
                    RotationComponent.State state2 = RotationComponent.State.RESET;
                    boolean flag4 = this.returning;
                    if (state == state2) {
                        flag3 = flag4;
                        break label37;
                    }
                }

                Rotation rotation2 = this.getUnjitteredRotation();
                Rotation rotation3 = this.lastRotation;
                boolean flag5 = this.returning;
                boolean flag6 = this.returnStarted;
                long l = this.returnStartTime;
                int i = this.returnTicks;
                float f = this.jitterYaw;
                float f2 = this.jitterPitch;
                int i2 = this.holdTicks;
                LivingEntity livingEntity = this.target;
                Rotation rotation4;
                if (flag5) {
                    this.returning = false;
                    this.returnStarted = false;
                    this.returnTicks = 0;
                    rotation4 = this.stepTowards(rotation2, rotation3, livingEntity);
                } else {
                    int i3;
                    int i4;
                    boolean flag7;
                    float f3;
                    boolean flag8;
                    float f4;
                    long l2;
                    if (!flag6) {
                        this.returning = false;
                        this.returnStarted = true;
                        long l3 = System.currentTimeMillis();
                        boolean flag9 = this.returning;
                        boolean flag10 = this.returnStarted;
                        int i5 = this.returnTicks;
                        float f5 = this.jitterYaw;
                        float f6 = this.jitterPitch;
                        i3 = this.holdTicks;
                        i4 = i5;
                        flag7 = flag9;
                        f3 = f5;
                        flag8 = flag10;
                        f4 = f6;
                        l2 = l3;
                    } else {
                        i3 = i2;
                        i4 = i;
                        flag7 = false;
                        f3 = f;
                        flag8 = flag6;
                        f4 = f2;
                        l2 = l;
                    }

                    int i6 = (int)(Integer.toUnsignedLong(i4) + 1L);
                    if (i4 >= 42) {
                        this.returning = flag7;
                        this.returnStarted = flag8;
                        this.returnStartTime = l2;
                        this.returnTicks = i6;
                        this.jitterYaw = 0.0F;
                        this.jitterPitch = 0.0F;
                        this.holdTicks = i3;
                        Rotation rotation5 = getServerRotation();
                        this.applyRotation(rotation5);
                        this.release();
                        flag3 = this.returning;
                        break label37;
                    }

                    if (i3 > 0) {
                        this.returning = flag7;
                        this.returnStarted = flag8;
                        this.returnStartTime = l2;
                        this.returnTicks = i6;
                        this.jitterYaw = f3;
                        this.jitterPitch = f4;
                        int i7 = i3 - 1;
                        this.holdTicks = i7;
                        long l4 = System.currentTimeMillis();
                        this.returnStartTime = l4;
                        this.updateJitter();
                        float f7 = this.jitterYaw;
                        float f8 = this.jitterPitch;
                        Rotation rotation6 = addOffset(rotation2, f7, f8);
                        this.applyRotation(rotation6);
                        flag3 = this.returning;
                        break label37;
                    }

                    this.returning = flag7;
                    this.returnStarted = flag8;
                    this.returnStartTime = l2;
                    this.returnTicks = i6;
                    this.jitterYaw = f3;
                    this.jitterPitch = f4;
                    this.holdTicks = i3;
                    Rotation rotation7 = getServerRotation();
                    rotation4 = this.stepTowards(rotation2, rotation7, null);
                }

                this.updateJitter();
                float f9 = this.jitterYaw;
                float f10 = this.jitterPitch;
                Rotation rotation8 = addOffset(rotation4, f9, f10);
                this.applyRotation(rotation8);
                flag3 = this.returning;
            } else {
                flag3 = false;
            }
        } else {
            flag3 = false;
        }

        this.returning = flag3;
    }

    private Rotation stepTowards(Rotation rotation, Rotation rotation2, LivingEntity livingEntity) {
        long l = System.currentTimeMillis() - this.lastStopTime;
        long l2 = this.returnStarted ? Math.max(0L, System.currentTimeMillis() - this.returnStartTime) : 0L;
        float f = MathHelper.clamp(
            2.0F - (float)l2 / (240.0F + CombatUtil.randomLerp(0.0F, 12.0F)), this.stopCount % 5 / 100.0F + this.stopCount % 2 / 4.0F, 1.0F
        );
        Rotation rotation3 = livingEntity != null ? rotation2 : addOffset(rotation2, f * this.overshootOffset.getYaw(), f * this.overshootOffset.getPitch());
        float f2 = MathHelper.wrapDegrees(rotation3.getYaw() - rotation.getYaw());
        float f3 = MathHelper.wrapDegrees(rotation3.getPitch() - rotation.getPitch());
        float f4 = (float)Math.hypot(f2, f3);
        float f5 = Math.max(f4, 1.0E-4F);
        float f6 = Math.abs(f2 / f5) * 180.0F;
        float f7 = Math.abs(f3 / f5) * 180.0F;
        float f8 = MathHelper.clamp(f2, -f6, f6);
        float f9 = MathHelper.clamp(f3, -f7, f7);
        if (l < 22L) {
            return rotation;
        }

        boolean flag = livingEntity != null && (this.feature.willAttackInTicks(1) || this.locked && mc.player.age > this.lockTick) && f4 < 92.0F;
        if (flag) {
            this.overshootOffset = new Rotation(
                f8 < 0.0F ? CombatUtil.randomLerp(24.0F, 39.0F) / 1.5F : -CombatUtil.randomLerp(24.0F, 39.0F) / 1.5F,
                f9 < 0.0F ? CombatUtil.randomLerp(9.0F, 19.0F) / 8.0F : -CombatUtil.randomLerp(9.0F, 19.0F) / 8.0F
            );
            return this.correctToHitbox(addOffset(rotation, f8, f9), livingEntity);
        }

        float f10;
        float f11;
        if (livingEntity == null && l2 >= 160L) {
            f10 = 1.0F;
            f11 = 1.0F;
        } else {
            f10 = CombatUtil.randomLerp(0.48F, 0.66F);
            f11 = CombatUtil.randomLerp(0.48F, 0.66F);
        }

        float f12 = f11;
        return addOffset(rotation, f10 * f8, f12 * f9);
    }

    private Rotation correctToHitbox(Rotation rotation, LivingEntity livingEntity) {
        Box box;
        boolean flag;
        label19: {
            float f = (float)this.feature.getAttackRange();
            box = livingEntity.getBoundingBox();
            boolean flag2;
            if (!RaycastUtil.isLookingAtBox(box, rotation.getYaw(), rotation.getPitch(), f)) {
                if (this.predictedBox != null) {
                    flag = RaycastUtil.isLookingAtBox(this.predictedBox, rotation.getYaw(), rotation.getPitch(), f);
                    break label19;
                }

                flag2 = false;
            } else {
                flag2 = true;
            }

            flag = flag2;
        }

        if (flag) {
            return rotation;
        }

        float[] floats = RotationUtil.getRotationArray(mc.player.getEyePos(), box.getCenter());
        float f2 = MathHelper.wrapDegrees(floats[0] - rotation.getYaw());
        float f3 = MathHelper.wrapDegrees(floats[1] - rotation.getPitch());
        return new Rotation(rotation.getYaw() + f2 * 0.5F, MathHelper.clamp(rotation.getPitch() + f3 * 0.5F, -90.0F, 90.0F));
    }

    private RotationComponent.SequenceStep nextRotationStep() {
        boolean flag;
        if (this.rotating) {
            this.returning = false;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            boolean flag2 = this.returning;
            Rotation rotation = this.lastRotation;
            boolean flag3 = this.returnStarted;
            long l = this.returnStartTime;
            if (clientPlayerEntity != null) {
                if (rotation != null) {
                    if (!flag3) {
                        this.returnStarted = true;
                        l = System.currentTimeMillis();
                    }

                    this.returnStartTime = l;
                    Rotation rotation2 = this.getUnjitteredRotation();
                    boolean flag4 = this.returning;
                    boolean flag5 = this.returnStarted;
                    long l2 = this.returnStartTime;
                    int i = this.returnTicks;
                    float f = this.jitterYaw;
                    float f2 = this.jitterPitch;
                    int i2 = (int)(Integer.toUnsignedLong(i) + 1L);
                    RotationComponent.SequenceStep sequenceStep;
                    if (i < 42) {
                        this.returning = flag4;
                        this.returnStarted = flag5;
                        this.returnStartTime = l2;
                        this.returnTicks = i2;
                        this.jitterYaw = f;
                        this.jitterPitch = f2;
                        Rotation rotation3 = getServerRotation();
                        Rotation rotation4 = this.stepTowards(rotation2, rotation3, null);
                        this.updateJitter();
                        float f3 = this.jitterYaw;
                        float f4 = this.jitterPitch;
                        Rotation rotation5 = addOffset(rotation4, f3, f4);
                        sequenceStep = new RotationComponent.SequenceStep(rotation5, 360.0F, 360.0F);
                    } else {
                        this.returning = flag4;
                        this.returnStarted = flag5;
                        this.returnStartTime = l2;
                        this.returnTicks = i2;
                        this.jitterYaw = 0.0F;
                        this.jitterPitch = 0.0F;
                        this.clearState();
                        Rotation rotation6 = getServerRotation();
                        sequenceStep = new RotationComponent.SequenceStep(rotation6, 360.0F, 360.0F);
                    }

                    return sequenceStep;
                }

                flag = flag2;
            } else {
                flag = flag2;
            }
        } else {
            flag = false;
        }

        this.returning = flag;
        this.clearState();
        return null;
    }

    private void applyRotation(Rotation rotation) {
        RotationComponent.rotateSequence(rotation, 360.0F, 360.0F, 360.0F, 360.0F, 0, 5, false, this.rotationProvider);
    }

    private void release() {
        RotationComponent rotationComponent = RotationComponent.getInstance();
        if (rotationComponent.getSequence() == this.rotationProvider) {
            rotationComponent.stop();
        }

        this.clearState();
    }

    private void clearState() {
        this.rotating = false;
        this.returning = false;
        this.lastRotation = null;
        this.target = null;
        this.predictedBox = null;
        this.aimPointOffsets = null;
        this.jitterYaw = 0.0F;
        this.jitterPitch = 0.0F;
        this.jitterYawGoal = 0.0F;
        this.jitterPitchGoal = 0.0F;
        this.jitterYawStepsLeft = 0;
        this.jitterPitchStepsLeft = 0;
        this.jitterYawStreak = 0;
        this.jitterPitchStreak = 0;
        this.holdTicks = 0;
        this.locked = false;
        this.tracking = false;
        this.returnStarted = false;
        this.attackCount = 0;
        this.yawSideBias = Float.NaN;
        this.returnTicks = 0;
        RotationComponent rotationComponent = RotationComponent.getInstance();
        if (rotationComponent.getSequence() == this.rotationProvider) {
            rotationComponent.setSequence(null);
        }
    }

    private Rotation getUnjitteredRotation() {
        return addOffset(new Rotation(mc.player), -this.jitterYaw, -this.jitterPitch);
    }

    private static Rotation getServerRotation() {
        return new Rotation(FreeLookComponent.getYaw(), FreeLookComponent.getPitch());
    }

    private static Rotation addOffset(Rotation rotation, float f, float f2) {
        return new Rotation(rotation.getYaw() + f, MathHelper.clamp(rotation.getPitch() + f2, -90.0F, 90.0F));
    }

    private boolean shouldJitter() {
        if (!this.feature.isEnabled() || !this.rotating) {
            return false;
        } else {
            return this.tracking ? true : this.target != null && !this.locked && !this.feature.willAttackInTicks(2);
        }
    }

    private void updateJitter() {
        if (!this.shouldJitter()) {
            this.jitterYaw = 0.0F;
            this.jitterPitch = 0.0F;
            this.jitterYawGoal = 0.0F;
            this.jitterPitchGoal = 0.0F;
            this.jitterYawStepsLeft = 0;
            this.jitterPitchStepsLeft = 0;
            this.jitterYawStreak = 0;
            this.jitterPitchStreak = 0;
        } else {
            this.jitterYaw = this.nextJitterValue(this.jitterYaw, 10.0F, 13.5F, true);
            this.jitterPitch = this.nextJitterValue(this.jitterPitch, 4.5F, 7.0F, false);
        }
    }

    private float nextJitterValue(float f, float f2, float f3, boolean flag) {
        int i = flag ? this.jitterYawStepsLeft : this.jitterPitchStepsLeft;
        float f4 = flag ? this.jitterYawGoal : this.jitterPitchGoal;
        if (i <= 0 || f4 == 0.0F) {
            int i2 = flag ? this.jitterYawStreak : this.jitterPitchStreak;
            float f5 = f4 == 0.0F ? (CombatUtil.randomBoolean() ? 1.0F : -1.0F) : Math.signum(f4);
            float f6 = i2 >= 3 ? 1.0F : (i2 == 2 ? 0.85F : 0.5F);
            byte b;
            float f7;
            if (f4 != 0.0F && CombatUtil.randomFloat() < f6) {
                b = 1;
                f7 = -f5;
            } else {
                b = (byte)(f4 == 0.0F ? 1 : i2 + 1);
                f7 = f5;
            }

            f4 = f7 * CombatUtil.randomLerp(f2, f3) * CombatUtil.randomLerp(0.8F, 1.0F);
            float f8 = CombatUtil.randomFloat();
            i = f8 < 0.15F ? 1 : (f8 < 0.7F ? 2 : 3);
            if (flag) {
                this.jitterYawStreak = b;
                this.jitterYawGoal = f4;
            } else {
                this.jitterPitchStreak = b;
                this.jitterPitchGoal = f4;
            }
        }

        float f9 = i <= 1 ? f4 : f + (f4 - f) / i * CombatUtil.randomLerp(0.85F, 1.15F);
        if (flag) {
            this.jitterYawStepsLeft = i - 1;
            return f9;
        } else {
            this.jitterPitchStepsLeft = i - 1;
            return f9;
        }
    }

    private int rollHoldTicks() {
        RangeSetting rangeSetting = this.feature.getFtsnapsHang();
        int i = Math.round(rangeSetting.getLow());
        int i2 = Math.round(rangeSetting.getHigh()) - i;
        double d = i + i2 * 0.4;
        double d2 = Math.max(1.0, i2 * 0.28);
        double[] doubles = new double[i2 + 1];
        double d3 = 0.0;

        for (int i3 = 0; i3 <= i2; i3++) {
            double d4 = i + i3 - d;
            doubles[i3] = Math.exp(-(d4 * d4) / (2.0 * d2 * d2));
            d3 += doubles[i3];
        }

        int i4 = i;

        for (int i5 = 0; i5 < 2; i5++) {
            double d5 = CombatUtil.randomDouble(0.0, d3);
            double d6 = 0.0;
            i4 = i + i2;

            for (int i6 = 0; i6 <= i2; i6++) {
                d6 += doubles[i6];
                if (d5 < d6) {
                    i4 = i + i6;
                    break;
                }
            }

            if (i4 != this.lastHoldTicks) {
                this.lastHoldTicks = i4;
                return i4;
            }
        }

        this.lastHoldTicks = i4;
        return i4;
    }

    private float rollYawSideBias() {
        switch (Math.floorMod(this.attackCount, 8)) {
            case 1:
                return -CombatUtil.randomLerp(0.62F, 0.78F);
            case 2:
                return CombatUtil.randomLerp(0.62F, 0.78F);
            case 3:
            case 5:
            default:
                return CombatUtil.randomLerp(-0.12F, 0.12F);
            case 4:
                return -CombatUtil.randomLerp(0.9F, 0.97F);
            case 6:
                return CombatUtil.randomLerp(0.9F, 0.97F);
        }
    }

    private Rotation computeTargetRotation(LivingEntity livingEntity) {
        Vec3d vec3d = mc.player.getEyePos();
        Box box = livingEntity.getBoundingBox();
        double d = MathHelper.clamp(ExactMath.divide(ExactMath.add(CombatUtil.clampToBox(vec3d, box).distanceTo(vec3d), -0.8), 1.2), 0.0, 1.0);
        double d2 = livingEntity.getX();
        double d3 = livingEntity.lastX;
        double d4 = livingEntity.getY();
        double d5 = livingEntity.lastY;
        double d6 = livingEntity.getZ();
        double d7 = livingEntity.lastZ;
        double[] doubles = this.aimPointOffsets;
        int i = this.aimPointTick;
        int i2 = this.aimPointTargetId;
        Box box2 = this.predictedBox;
        double d8 = ExactMath.subtract(d2, d3);
        double d9 = ExactMath.subtract(d4, d5);
        double d10 = ExactMath.subtract(d6, d7);
        double d11 = ExactMath.multiply(d8, d8);
        double d12 = ExactMath.add(ExactMath.multiply(d9, d9), d11);
        double d13 = ExactMath.add(ExactMath.multiply(d10, d10), d12);
        Box box3;
        int i3;
        int i4;
        double[] doubles2;
        Box box4;
        if (1.0 > d13) {
            this.aimPointOffsets = doubles;
            this.aimPointTick = i;
            this.aimPointTargetId = i2;
            this.predictedBox = box2;
            double d14 = ExactMath.multiply(d8, d);
            double d15 = ExactMath.multiply(d9, d);
            double d16 = ExactMath.multiply(d10, d);
            Box box5 = box.offset(d14, d15, d16);
            double[] doubles3 = this.aimPointOffsets;
            int i5 = this.aimPointTick;
            int i6 = this.aimPointTargetId;
            Box box6 = this.predictedBox;
            box3 = box5;
            i3 = i5;
            i4 = i6;
            doubles2 = doubles3;
            box4 = box6;
        } else {
            box3 = box;
            i3 = i;
            i4 = i2;
            doubles2 = doubles;
            box4 = box2;
        }

        this.aimPointOffsets = doubles2;
        this.aimPointTick = i3;
        this.aimPointTargetId = i4;
        this.predictedBox = box4;
        double d17 = Math.max(box3.getLengthX(), 1.0E-4);
        double d18 = Math.max(box3.getLengthY(), 1.0E-4);
        double d19 = Math.max(box3.getLengthZ(), 1.0E-4);
        Vec3d vec3d2 = CombatUtil.clampToBox(vec3d, box3);
        double[] doubles4 = new double[3];
        double d20 = vec3d2.x;
        double d21 = box3.minX;
        double d22 = MathHelper.clamp(ExactMath.divide(ExactMath.subtract(d20, d21), d17), 0.1, 0.9);
        Array.setDouble(doubles4, 0, d22);
        double d23 = vec3d2.y;
        double d24 = box3.minY;
        double d25 = MathHelper.clamp(ExactMath.divide(ExactMath.subtract(d23, d24), d18), 0.1, 0.9);
        Array.setDouble(doubles4, 1, d25);
        double d26 = vec3d2.z;
        double d27 = box3.minZ;
        double d28 = MathHelper.clamp(ExactMath.divide(ExactMath.subtract(d26, d27), d19), 0.1, 0.9);
        Array.setDouble(doubles4, 2, d28);
        int i7 = mc.player.age;
        double[] doubles5 = this.aimPointOffsets;
        int i8 = this.aimPointTick;
        if (doubles5 != null) {
            int i9 = livingEntity.getId();
            int i10 = this.aimPointTargetId;
            if (i8 == i9 && i7 - i10 <= 2) {
                double d29 = Math.max(0.15, d);
                double d30 = Array.getDouble(this.aimPointOffsets, 0);
                double d31 = Array.getDouble(doubles4, 0);
                double d32 = Array.getDouble(this.aimPointOffsets, 0);
                double d33 = ExactMath.add(ExactMath.multiply(ExactMath.subtract(d31, d32), d29), d30);
                Array.setDouble(doubles4, 0, d33);
                double d34 = Array.getDouble(this.aimPointOffsets, 1);
                double d35 = Array.getDouble(doubles4, 1);
                double d36 = Array.getDouble(this.aimPointOffsets, 1);
                double d37 = ExactMath.add(ExactMath.multiply(ExactMath.subtract(d35, d36), d29), d34);
                Array.setDouble(doubles4, 1, d37);
                double d38 = Array.getDouble(this.aimPointOffsets, 2);
                double d39 = Array.getDouble(doubles4, 2);
                double d40 = Array.getDouble(this.aimPointOffsets, 2);
                double d41 = ExactMath.add(ExactMath.multiply(ExactMath.subtract(d39, d40), d29), d38);
                Array.setDouble(doubles4, 2, d41);
            }
        }

        this.aimPointOffsets = doubles4;
        int i11 = livingEntity.getId();
        this.aimPointTick = i11;
        this.aimPointTargetId = i7;
        this.predictedBox = box3;
        double d42 = box3.minX;
        double d43 = Array.getDouble(doubles4, 0);
        double d44 = box3.minY;
        double d45 = Array.getDouble(doubles4, 1);
        double d46 = box3.minZ;
        double d47 = Array.getDouble(doubles4, 2);
        double d48 = ExactMath.add(ExactMath.multiply(d17, d43), d42);
        double d49 = ExactMath.add(ExactMath.multiply(d18, d45), d44);
        double d50 = ExactMath.add(ExactMath.multiply(d19, d47), d46);
        Vec3d vec3d3 = new Vec3d(d48, d49, d50);
        float[] floats = RotationUtil.getRotationArray(vec3d, vec3d3);
        float f = Array.getFloat(floats, 0);
        float f2 = this.applyYawSideBias(box3, f);
        float f3 = Array.getFloat(floats, 1);
        return new Rotation(f2, f3);
    }

    private float applyYawSideBias(Box box, float f) {
        if (Float.isNaN(this.yawSideBias)) {
            this.yawSideBias = this.rollYawSideBias();
        }

        Vec3d vec3d = mc.player.getEyePos();
        Vec3d vec3d2 = box.getCenter();
        float f2 = yawTo(vec3d, vec3d2.x, vec3d2.z);
        float f3 = 0.0F;
        float f4 = 0.0F;

        for (double d : new double[]{box.minX, box.maxX}) {
            for (double d2 : new double[]{box.minZ, box.maxZ}) {
                float f5 = MathHelper.wrapDegrees(yawTo(vec3d, d, d2) - f2);
                f3 = Math.min(f3, f5);
                f4 = Math.max(f4, f5);
            }
        }

        if (f3 > -0.01F && f4 < 0.01F) {
            return f;
        }

        float f6 = this.yawSideBias < 0.0F ? -f3 : f4;
        return MathHelper.wrapDegrees(f2 + MathHelper.clamp(this.yawSideBias * f6, -40.0F, 40.0F));
    }

    private static float yawTo(Vec3d vec3d, double d, double d2) {
        return (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d2 - vec3d.z, d - vec3d.x)) - 90.0);
    }
}
