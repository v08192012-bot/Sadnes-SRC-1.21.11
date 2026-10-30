package tech.sadnes.client.feature.impl.combat.aura.rotation;

import java.lang.reflect.Array;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.WaveSetting;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.client.util.rotation.SensitivityUtil;

public final class FunTimeRotation extends TaskHandler {
    private static final float[][] WAVE_PATTERNS = new float[][]{
        {1.0F, (float) (Math.PI / 2), 0.5F},
        {1.0F, (float) (-Math.PI / 2), 0.5F},
        {2.0F, 0.0F, 0.45F},
        {2.0F, (float) (Math.PI / 2), 0.4F},
        {1.0F, 0.0F, 0.45F},
        {1.0F, (float) Math.PI, 0.45F},
        {3.0F, 0.0F, 0.3F},
        {1.0F, (float) (Math.PI / 2), 0.15F}
    };
    private static final float[][] SPEED_PROFILES = new float[][]{{1.0F, 1.0F}, {1.5F, 1.5F}, {2.0F, 2.0F}, {2.2F, 1.0F}, {3.0F, 1.5F}, {4.0F, 1.0F}};
    private final StopWatch frameTimer = new StopWatch();
    private float speedMultiplier = 1.0F;
    private boolean firstTick = true;
    private int tickCounter;
    private int profileIndex = 2;
    private boolean overshooting;
    private int profileDuration = 12;
    private int patternIndex;
    private int patternTicks;
    private boolean settled;
    private float maxYawSpeed = 25.0F;
    private float maxPitchSpeed = 11.5F;
    private float accelerationLimit = 40.0F;
    private int stallTicks;
    private int aimPointTicks;
    private final float[] targetOffset = new float[2];
    private final float[] currentOffset = new float[2];
    private float wavePhase;
    private int jitterTicks;
    private int pauseTicks;
    private int pauseInterval = 3;
    private float minSmoothing = 1.8F;
    private float maxSmoothing = 3.5F;
    private double lastDistance;
    private float[] aimPoint = new float[2];
    private float lastYawDelta;
    private float lastPitchDelta;
    private float targetYaw;
    private LivingEntity lastTarget;

    public FunTimeRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public void update(LivingEntity livingEntity) {
        StopWatch stopWatch = this.frameTimer;
        long l = stopWatch.getElapsed();
        float f = Math.min(ExactMath.divide((float)l, 1000.0F), 0.05F);
        stopWatch.reset();
        MinecraftClient minecraftClient = mc;
        ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
        float f2 = clientPlayerEntity.getYaw();
        MinecraftClient minecraftClient2 = mc;
        ClientPlayerEntity clientPlayerEntity2 = minecraftClient2.player;
        float f3 = clientPlayerEntity2.getPitch();
        MinecraftClient minecraftClient3 = mc;
        ClientPlayerEntity clientPlayerEntity3 = minecraftClient3.player;
        Vec3d vec3d = clientPlayerEntity3.getEyePos();
        Box box = livingEntity.getBoundingBox();
        Vec3d vec3d2 = box.getCenter();
        float[] floats = RotationUtil.getRotationArray(vec3d, vec3d2);
        LivingEntity livingEntity2 = this.lastTarget;
        boolean flag = this.firstTick;
        int i = this.tickCounter;
        float f4 = this.targetYaw;
        float[] floats2 = this.aimPoint;
        float f5 = this.lastYawDelta;
        int i2 = this.aimPointTicks;
        int i3;
        float f6;
        int i4;
        float f7;
        LivingEntity livingEntity3;
        float[] floats3;
        if (livingEntity != livingEntity2) {
            this.lastTarget = livingEntity;
            this.firstTick = true;
            this.tickCounter = 0;
            float f8 = Array.getFloat(floats, 0);
            float[] floats4 = this.targetOffset;
            this.targetYaw = f8;
            Array.setFloat(floats4, 1, 0.0F);
            Array.setFloat(floats4, 0, 0.0F);
            float[] floats5 = this.computeAimOffset(0.0F);
            this.aimPoint = floats5;
            float f9 = Array.getFloat(floats5, 1);
            float f10 = this.lastPitchDelta;
            float f11 = this.randomizeDelta(f10);
            LivingEntity livingEntity4 = this.lastTarget;
            flag = this.firstTick;
            int i5 = this.tickCounter;
            f6 = this.targetYaw;
            float[] floats6 = this.aimPoint;
            int i6 = this.aimPointTicks;
            f7 = ExactMath.subtract(ExactMath.subtract(f3, f9), f11);
            i3 = i5;
            i4 = i6;
            livingEntity3 = livingEntity4;
            floats3 = floats6;
        } else {
            i3 = i;
            f6 = f4;
            i4 = i2;
            f7 = f5;
            livingEntity3 = livingEntity2;
            floats3 = floats2;
        }

        this.lastTarget = livingEntity3;
        this.firstTick = flag;
        this.tickCounter = i3;
        this.targetYaw = f6;
        this.aimPoint = floats3;
        this.lastYawDelta = f7;
        this.aimPointTicks = i4;
        double d = CombatUtil.getDistance(livingEntity);
        float f12 = MathHelper.clamp(ExactMath.divide((float)ExactMath.add(d, -0.8), 1.2F), 0.0F, 1.0F);
        float f13 = Array.getFloat(floats, 0);
        float f14 = this.targetYaw;
        float f15 = MathHelper.wrapDegrees(ExactMath.subtract(f13, f14));
        float f16 = MathHelper.clamp(f15, -30.0F, 30.0F);
        float f17 = Array.getFloat(floats, 0);
        this.targetYaw = f17;
        boolean flag2 = AttackTimer.hasCooldownPassed(-150L);
        LivingEntity livingEntity5 = this.lastTarget;
        boolean flag3 = this.firstTick;
        int i7 = this.tickCounter;
        float f18 = this.targetYaw;
        float[] floats7 = this.aimPoint;
        float f19 = this.lastYawDelta;
        int i8 = this.aimPointTicks;
        float f20 = ExactMath.multiply(f12, f16);
        float f21;
        float f22;
        int i9;
        float f23;
        int i10;
        LivingEntity livingEntity6;
        float[] floats8;
        if (!flag2) {
            this.lastTarget = livingEntity5;
            this.firstTick = flag3;
            this.tickCounter = i7;
            this.targetYaw = f18;
            this.aimPoint = floats7;
            this.lastYawDelta = f19;
            this.aimPointTicks = i8;
            float f24 = AttackTimer.getElapsedMs();
            float f25 = MathHelper.clamp(ExactMath.divide(ExactMath.add(f24, -100.0F), 100.0F), 0.0F, 1.0F);
            LivingEntity livingEntity7 = this.lastTarget;
            flag3 = this.firstTick;
            int i11 = this.tickCounter;
            float f26 = this.targetYaw;
            float[] floats9 = this.aimPoint;
            float f27 = this.lastYawDelta;
            int i12 = this.aimPointTicks;
            i9 = i11;
            f23 = f26;
            f21 = f25;
            i10 = i12;
            f22 = f27;
            livingEntity6 = livingEntity7;
            floats8 = floats9;
        } else {
            f21 = 0.0F;
            f22 = f19;
            i9 = i7;
            f23 = f18;
            i10 = i8;
            livingEntity6 = livingEntity5;
            floats8 = floats7;
        }

        this.lastTarget = livingEntity6;
        this.firstTick = flag3;
        this.tickCounter = i9;
        this.targetYaw = f23;
        this.aimPoint = floats8;
        this.lastYawDelta = f22;
        this.aimPointTicks = i10;
        float f28 = ExactMath.multiply(f21, f);
        float[] floats10 = this.computeAimOffset(f28);
        float[] floats11 = new float[2];
        float f29 = Array.getFloat(floats10, 0);
        float[] floats12 = this.aimPoint;
        float f30 = Array.getFloat(floats12, 0);
        float f31 = ExactMath.subtract(f29, f30);
        Array.setFloat(floats11, 0, f31);
        float f32 = Array.getFloat(floats10, 1);
        float[] floats13 = this.aimPoint;
        float f33 = Array.getFloat(floats13, 1);
        float f34 = ExactMath.subtract(f32, f33);
        Array.setFloat(floats11, 1, f34);
        float[] floats14 = this.aimPoint;
        float[] floats15;
        if (flag2) {
            float[] floats16 = new float[2];
            floats15 = floats16;
        } else {
            floats15 = floats14;
        }

        this.aimPoint = floats10;
        boolean flag4 = ServerManager.isIntersecting(livingEntity, 0.1F);
        LivingEntity livingEntity8 = this.lastTarget;
        boolean flag5 = this.firstTick;
        int i13 = this.tickCounter;
        float f35 = this.targetYaw;
        float[] floats17 = this.aimPoint;
        float f36 = this.lastYawDelta;
        int i14 = this.aimPointTicks;
        float f37 = ExactMath.add(f20, f2);
        int i15;
        byte b;
        float f38;
        float f39;
        LivingEntity livingEntity9;
        float[] floats18;
        if (flag4) {
            this.lastTarget = livingEntity8;
            this.firstTick = flag5;
            this.tickCounter = i13;
            this.targetYaw = f35;
            this.aimPoint = floats17;
            this.lastYawDelta = f36;
            this.aimPointTicks = i14;
            boolean flag6 = RaycastUtil.isPitchFacing(livingEntity, f2);
            LivingEntity livingEntity10 = this.lastTarget;
            flag5 = this.firstTick;
            i13 = this.tickCounter;
            f38 = this.targetYaw;
            float[] floats19 = this.targetOffset;
            floats18 = this.aimPoint;
            f39 = this.lastYawDelta;
            int i16 = this.aimPointTicks;
            if (flag6) {
                Array.setFloat(floats11, 0, 0.0F);
                Array.setFloat(floats19, 0, 0.0F);
                LivingEntity livingEntity11 = this.lastTarget;
                flag5 = this.firstTick;
                i13 = this.tickCounter;
                f38 = this.targetYaw;
                float[] floats20 = this.aimPoint;
                float f40 = this.lastYawDelta;
                int i17 = this.aimPointTicks;
                i15 = i17;
                f39 = f40;
                f37 = f2;
                b = 0;
                livingEntity9 = livingEntity11;
                floats18 = floats20;
            } else {
                i15 = i16;
                livingEntity9 = livingEntity10;
                b = 1;
            }
        } else {
            i15 = i14;
            b = 1;
            f38 = f35;
            f39 = f36;
            livingEntity9 = livingEntity8;
            floats18 = floats17;
        }

        boolean flag7;
        short s;
        label161: {
            this.lastTarget = livingEntity9;
            this.firstTick = flag5;
            this.tickCounter = i13;
            this.targetYaw = f38;
            this.aimPoint = floats18;
            this.lastYawDelta = f39;
            this.aimPointTicks = i15;
            AttackAura attackAura = this.feature;
            ModeSetting modeSetting = attackAura.getAttackMode();
            flag7 = modeSetting.is("1.9+");
            boolean flag8 = this.settled;
            if (flag8) {
                s = 450;
                if (!flag7) {
                    break label161;
                }
            } else {
                s = 200;
                if (!flag7) {
                    break label161;
                }
            }

            boolean flag9 = AttackTimer.hasElapsed(Integer.toUnsignedLong(s));
            float[] floats21 = this.targetOffset;
            if (!flag9) {
                Array.setFloat(floats21, 1, 0.0F);
                Array.setFloat(floats21, 0, 0.0F);
                float f41 = Array.getFloat(floats11, 0);
                float f42 = Array.getFloat(floats11, 1);
                this.applyRotation(f41, f42, f);
                return;
            }
        }

        float f43 = Array.getFloat(floats, 0);
        float f44 = MathHelper.wrapDegrees(ExactMath.subtract(f2, f43));
        this.updateProfile(f44);
        float f45 = Array.getFloat(floats, 0);
        float[] floats22 = getRotationToBox(vec3d, box, f45);
        float f46 = this.speedMultiplier;
        boolean flag10;
        int i18;
        int i19;
        float f47;
        int i20;
        float f48;
        LivingEntity livingEntity12;
        float[] floats23;
        if (!(0.0F > f46)) {
            float f49 = Array.getFloat(floats22, 1);
            LivingEntity livingEntity13 = this.lastTarget;
            flag10 = this.firstTick;
            i19 = this.tickCounter;
            float f50 = this.targetYaw;
            float[] floats24 = this.aimPoint;
            float f51 = this.lastYawDelta;
            i20 = this.aimPointTicks;
            i18 = Float.floatToRawIntBits(f49);
            f47 = f50;
            f48 = f51;
            livingEntity12 = livingEntity13;
            floats23 = floats24;
        } else {
            float f52 = Array.getFloat(floats22, 0);
            LivingEntity livingEntity14 = this.lastTarget;
            flag10 = this.firstTick;
            int i21 = this.tickCounter;
            float f53 = this.targetYaw;
            float[] floats25 = this.aimPoint;
            float f54 = this.lastYawDelta;
            int i22 = this.aimPointTicks;
            i18 = Integer.MIN_VALUE ^ Float.floatToRawIntBits(f52);
            i19 = i21;
            f47 = f53;
            i20 = i22;
            f48 = f54;
            livingEntity12 = livingEntity14;
            floats23 = floats25;
        }

        float f55;
        float f56;
        int i23;
        LivingEntity livingEntity15;
        float[] floats26;
        boolean flag11;
        float f57;
        float f58;
        float f59;
        int i24;
        float f60;
        int i25;
        label154: {
            boolean flag12;
            int i26;
            float f61;
            int i27;
            float f62;
            int i28;
            float f63;
            float f64;
            LivingEntity livingEntity16;
            float[] floats27;
            label165: {
                int i29;
                int i30;
                boolean flag13;
                float f65;
                float f66;
                float f67;
                float f68;
                LivingEntity livingEntity17;
                float[] floats28;
                label152: {
                    label151: {
                        float f69;
                        label166: {
                            float f70;
                            boolean flag14;
                            float f71;
                            LivingEntity livingEntity18;
                            float[] floats29;
                            label167: {
                                this.lastTarget = livingEntity12;
                                this.firstTick = flag10;
                                this.tickCounter = i19;
                                this.targetYaw = f47;
                                this.aimPoint = floats23;
                                this.lastYawDelta = f48;
                                this.aimPointTicks = i20;
                                f55 = Math.min(Float.intBitsToFloat(i18), 30.0F);
                                float f72 = distanceToBox(vec3d, livingEntity);
                                LivingEntity livingEntity19 = this.lastTarget;
                                boolean flag15 = this.firstTick;
                                int i31 = this.tickCounter;
                                float f73 = this.targetYaw;
                                float[] floats30 = this.aimPoint;
                                float f74 = this.lastYawDelta;
                                float f75 = this.speedMultiplier;
                                float f76 = this.maxYawSpeed;
                                int i32 = this.aimPointTicks;
                                f56 = ExactMath.add(ExactMath.multiply(f72, 1.5F), 0.8F);
                                float f77;
                                if (flag2) {
                                    this.lastTarget = livingEntity19;
                                    this.firstTick = flag15;
                                    this.tickCounter = i31;
                                    this.targetYaw = f73;
                                    this.aimPoint = floats30;
                                    this.lastYawDelta = f74;
                                    this.aimPointTicks = i32;
                                    float f78 = ExactMath.add(ExactMath.multiply(f75, f56), f37);
                                    boolean flag16 = RaycastUtil.isLookingAtBox(box, f78, f3, 6.0F);
                                    float f79 = ExactMath.add(ExactMath.subtract(f55, f56), -1.7F);
                                    float f80 = ExactMath.multiply(0.2F, f55);
                                    float f81 = Math.max(f79, f80);
                                    livingEntity17 = this.lastTarget;
                                    boolean flag17 = this.firstTick;
                                    int i33 = this.tickCounter;
                                    float f82 = this.targetYaw;
                                    floats28 = this.aimPoint;
                                    float f83 = this.lastYawDelta;
                                    boolean flag18 = this.overshooting;
                                    f70 = this.accelerationLimit;
                                    float f84 = this.maxPitchSpeed;
                                    int i34 = this.aimPointTicks;
                                    if (flag16) {
                                        flag13 = flag17;
                                        i29 = i33;
                                        f65 = f82;
                                        i30 = i34;
                                        f66 = f83;
                                        f67 = f70;
                                        f68 = f37;
                                        break label152;
                                    }

                                    flag14 = flag18;
                                    f77 = f81;
                                    f71 = f84;
                                    flag13 = flag17;
                                    i29 = i33;
                                    f65 = f82;
                                    i30 = i34;
                                    f66 = f83;
                                    livingEntity18 = livingEntity17;
                                    floats29 = floats28;
                                    if ((1 & b) == 0) {
                                        break label167;
                                    }
                                } else {
                                    this.lastTarget = livingEntity19;
                                    this.firstTick = flag15;
                                    this.tickCounter = i31;
                                    this.targetYaw = f73;
                                    this.aimPoint = floats30;
                                    this.lastYawDelta = f74;
                                    this.aimPointTicks = i32;
                                    float f85 = Math.min(ExactMath.add(f76, f55), 45.0F);
                                    LivingEntity livingEntity20 = this.lastTarget;
                                    boolean flag19 = this.firstTick;
                                    i29 = this.tickCounter;
                                    float f86 = this.targetYaw;
                                    float[] floats31 = this.aimPoint;
                                    float f87 = this.lastYawDelta;
                                    boolean flag20 = this.overshooting;
                                    f70 = this.accelerationLimit;
                                    float f88 = this.maxPitchSpeed;
                                    i30 = this.aimPointTicks;
                                    flag14 = flag20;
                                    f77 = f85;
                                    f71 = f88;
                                    flag13 = flag19;
                                    f65 = f86;
                                    f66 = f87;
                                    livingEntity18 = livingEntity20;
                                    floats29 = floats31;
                                    if ((1 & b) == 0) {
                                        break label167;
                                    }
                                }

                                float f89 = Array.getFloat(floats, 0);
                                float f90 = this.speedMultiplier;
                                float f91 = Array.getFloat(floats15, 0);
                                livingEntity17 = this.lastTarget;
                                boolean flag21 = this.firstTick;
                                int i35 = this.tickCounter;
                                float f92 = this.targetYaw;
                                floats28 = this.aimPoint;
                                float f93 = this.lastYawDelta;
                                boolean flag22 = this.overshooting;
                                float f94 = this.accelerationLimit;
                                float f95 = this.maxPitchSpeed;
                                int i36 = this.aimPointTicks;
                                float f96 = ExactMath.add(ExactMath.add(ExactMath.multiply(f77, f90), f89), f91);
                                f67 = f94;
                                f68 = f96;
                                f69 = f95;
                                flag13 = flag21;
                                i29 = i35;
                                f65 = f92;
                                i30 = i36;
                                f66 = f93;
                                if (flag2) {
                                    break label152;
                                }

                                if (flag22) {
                                    break label151;
                                }
                                break label166;
                            }

                            f67 = f70;
                            f68 = f37;
                            f69 = f71;
                            livingEntity17 = livingEntity18;
                            floats28 = floats29;
                            if (flag2) {
                                break label152;
                            }

                            if (flag14) {
                                break label151;
                            }
                        }

                        this.lastTarget = livingEntity17;
                        this.firstTick = flag13;
                        this.tickCounter = i29;
                        this.targetYaw = f65;
                        this.aimPoint = floats28;
                        this.lastYawDelta = f66;
                        this.aimPointTicks = i30;
                        float f97 = MathHelper.wrapDegrees(ExactMath.subtract(f68, f37));
                        long l2 = AttackTimer.getCooldownMs();
                        float f98 = AttackTimer.getElapsedMs();
                        LivingEntity livingEntity21 = this.lastTarget;
                        flag12 = this.firstTick;
                        i26 = this.tickCounter;
                        float f99 = this.targetYaw;
                        float[] floats32 = this.aimPoint;
                        float f100 = this.lastYawDelta;
                        int i37 = this.aimPointTicks;
                        float f101 = ExactMath.subtract((float)l2, f98);
                        float f102 = ExactMath.add(ExactMath.multiply(500.0F, f), f101);
                        int i38 = Float.floatToRawIntBits(f69);
                        f61 = f102;
                        i27 = i38;
                        f62 = f99;
                        i28 = i37;
                        f63 = f97;
                        f64 = f100;
                        livingEntity16 = livingEntity21;
                        floats27 = floats32;
                        break label165;
                    }

                    this.lastTarget = livingEntity17;
                    this.firstTick = flag13;
                    this.tickCounter = i29;
                    this.targetYaw = f65;
                    this.aimPoint = floats28;
                    this.lastYawDelta = f66;
                    this.aimPointTicks = i30;
                    float f103 = MathHelper.wrapDegrees(ExactMath.subtract(f68, f37));
                    long l3 = AttackTimer.getCooldownMs();
                    float f104 = AttackTimer.getElapsedMs();
                    LivingEntity livingEntity22 = this.lastTarget;
                    flag12 = this.firstTick;
                    i26 = this.tickCounter;
                    float f105 = this.targetYaw;
                    float[] floats33 = this.aimPoint;
                    float f106 = this.lastYawDelta;
                    int i39 = this.aimPointTicks;
                    float f107 = ExactMath.subtract((float)l3, f104);
                    float f108 = ExactMath.add(ExactMath.multiply(500.0F, f), f107);
                    i27 = Float.floatToRawIntBits(f67);
                    f61 = f108;
                    f62 = f105;
                    i28 = i39;
                    f63 = f103;
                    f64 = f106;
                    livingEntity16 = livingEntity22;
                    floats27 = floats33;
                    break label165;
                }

                float f109;
                int i40;
                boolean flag23;
                float f110;
                int i41;
                float f111;
                float[][] floatses;
                float f112;
                label130: {
                    this.lastTarget = livingEntity17;
                    this.firstTick = flag13;
                    this.tickCounter = i29;
                    this.targetYaw = f65;
                    this.aimPoint = floats28;
                    this.lastYawDelta = f66;
                    this.aimPointTicks = i30;
                    f109 = MathHelper.wrapDegrees(ExactMath.subtract(f68, f37));
                    long l4 = AttackTimer.getCooldownMs();
                    float f113 = AttackTimer.getElapsedMs();
                    LivingEntity livingEntity23 = this.lastTarget;
                    boolean flag24 = this.firstTick;
                    i40 = this.tickCounter;
                    float f114 = this.targetYaw;
                    float[] floats34 = this.aimPoint;
                    float f115 = this.lastYawDelta;
                    int i42 = this.aimPointTicks;
                    float[][] floatses2 = SPEED_PROFILES;
                    float f116 = ExactMath.subtract((float)l4, f113);
                    float f117 = ExactMath.add(ExactMath.multiply(500.0F, f), f116);
                    i23 = Float.floatToRawIntBits(f67);
                    flag23 = flag24;
                    f110 = f114;
                    i41 = i42;
                    f111 = f115;
                    livingEntity15 = livingEntity23;
                    floats26 = floats34;
                    floatses = floatses2;
                    f112 = ExactMath.divide(ExactMath.subtract(150.0F, f117), 127.0F);
                    if (0.2F > f112) {
                        this.lastTarget = livingEntity23;
                        this.firstTick = flag24;
                        this.tickCounter = i40;
                        this.targetYaw = f114;
                        this.aimPoint = floats34;
                        this.lastYawDelta = f115;
                        this.aimPointTicks = i42;
                        float f118 = Math.abs(f109);
                        LivingEntity livingEntity24 = this.lastTarget;
                        boolean flag25 = this.firstTick;
                        int i43 = this.tickCounter;
                        float f119 = this.targetYaw;
                        floats26 = this.aimPoint;
                        float f120 = this.lastYawDelta;
                        int i44 = this.aimPointTicks;
                        float[][] floatses3 = SPEED_PROFILES;
                        if (f118 > 35.0F) {
                            i41 = 0;
                            flag23 = flag25;
                            i40 = i43;
                            f110 = f119;
                            f111 = f120;
                            livingEntity15 = livingEntity24;
                            floatses = floatses3;
                            if (!flag7) {
                                flag11 = flag25;
                                f57 = f120;
                                f58 = f109;
                                f59 = f119;
                                i24 = i43;
                                f60 = 1.0F;
                                i25 = 0;
                                break label154;
                            }
                            break label130;
                        }

                        flag23 = flag25;
                        i40 = i43;
                        f110 = f119;
                        i41 = i44;
                        f111 = f120;
                        livingEntity15 = livingEntity24;
                        floatses = floatses3;
                        if (flag7) {
                            break label130;
                        }
                    } else if (flag7) {
                        break label130;
                    }

                    flag11 = flag23;
                    f57 = f111;
                    f58 = f109;
                    f59 = f110;
                    i24 = i40;
                    f60 = 1.0F;
                    i25 = i41;
                    break label154;
                }

                this.lastTarget = livingEntity15;
                this.firstTick = flag23;
                this.tickCounter = i40;
                this.targetYaw = f110;
                this.aimPoint = floats26;
                this.lastYawDelta = f111;
                this.aimPointTicks = i41;
                Object object = Array.get(floatses, i41);
                float f121 = evaluatePattern((float[])object, f112);
                LivingEntity livingEntity25 = this.lastTarget;
                flag11 = this.firstTick;
                i24 = this.tickCounter;
                float f122 = this.targetYaw;
                float[] floats35 = this.aimPoint;
                float f123 = this.lastYawDelta;
                i25 = this.aimPointTicks;
                f59 = f122;
                f60 = f121;
                f57 = f123;
                f58 = f109;
                livingEntity15 = livingEntity25;
                floats26 = floats35;
                break label154;
            }

            this.lastTarget = livingEntity16;
            this.firstTick = flag12;
            this.tickCounter = i26;
            this.targetYaw = f62;
            this.aimPoint = floats27;
            this.lastYawDelta = f64;
            this.aimPointTicks = i28;
            long l5 = AttackTimer.getCooldownMs();
            LivingEntity livingEntity26 = this.lastTarget;
            flag11 = this.firstTick;
            int i45 = this.tickCounter;
            float f124 = this.targetYaw;
            float[] floats36 = this.aimPoint;
            f57 = this.lastYawDelta;
            int i46 = this.aimPointTicks;
            float[][] floatses4 = SPEED_PROFILES;
            int i47 = this.stallTicks;
            float f125 = ExactMath.add((float)l5, -150.0F);
            float f126 = s;
            float f127 = ExactMath.subtract(f125, f126);
            float f128 = ExactMath.divide(ExactMath.add(f61, -150.0F), f127);
            float f129 = ExactMath.subtract(1.0F, f128);
            if (!flag7) {
                f58 = f63;
                f59 = f124;
                livingEntity15 = livingEntity26;
                floats26 = floats36;
                i24 = i45;
                f60 = 1.0F;
                i25 = i46;
                i23 = i27;
            } else {
                i23 = i27;
                this.lastTarget = livingEntity26;
                this.firstTick = flag11;
                this.tickCounter = i45;
                this.targetYaw = f124;
                this.aimPoint = floats36;
                this.lastYawDelta = f57;
                this.aimPointTicks = i46;
                Object object2 = Array.get(floatses4, i47);
                float f130 = evaluatePattern((float[])object2, f129);
                LivingEntity livingEntity27 = this.lastTarget;
                flag11 = this.firstTick;
                i24 = this.tickCounter;
                float f131 = this.targetYaw;
                float[] floats37 = this.aimPoint;
                float f132 = this.lastYawDelta;
                i25 = this.aimPointTicks;
                f59 = f131;
                f60 = f130;
                f57 = f132;
                f58 = f63;
                livingEntity15 = livingEntity27;
                floats26 = floats37;
            }
        }

        float f133;
        float f134;
        boolean flag26;
        float f135;
        float f136;
        int i48;
        int i49;
        float f137;
        LivingEntity livingEntity28;
        float[] floats38;
        label120: {
            boolean flag27;
            float[] floats39;
            int i50;
            int i51;
            LivingEntity livingEntity29;
            float f138;
            float f139;
            label119: {
                this.lastTarget = livingEntity15;
                this.firstTick = flag11;
                this.tickCounter = i24;
                this.targetYaw = f59;
                this.aimPoint = floats26;
                this.lastYawDelta = f57;
                this.aimPointTicks = i25;
                float f140 = Float.intBitsToFloat(i23);
                f133 = ExactMath.multiply(f60, f);
                float f141 = this.sampleWave(0, f58, f140, f133);
                LivingEntity livingEntity30 = this.lastTarget;
                flag27 = this.firstTick;
                int i52 = this.tickCounter;
                float f142 = this.targetYaw;
                floats39 = this.aimPoint;
                float f143 = this.lastPitchDelta;
                float f144 = this.lastYawDelta;
                i50 = this.aimPointTicks;
                f134 = ExactMath.add(ExactMath.subtract(f37, f2), f141);
                float f145;
                if (b != 0) {
                    this.lastTarget = livingEntity30;
                    this.firstTick = flag27;
                    this.tickCounter = i52;
                    this.targetYaw = f142;
                    this.aimPoint = floats39;
                    this.lastYawDelta = f144;
                    this.aimPointTicks = i50;
                    float f146 = Array.getFloat(floats, 0);
                    float f147 = this.speedMultiplier;
                    float f148 = Math.max(ExactMath.subtract(f55, f56), 0.0F);
                    LivingEntity livingEntity31 = this.lastTarget;
                    flag27 = this.firstTick;
                    int i53 = this.tickCounter;
                    float f149 = this.targetYaw;
                    float[] floats40 = this.aimPoint;
                    float f150 = this.lastPitchDelta;
                    float f151 = this.lastYawDelta;
                    int i54 = this.aimPointTicks;
                    f2 = ExactMath.add(ExactMath.multiply(f147, f148), f146);
                    f145 = f150;
                    i51 = i53;
                    f138 = f149;
                    i50 = i54;
                    f139 = f151;
                    livingEntity29 = livingEntity31;
                    floats39 = floats40;
                    if (flag2) {
                        break label119;
                    }
                } else {
                    f145 = f143;
                    i51 = i52;
                    livingEntity29 = livingEntity30;
                    f138 = f142;
                    f139 = f144;
                    if (flag2) {
                        break label119;
                    }
                }

                this.lastTarget = livingEntity29;
                this.firstTick = flag27;
                this.tickCounter = i51;
                this.targetYaw = f138;
                this.aimPoint = floats39;
                this.lastYawDelta = f139;
                this.aimPointTicks = i50;
                float f152 = this.randomizeDelta(f145);
                LivingEntity livingEntity32 = this.lastTarget;
                flag26 = this.firstTick;
                int i55 = this.tickCounter;
                f135 = this.targetYaw;
                float[] floats41 = this.aimPoint;
                float f153 = this.lastYawDelta;
                int i56 = this.aimPointTicks;
                f136 = ExactMath.add(f139, f152);
                i48 = i55;
                i49 = i56;
                f137 = f153;
                livingEntity28 = livingEntity32;
                floats38 = floats41;
                break label120;
            }

            this.lastTarget = livingEntity29;
            this.firstTick = flag27;
            this.tickCounter = i51;
            this.targetYaw = f138;
            this.aimPoint = floats39;
            this.lastYawDelta = f139;
            this.aimPointTicks = i50;
            float f154 = this.computeTargetPitch(vec3d, livingEntity, f2);
            LivingEntity livingEntity33 = this.lastTarget;
            flag26 = this.firstTick;
            int i57 = this.tickCounter;
            float f155 = this.targetYaw;
            float[] floats42 = this.aimPoint;
            float f156 = this.lastYawDelta;
            int i58 = this.aimPointTicks;
            f136 = f154;
            i48 = i57;
            f135 = f155;
            i49 = i58;
            f137 = f156;
            livingEntity28 = livingEntity33;
            floats38 = floats42;
        }

        boolean flag28;
        float f157;
        int i59;
        float f158;
        int i60;
        float f159;
        LivingEntity livingEntity34;
        float[] floats43;
        boolean flag29;
        label112: {
            int i61;
            label111: {
                this.lastTarget = livingEntity28;
                this.firstTick = flag26;
                this.tickCounter = i48;
                this.targetYaw = f135;
                this.aimPoint = floats38;
                this.lastYawDelta = f137;
                this.aimPointTicks = i49;
                float f160 = Array.getFloat(floats15, 1);
                LivingEntity livingEntity35 = this.lastTarget;
                flag28 = this.firstTick;
                int i62 = this.tickCounter;
                float f161 = this.targetYaw;
                float[] floats44 = this.aimPoint;
                float f162 = this.lastYawDelta;
                int i63 = this.aimPointTicks;
                int i64 = this.patternIndex;
                int i65 = this.profileDuration;
                f157 = ExactMath.add(f136, f160);
                if (i64 > 0) {
                    if (!flag7) {
                        i59 = i62;
                        f158 = f161;
                        i60 = i63;
                        f159 = f162;
                        livingEntity34 = livingEntity35;
                        floats43 = floats44;
                        flag29 = true;
                        break label112;
                    }

                    i59 = i62;
                    f158 = f161;
                    i61 = i63;
                    f159 = f162;
                    livingEntity34 = livingEntity35;
                    floats43 = floats44;
                    if (i65 > 0) {
                        break label111;
                    }
                } else {
                    this.lastTarget = livingEntity35;
                    this.firstTick = flag28;
                    this.tickCounter = i62;
                    this.targetYaw = f161;
                    this.aimPoint = floats44;
                    this.lastYawDelta = f162;
                    this.aimPointTicks = i63;
                    float f163 = AttackTimer.getElapsedMs();
                    LivingEntity livingEntity36 = this.lastTarget;
                    flag28 = this.firstTick;
                    int i66 = this.tickCounter;
                    float f164 = this.targetYaw;
                    float[] floats45 = this.aimPoint;
                    float f165 = this.lastYawDelta;
                    int i67 = this.aimPointTicks;
                    int i68 = this.profileDuration;
                    if (!(300.0F > f163)) {
                        if (!flag7) {
                            i59 = i66;
                            f158 = f164;
                            i60 = i67;
                            f159 = f165;
                            livingEntity34 = livingEntity36;
                            floats43 = floats45;
                            flag29 = true;
                            break label112;
                        }

                        i59 = i66;
                        f158 = f164;
                        i61 = i67;
                        f159 = f165;
                        livingEntity34 = livingEntity36;
                        floats43 = floats45;
                        if (i68 <= 0) {
                            i60 = i67;
                            flag29 = true;
                            break label112;
                        }
                        break label111;
                    }

                    if (!flag7) {
                        i59 = i66;
                        f158 = f164;
                        i60 = i67;
                        f159 = f165;
                        livingEntity34 = livingEntity36;
                        floats43 = floats45;
                        flag29 = true;
                        break label112;
                    }

                    i59 = i66;
                    f158 = f164;
                    i61 = i67;
                    f159 = f165;
                    livingEntity34 = livingEntity36;
                    floats43 = floats45;
                    if (i68 > 0) {
                        break label111;
                    }
                }

                this.lastTarget = livingEntity34;
                this.firstTick = flag28;
                this.tickCounter = i59;
                this.targetYaw = f158;
                this.aimPoint = floats43;
                this.lastYawDelta = f159;
                this.aimPointTicks = i61;
                MinecraftClient minecraftClient4 = mc;
                ClientPlayerEntity clientPlayerEntity4 = minecraftClient4.player;
                boolean flag30 = clientPlayerEntity4.isGliding();
                LivingEntity livingEntity37 = this.lastTarget;
                flag28 = this.firstTick;
                int i69 = this.tickCounter;
                float f166 = this.targetYaw;
                float[] floats46 = this.aimPoint;
                float f167 = this.lastYawDelta;
                int i70 = this.aimPointTicks;
                if (!flag30) {
                    MinecraftClient minecraftClient5 = mc;
                    ClientPlayerEntity clientPlayerEntity5 = minecraftClient5.player;
                    boolean flag31 = clientPlayerEntity5.isSwimming();
                    LivingEntity livingEntity38 = this.lastTarget;
                    flag28 = this.firstTick;
                    int i71 = this.tickCounter;
                    float f168 = this.targetYaw;
                    float[] floats47 = this.aimPoint;
                    float f169 = this.lastYawDelta;
                    int i72 = this.aimPointTicks;
                    flag29 = flag31;
                    i59 = i71;
                    f158 = f168;
                    i60 = i72;
                    f159 = f169;
                    livingEntity34 = livingEntity38;
                    floats43 = floats47;
                } else {
                    i59 = i69;
                    f158 = f166;
                    i60 = i70;
                    f159 = f167;
                    livingEntity34 = livingEntity37;
                    floats43 = floats46;
                    flag29 = true;
                }
                break label112;
            }

            i60 = i61;
            flag29 = true;
        }

        this.lastTarget = livingEntity34;
        this.firstTick = flag28;
        this.tickCounter = i59;
        this.targetYaw = f158;
        this.aimPoint = floats43;
        this.lastYawDelta = f159;
        this.aimPointTicks = i60;
        float f170 = ExactMath.subtract(f157, f3);
        float f171 = Float.intBitsToFloat(i23);
        float f172 = this.sampleWave(1, f170, f171, f133);
        float f173 = ExactMath.multiply(f, -600.0F);
        float f174 = ExactMath.multiply(f, 600.0F);
        float f175 = MathHelper.clamp(f172, f173, f174);
        float f176 = Array.getFloat(floats11, 0);
        LivingEntity livingEntity39 = this.lastTarget;
        boolean flag32 = this.firstTick;
        int i73 = this.tickCounter;
        float f177 = this.targetYaw;
        float[] floats48 = this.aimPoint;
        float f178 = this.lastYawDelta;
        int i74 = this.aimPointTicks;
        float f179 = ExactMath.add(f134, f176);
        boolean flag33;
        float f180;
        int i75;
        float f181;
        int i76;
        float f182;
        LivingEntity livingEntity40;
        float[] floats49;
        if (flag29) {
            this.lastTarget = livingEntity39;
            this.firstTick = flag32;
            this.tickCounter = i73;
            this.targetYaw = f177;
            this.aimPoint = floats48;
            this.lastYawDelta = f178;
            this.aimPointTicks = i74;
            float f183 = Array.getFloat(floats11, 1);
            LivingEntity livingEntity41 = this.lastTarget;
            flag33 = this.firstTick;
            int i77 = this.tickCounter;
            f181 = this.targetYaw;
            float[] floats50 = this.aimPoint;
            float f184 = this.lastYawDelta;
            int i78 = this.aimPointTicks;
            f180 = ExactMath.add(f175, f183);
            i75 = i77;
            i76 = i78;
            f182 = f184;
            livingEntity40 = livingEntity41;
            floats49 = floats50;
        } else {
            this.lastTarget = livingEntity39;
            this.firstTick = flag32;
            this.tickCounter = i73;
            this.targetYaw = f177;
            this.aimPoint = floats48;
            this.lastYawDelta = f178;
            this.aimPointTicks = i74;
            float f185 = this.computeSpeedFactor(f170);
            LivingEntity livingEntity42 = this.lastTarget;
            flag33 = this.firstTick;
            int i79 = this.tickCounter;
            float f186 = this.targetYaw;
            float[] floats51 = this.aimPoint;
            float f187 = this.lastYawDelta;
            int i80 = this.aimPointTicks;
            f180 = f185;
            i75 = i79;
            f181 = f186;
            i76 = i80;
            f182 = f187;
            livingEntity40 = livingEntity42;
            floats49 = floats51;
        }

        this.lastTarget = livingEntity40;
        this.firstTick = flag33;
        this.tickCounter = i75;
        this.targetYaw = f181;
        this.aimPoint = floats49;
        this.lastYawDelta = f182;
        this.aimPointTicks = i76;
        this.applyRotation(f179, f180, f);
    }

    @Override
    public void reset() {
        this.lastTarget = null;
        this.frameTimer.reset();
    }

    @Override
    public void onStop() {
        this.settled = ServerManager.isIntersecting(this.feature.getCurrentTarget(), 0.1F);
        this.firstTick = true;
        this.tickCounter++;
        this.patternIndex = 0;
        this.profileDuration--;
        if (--this.pauseInterval <= 0) {
            this.pauseTicks = (this.pauseTicks + CombatUtil.randomInt(1, WAVE_PATTERNS.length)) % WAVE_PATTERNS.length;
            this.pauseInterval = CombatUtil.randomInt(2, 5);
            this.minSmoothing = CombatUtil.randomLerp(1.3F, 2.2F);
            this.maxSmoothing = CombatUtil.randomLerp(2.5F, 4.5F);
            this.aimPoint = this.computeAimOffset(0.0F);
        }

        this.maxYawSpeed = CombatUtil.randomLerp(20.0F, 30.0F);
        this.maxPitchSpeed = CombatUtil.randomLerp(10.0F, 13.0F);
        this.accelerationLimit = CombatUtil.randomLerp(36.0F, 44.0F);
        this.stallTicks = (this.stallTicks + CombatUtil.randomInt(1, SPEED_PROFILES.length)) % SPEED_PROFILES.length;
        this.aimPointTicks = (this.aimPointTicks + CombatUtil.randomInt(1, SPEED_PROFILES.length)) % SPEED_PROFILES.length;
    }

    private void updateProfile(float f) {
        float f2 = f == 0.0F ? this.speedMultiplier : Math.signum(f);
        if (this.overshooting && f2 == this.speedMultiplier) {
            this.overshooting = false;
        }

        if (this.overshooting) {
            this.firstTick = false;
        }

        boolean flag = !this.overshooting && this.speedMultiplier * f < -2.0F;
        if (this.firstTick || flag) {
            boolean flag2 = this.firstTick && this.tickCounter >= this.profileIndex;
            float f3 = flag2 ? -this.speedMultiplier : f2;
            this.firstTick = false;
            if (f3 != this.speedMultiplier) {
                this.overshooting = f3 != f2;
                this.speedMultiplier = f3;
                this.tickCounter = 0;
                this.profileIndex = CombatUtil.randomInt(1, 4);
            }
        }
    }

    private float computeSpeedFactor(float f) {
        if (this.patternIndex == 0 || mc.player.age != this.patternTicks) {
            this.patternIndex++;
        }

        this.patternTicks = mc.player.age;
        this.targetOffset[1] = 0.0F;
        if (this.patternIndex < 3) {
            return this.patternIndex == 1 ? -200.0F : 200.0F;
        }

        this.profileDuration = CombatUtil.randomInt(10, 16);
        this.patternIndex = 0;
        return f;
    }

    private void applyRotation(float f, float f2, float f3) {
        if (mc.player.age != this.jitterTicks) {
            this.wavePhase = 0.0F;
        }

        this.jitterTicks = mc.player.age;
        float f4 = MathHelper.clamp(f, Math.max(-600.0F * f3, -40.0F - this.wavePhase), Math.min(600.0F * f3, 40.0F - this.wavePhase));
        this.wavePhase += f4;
        float f5 = f4 + this.currentOffset[0];
        float f6 = f2 + this.currentOffset[1];
        float f7 = SensitivityUtil.snapDelta(f5);
        float f8 = SensitivityUtil.snapDelta(f6);
        this.currentOffset[0] = f5 - f7;
        this.currentOffset[1] = f6 - f8;
        this.lastPitchDelta = MathHelper.wrapDegrees(this.lastPitchDelta + f7);
        RotationComponent.rotate(
            new Rotation(mc.player.getYaw() + f7, mc.player.getPitch() + f8),
            Math.abs(f7) + 0.1F,
            Math.abs(f8) + 0.1F,
            CombatUtil.randomLerp(10.0F, 15.0F),
            CombatUtil.randomLerp(5.0F, 6.0F),
            1,
            4,
            false
        );
    }

    private float sampleWave(int i, float f, float f2, float f3) {
        float f4 = -f;
        float f5 = (this.targetOffset[i] + f2 * f4) * f3;
        float f6 = (float)Math.exp(-f2 * f3);
        this.targetOffset[i] = MathHelper.clamp((this.targetOffset[i] - f2 * f5) * f6, -600.0F, 600.0F);
        return MathHelper.clamp((f4 + f5) * f6 - f4, -600.0F * f3, 600.0F * f3);
    }

    private static float evaluatePattern(float[] floats, float f) {
        if (f >= 1.0F) {
            return 1.0F;
        }

        float f2 = Math.max(f, 0.0F);
        double d = Math.pow(f2, floats[0] - 1.0F);
        return (float)(floats[0] * floats[1] * d * Math.pow(1.0 - d * f2, floats[1] - 1.0F));
    }

    private static float distanceToBox(Vec3d vec3d, LivingEntity livingEntity) {
        double d = livingEntity.getX() - vec3d.x;
        double d2 = livingEntity.getZ() - vec3d.z;
        double d3 = Math.max(Math.hypot(d, d2), 0.3);
        double d4 = Math.abs((livingEntity.getX() - livingEntity.lastX) * d2 - (livingEntity.getZ() - livingEntity.lastZ) * d) / d3;
        return (float)Math.toDegrees(Math.atan2(d4, d3));
    }

    private float[] computeAimOffset(float f) {
        float[] floats = WAVE_PATTERNS[this.pauseTicks];
        this.lastDistance = this.lastDistance + (float) (Math.PI * 2) * this.minSmoothing / floats[0] * f;
        double d = this.maxSmoothing * (0.75 + 0.25 * Math.sin(this.lastDistance * 0.23));
        return new float[]{(float)(d * Math.sin(this.lastDistance)), (float)(d * floats[2] * Math.sin(floats[0] * this.lastDistance + floats[1]))};
    }

    private float computeTargetPitch(Vec3d vec3d, LivingEntity livingEntity, float f) {
        Box box = livingEntity.getBoundingBox();
        double d = 1.5 * (livingEntity.getY() - livingEntity.lastY);
        double d2 = Math.min(box.minY + 0.15 + Math.max(d, 0.0), box.maxY - 0.15);
        double d3 = Math.max(box.maxY - 0.15 + Math.min(d, 0.0), d2);
        double d4 = -Math.sin(Math.toRadians(f));
        double d5 = Math.cos(Math.toRadians(f));
        double d6 = (box.minX - vec3d.x) / d4;
        double d7 = (box.maxX - vec3d.x) / d4;
        double d8 = (box.minZ - vec3d.z) / d5;
        double d9 = (box.maxZ - vec3d.z) / d5;
        double d10 = Math.max(Math.max(Math.min(d6, d7), Math.min(d8, d9)), 0.0);
        double d11 = Math.min(Math.max(d6, d7), Math.max(d8, d9));
        if (!(d11 >= d10)) {
            d10 = d11 = Math.hypot(box.getCenter().x - vec3d.x, box.getCenter().z - vec3d.z);
        }

        float f2 = lerpNoise(vec3d.y - d3, vec3d.y > d3 ? d11 : d10);
        float f3 = lerpNoise(vec3d.y - d2, vec3d.y > d2 ? d10 : d11);
        double d12 = this.feature.getAttackRange() - 0.05;
        float f4 = (float)Math.toDegrees(Math.acos(Math.min(d10 / d12, 1.0)));
        float f5 = vec3d.y > box.maxY ? (float)Math.toDegrees(Math.asin(Math.min((vec3d.y - box.maxY) / d12, 1.0))) : -f4;
        float f6 = Math.max(f2, f5);
        float f7 = (float)Math.toDegrees(Math.acos(Math.min(d10 / (d12 - 0.3), 1.0)));
        float f8 = lerpNoise(vec3d.y - MathHelper.clamp(vec3d.y, d2, d3), d10);
        float f9 = Math.max(Math.min(Math.min(f3, 60.0F), Math.max(f7, Math.min(f8, f4))), f6);
        float f10 = Math.min(6.0F, (f9 - f6) / 2.0F);
        this.lastYawDelta = MathHelper.clamp(this.lastYawDelta, Math.max(f9 - f10 - Math.max(f3 - f6, 0.0F) * 0.1F, f6), f9 - f10);
        return MathHelper.clamp(this.lastYawDelta + this.randomizeDelta(this.lastPitchDelta), f6, f9);
    }

    private static float lerpNoise(double d, double d2) {
        return (float)Math.toDegrees(Math.atan2(d, d2));
    }

    private float randomizeDelta(float f) {
        if (!this.feature.getPitchWave().getValue()) {
            return 0.0F;
        }

        WaveSetting waveSetting = this.feature.getPitchWaveShape();
        return waveSetting.getValue().getMaxAmplitude() > 0.0F
            ? waveSetting.getOffset(f)
            : 6.0F * MathHelper.sin(f * (float) (Math.PI * 2) / 60.0F) + 2.0F * MathHelper.sin(f * (float) (Math.PI * 2) / 18.0F + 1.3F);
    }

    private static float[] getRotationToBox(Vec3d vec3d, Box box, float f) {
        float[] floats = new float[2];

        for (double d : new double[]{box.minX, box.maxX}) {
            for (double d2 : new double[]{box.minZ, box.maxZ}) {
                float f2 = MathHelper.wrapDegrees(RotationUtil.getRotationArray(vec3d, new Vec3d(d, vec3d.y, d2))[0] - f);
                floats[0] = Math.min(floats[0], f2);
                floats[1] = Math.max(floats[1], f2);
            }
        }

        return floats;
    }
}
