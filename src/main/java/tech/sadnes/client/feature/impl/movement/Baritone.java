package tech.sadnes.client.feature.impl.movement;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.Settings.Setting;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.TickEvent.Type;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.movement.IMovement;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IInputOverrideHandler;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.pathing.movement.movements.MovementDiagonal;
import baritone.pathing.movement.movements.MovementTraverse;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.misc.AutoBuild;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.player.CombatUtil;

@FeatureInfo(name = "Baritone", displayName = "feature.impl.misc.baritone", category = Category.MOVEMENT, subcategory = Subcategory.MOVEMENT_ASSIST)
public final class Baritone extends Feature {
    private final TextSetting prefix = new TextSetting("baritone.prefix", "#", "#");
    final BooleanSetting improved = new BooleanSetting("baritone.improved", false);
    private static boolean listenerRegistered;
    private boolean allowPlace;
    private boolean freeLooking;
    private float yaw;
    private float pitch;
    private float prevYaw;
    private float prevPitch;
    int lastStrafeCorrectionTick = -1;
    int nextStrafeTick;
    int strafeDirection;
    int strafeHoldUntilTick;
    IPathExecutor jumpExecutor;
    int jumpTick = -1000;
    private float jumpYaw;
    private boolean jumpSprinting;
    private int jumpY;
    private UUID rotationProfileId;
    private Easing rotationEasing;
    float yawSpeed;
    float pitchSpeed;
    int lastSpeedUpdateTick = -1;

    public static Baritone getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Baritone.class);
    }

    @Override
    public void initialize() {
        super.initialize();
        this.registerListener();
        BaritoneAPI.getSettings().prefix.value = this.getPrefix();
        BaritoneAPI.getSettings().prefixControl.value = this.isEnabled();
        BaritoneAPI.getSettings().chatControl.value = false;
        BaritoneAPI.getSettings().chatControlAnyway.value = false;
        BaritoneAPI.getSettings().doDeathWaypoints.value = this.isEnabled();
        this.applySettingRestrictions();
    }

    void applySettingRestrictions() {
        Settings settings = BaritoneAPI.getSettings();
        if (!this.allowPlace) {
            settings.allowPlace.value = false;
        }

        settings.allowParkourPlace.value = false;
        settings.allowWaterBucketFall.value = false;
    }

    @Override
    public void onEnable() {
        this.jumpExecutor = null;
        this.jumpTick = -1000;
        this.lastStrafeCorrectionTick = -1;
        this.nextStrafeTick = 0;
        this.strafeDirection = 0;
        this.strafeHoldUntilTick = 0;
        this.lastSpeedUpdateTick = -1;
        this.yawSpeed = 0.0F;
        this.pitchSpeed = 0.0F;
        IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (iBaritone != null) {
            iBaritone.getPathingBehavior().cancelEverything();
        }

        super.onEnable();
    }

    void updateRotations(IBaritone iBaritone) {
        Rotation rotation = iBaritone.getPlayerContext().playerRotations();
        boolean flag = this.freeLooking;
        this.freeLooking = this.isEnabled() && BaritoneAPI.getSettings().freeLook.value && iBaritone.getPathingBehavior().isPathing();
        this.prevYaw = flag ? this.yaw : rotation.getYaw();
        this.prevPitch = flag ? this.pitch : rotation.getPitch();
        this.yaw = rotation.getYaw();
        this.pitch = rotation.getPitch();
    }

    public boolean isFreeLooking() {
        return this.freeLooking && this.isEnabled();
    }

    public float getInterpolatedYaw(float f) {
        return MathHelper.lerpAngleDegrees(f, this.prevYaw, this.yaw);
    }

    public float getInterpolatedPitch(float f) {
        return MathHelper.lerp(f, this.prevPitch, this.pitch);
    }

    String getPrefix() {
        String string = this.prefix.getValue();
        return string != null && !string.isEmpty() ? string : "#";
    }

    public Rotation modifyRotation(IBaritone iBaritone, Rotation rotation, boolean flag) {
        if (!this.isEnabled()
            || !this.improved.getValue()
            || mc.player == null
            || mc.player.isGliding()
            || iBaritone != BaritoneAPI.getProvider().getPrimaryBaritone()) {
            return rotation;
        }

        if (flag) {
            AutoBuild autoBuild = AutoBuild.getInstance();
            if (autoBuild != null && autoBuild.isBuilding()) {
                return rotation;
            }

            RotationComponent.rotate(
                new tech.sadnes.client.util.math.Rotation(rotation.getYaw(), rotation.getPitch()),
                CombatUtil.randomLerp(21.0F, 33.0F),
                CombatUtil.randomLerp(21.0F, 33.0F),
                4.0F,
                4.0F,
                8,
                30,
                false
            );
            return new Rotation(mc.player.getYaw(), mc.player.getPitch());
        } else {
            if (!iBaritone.getPathingBehavior().isPathing()) {
                return rotation;
            }

            UUID uuid = mc.getSession().getUuidOrNull();
            if (uuid == null) {
                uuid = mc.player.getUuid();
            }

            if (!uuid.equals(this.rotationProfileId)) {
                this.rotationProfileId = uuid;

                this.rotationEasing = switch (new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits()).nextInt(4)) {
                    case 0 -> Easings.SINE_IN_OUT;
                    case 1 -> Easings.QUAD_IN_OUT;
                    case 2 -> Easings.CUBIC_IN_OUT;
                    default -> Easings.SINE_OUT;
                };
                this.yawSpeed = 0.0F;
                this.pitchSpeed = 0.0F;
                this.lastSpeedUpdateTick = -1;
            }

            float f = iBaritone.getPlayerContext().playerRotations().getYaw();
            float f2 = MathHelper.wrapDegrees(rotation.getYaw() - f);
            float f3 = iBaritone.getPlayerContext().playerRotations().getPitch();
            float f4 = MathHelper.clamp(rotation.getPitch() - f3, -180.0F, 180.0F);
            if (this.lastSpeedUpdateTick != mc.player.age) {
                double d = MathHelper.clamp(Math.abs(f2) / 100.0, 0.0, 1.0);
                double d2 = MathHelper.clamp(Math.abs(f4) / 90.0, 0.0, 1.0);
                float f5 = (float)((6.0 + 50.0 * this.rotationEasing.ease(d)) * CombatUtil.randomLerp(1.5F, 3.0F) * 3.0);
                float f6 = (float)((6.0 + 50.0 * this.rotationEasing.ease(d2)) * CombatUtil.randomLerp(1.5F, 3.0F) * 3.0);
                this.yawSpeed = this.yawSpeed + (f5 - this.yawSpeed) * 0.8F;
                this.pitchSpeed = this.pitchSpeed + (f6 - this.pitchSpeed) * 0.8F;
                this.lastSpeedUpdateTick = mc.player.age;
            }

            float f7 = Math.signum(f2) * Math.min(Math.abs(f2), this.yawSpeed);
            float f8 = Math.signum(f4) * Math.min(Math.abs(f4), this.pitchSpeed);
            return new Rotation(f + f7, f3 + f8);
        }
    }

    public void modifyMovement(IBaritone iBaritone, Movement movement, MovementState movementState) {
        if (this.isEnabled() && this.improved.getValue() && mc.player != null && mc.world != null) {
            if (iBaritone == BaritoneAPI.getProvider().getPrimaryBaritone() && iBaritone.getPathingBehavior().isPathing()) {
                if ((movement instanceof MovementTraverse || movement instanceof MovementDiagonal) && movementState.getStatus() == MovementStatus.RUNNING) {
                    if (!mc.player.isInLava() && !mc.player.isClimbing() && !mc.player.isGliding() && !mc.player.hasVehicle()) {
                        MovementTarget movementTarget = movementState.getTarget();
                        Map<Input, Boolean> map = movementState.getInputStates();
                        if (movementTarget.rotation != null && !movementTarget.hasToForceRotations()) {
                            if (!map.getOrDefault(Input.MOVE_BACK, false)
                                && !map.getOrDefault(Input.SNEAK, false)
                                && !map.getOrDefault(Input.CLICK_LEFT, false)
                                && !map.getOrDefault(Input.CLICK_RIGHT, false)) {
                                IPathExecutor iPathExecutor = iBaritone.getPathingBehavior().getCurrent();
                                if (iPathExecutor != null) {
                                    if (map.getOrDefault(Input.MOVE_FORWARD, false)) {
                                        List list = iPathExecutor.getPath().movements();
                                        int i = iPathExecutor.getPosition();
                                        int i2 = list.size();
                                        if (i >= 0 && i < i2 && list.get(i) == movement) {
                                            BetterBlockPos betterBlockPos = movement.getDest();
                                            if (movement.getSrc().getY() == betterBlockPos.getY()) {
                                                Vec3d vec3d = mc.player.getEntityPos();
                                                double d = betterBlockPos.getX() + 0.5;
                                                double d2 = betterBlockPos.getZ() + 0.5;
                                                boolean flag = mc.player.isOnGround() && Math.abs(vec3d.y - betterBlockPos.getY()) < 0.05;
                                                int i3 = betterBlockPos.getX() - movement.getSrc().getX();
                                                int i4 = betterBlockPos.getZ() - movement.getSrc().getZ();
                                                boolean flag2 = false;
                                                if (flag && i + 1 < i2) {
                                                    IMovement iMovement = (IMovement)list.get(i + 1);
                                                    if ((iMovement instanceof MovementTraverse || iMovement instanceof MovementDiagonal)
                                                        && iMovement.getDest().getY() == betterBlockPos.getY()) {
                                                        int i5 = iMovement.getDest().getX() - betterBlockPos.getX();
                                                        int i6 = iMovement.getDest().getZ() - betterBlockPos.getZ();
                                                        flag2 = i5 == i3 && i6 == i4;
                                                        double d3 = Math.hypot(i5, i6);
                                                        if (!flag2 && d3 > 0.0) {
                                                            double d4 = d + i5 / d3 * 0.28;
                                                            double d5 = d2 + i6 / d3 * 0.28;
                                                            if (this.isPathClear(d4 - vec3d.x, d5 - vec3d.z, 0.0)) {
                                                                d = d4;
                                                                d2 = d5;
                                                            }
                                                        }
                                                    }
                                                }

                                                float f = movementTarget.rotation.getYaw();
                                                if (flag && this.isPathClear(d - vec3d.x, d2 - vec3d.z, 0.0)) {
                                                    f = (float)Math.toDegrees(Math.atan2(d2 - vec3d.z, d - vec3d.x)) - 90.0F;
                                                }

                                                if (flag2
                                                    && Math.abs((vec3d.x - movement.getSrc().getX() - 0.5) * i4 - (vec3d.z - movement.getSrc().getZ() - 0.5) * i3)
                                                        < 0.2 * Math.hypot(i3, i4)) {
                                                    f = (float)Math.toDegrees(Math.atan2(i4, i3)) - 90.0F;
                                                }

                                                float f2 = iBaritone.getPlayerContext().playerRotations().getYaw();
                                                float f3 = f2 + MathHelper.wrapDegrees(f - f2);
                                                float f4 = MathHelper.wrapDegrees(f3 - f2);
                                                double d6 = Math.hypot(i3, i4);
                                                double d7 = i3 / Math.max(d6, 1.0);
                                                double d8 = i4 / Math.max(d6, 1.0);
                                                float f5 = (float)Math.toDegrees(Math.atan2(i4, i3)) - 90.0F;
                                                double d9 = ((vec3d.x - movement.getSrc().getX() - 0.5) * i4 - (vec3d.z - movement.getSrc().getZ() - 0.5) * i3)
                                                    / Math.max(d6, 1.0);
                                                double d10 = Math.toRadians(f3);
                                                double d11 = -Math.sin(d10);
                                                double d12 = Math.cos(d10);
                                                double d13 = mc.player.getVelocity().x * d12 - mc.player.getVelocity().z * d11;
                                                int i7 = 0;
                                                if (flag && !flag2 && mc.player.age >= this.nextStrafeTick && !map.getOrDefault(Input.JUMP, false)) {
                                                    if (Math.abs(d9) > 0.28 && Math.abs(f4) > 10.0F && Math.abs(f4) < 30.0F) {
                                                        this.strafeDirection = f4 > 0.0F ? 1 : -1;
                                                        this.strafeHoldUntilTick = mc.player.age + 2;
                                                    } else if (Math.abs(d9) < 0.12 && mc.player.age >= this.strafeHoldUntilTick) {
                                                        this.strafeDirection = 0;
                                                    }

                                                    if (this.strafeDirection != 0
                                                        && this.isPathClear(
                                                            d11 * 0.3 + d12 * this.strafeDirection * 0.05,
                                                            d12 * 0.3 - d11 * this.strafeDirection * 0.05,
                                                            0.0
                                                        )) {
                                                        i7 = this.strafeDirection;
                                                    }

                                                    if (i7 == 0
                                                        && Math.abs(d13) > 0.08
                                                        && (this.lastStrafeCorrectionTick < 0 || mc.player.age - this.lastStrafeCorrectionTick >= 6)) {
                                                        int i8 = d13 > 0.0 ? -1 : 1;
                                                        if (this.isPathClear(d11 * 0.35 + d12 * i8 * 0.1, d12 * 0.35 - d11 * i8 * 0.1, 0.0)) {
                                                            i7 = i8;
                                                            this.lastStrafeCorrectionTick = mc.player.age;
                                                        }
                                                    }
                                                }

                                                movementState.setInput(Input.MOVE_LEFT, i7 < 0).setInput(Input.MOVE_RIGHT, i7 > 0);
                                                movementState.setTarget(new MovementTarget(new Rotation(f3, movementTarget.rotation.getPitch()), false));
                                                if (flag && mc.player.horizontalCollision && !map.getOrDefault(Input.JUMP, false)) {
                                                    movementState.setInput(Input.JUMP, true).setInput(Input.MOVE_LEFT, false).setInput(Input.MOVE_RIGHT, false);
                                                    this.jumpExecutor = iPathExecutor;
                                                    this.jumpTick = mc.player.age;
                                                    this.jumpYaw = f2;
                                                    if (!map.getOrDefault(Input.SPRINT, false) && !mc.player.isSprinting()) {
                                                        this.jumpSprinting = false;
                                                        this.jumpY = betterBlockPos.getY();
                                                    } else {
                                                        this.jumpSprinting = true;
                                                        this.jumpY = betterBlockPos.getY();
                                                    }
                                                } else if (flag && !map.getOrDefault(Input.JUMP, false) && !mc.player.horizontalCollision) {
                                                    double d14 = mc.player.getVelocity().horizontalLength();
                                                    if (!(d14 < 0.1)
                                                        && !mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)
                                                        && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                                                        && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                                                        float f6 = (float)Math.toDegrees(Math.atan2(-mc.player.getVelocity().x, mc.player.getVelocity().z));
                                                        float f7 = Math.abs(MathHelper.wrapDegrees(f5 - f2));
                                                        float f8 = Math.abs(MathHelper.wrapDegrees(f5 - f6));
                                                        if (!(f7 > 18.0F) && !(f8 > 18.0F) && !(Math.abs(d9) > 0.45) && i7 == 0) {
                                                            boolean flag3 = map.getOrDefault(Input.SPRINT, false) || mc.player.isSprinting();
                                                            double d15 = d14 + (flag3 ? 0.2 : 0.0);
                                                            double d16 = 0.5;

                                                            for (int i9 = 0; i9 < 10; i9++) {
                                                                double d17 = d15 + (flag3 ? 0.026 : 0.02);
                                                                d16 += d17;
                                                                d15 = d17 * 0.91;
                                                            }

                                                            if (!(d16 > 16.0)) {
                                                                double d18 = 0.0;
                                                                int i10 = i;

                                                                while (i10 < i2 && d18 < d16) {
                                                                    IMovement iMovement2 = (IMovement)list.get(i10);
                                                                    if ((iMovement2 instanceof MovementTraverse || iMovement2 instanceof MovementDiagonal)
                                                                        && iMovement2.getSrc().getY() == betterBlockPos.getY()
                                                                        && iMovement2.getDest().getY() == betterBlockPos.getY()) {
                                                                        double d19 = iMovement2.getDest().getX() - iMovement2.getSrc().getX();
                                                                        double d20 = iMovement2.getDest().getZ() - iMovement2.getSrc().getZ();
                                                                        if (Math.abs(d19 * d8 - d20 * d7) > 0.2 * Math.hypot(d19, d20)) {
                                                                            return;
                                                                        }

                                                                        double d21 = iMovement2.getDest().getX() + 0.5 - vec3d.x;
                                                                        double d22 = iMovement2.getDest().getZ() + 0.5 - vec3d.z;
                                                                        double d23 = d21 * d7 + d22 * d8;
                                                                        if (!(d23 <= d18) && !(Math.abs(d21 * d8 - d22 * d7) > 0.75)) {
                                                                            d18 = d23;
                                                                            i10++;
                                                                            continue;
                                                                        }

                                                                        return;
                                                                    }

                                                                    if (d18 < 2.5) {
                                                                        return;
                                                                    }

                                                                    d16 = Math.min(d16, d18);
                                                                    break;
                                                                }

                                                                if (d18 < d16) {
                                                                    return;
                                                                }

                                                                if (this.isPathClear(d7 * d16, d8 * d16, 1.3)) {
                                                                    movementState.setInput(Input.JUMP, true);
                                                                    this.jumpExecutor = iPathExecutor;
                                                                    this.jumpTick = mc.player.age;
                                                                    this.jumpYaw = f5;
                                                                    this.jumpSprinting = flag3;
                                                                    this.jumpY = betterBlockPos.getY();
                                                                }

                                                                return;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public MovementStatus overrideMovementStatus(IBaritone iBaritone, Movement movement) {
        if (this.isEnabled()
            && this.improved.getValue()
            && this.jumpExecutor != null
            && mc.player != null
            && mc.world != null
            && iBaritone == BaritoneAPI.getProvider().getPrimaryBaritone()
            && iBaritone.getPathingBehavior().isPathing()) {
            if (iBaritone.getPathingBehavior().getCurrent() != this.jumpExecutor) {
                return null;
            }

            if (!mc.player.isOnGround() && mc.player.age > this.jumpTick && mc.player.age - this.jumpTick <= 12) {
                if (mc.player.horizontalCollision) {
                    return null;
                }

                if ((movement instanceof MovementTraverse || movement instanceof MovementDiagonal) && movement.getSrc().getY() == this.jumpY) {
                    if (movement.getDest().getY() != this.jumpY) {
                        return null;
                    }

                    IInputOverrideHandler iInputOverrideHandler = iBaritone.getInputOverrideHandler();
                    iInputOverrideHandler.clearAllKeys();
                    double d = Math.toRadians(this.jumpYaw);
                    double d2 = mc.player.getX() - movement.getDest().getX() - 0.5;
                    double d3 = mc.player.getZ() - movement.getDest().getZ() - 0.5;
                    if ((mc.player.getBlockX() != movement.getDest().getX() || mc.player.getBlockZ() != movement.getDest().getZ())
                        && (!(-d2 * Math.sin(d) + d3 * Math.cos(d) >= 0.0) || !(Math.abs(d2 * Math.cos(d) + d3 * Math.sin(d)) < 0.75))) {
                        iInputOverrideHandler.setInputForceState(Input.MOVE_FORWARD, true);
                        iInputOverrideHandler.setInputForceState(Input.SPRINT, this.jumpSprinting);
                        iBaritone.getLookBehavior().updateTarget(new Rotation(this.jumpYaw, iBaritone.getPlayerContext().playerRotations().getPitch()), false);
                        return MovementStatus.RUNNING;
                    }

                    return MovementStatus.SUCCESS;
                }
            }
        }

        return null;
    }

    private boolean isPathClear(double d, double d2, double d3) {
        Vec3d vec3d = mc.player.getEntityPos();
        int i = Math.max(1, (int)Math.ceil(Math.hypot(d, d2) / 0.2));
        Box box = mc.player.getBoundingBox();
        Mutable mutable = new Mutable();

        for (int i2 = 0; i2 <= i; i2++) {
            double d4 = (double)i2 / i;
            Box box2 = box.offset(d * d4, 0.0, d2 * d4).stretch(0.0, d3, 0.0);
            if (!mc.world.isSpaceEmpty(mc.player, box2)) {
                return false;
            }

            int i3 = MathHelper.floor(box2.minX);
            int i4 = MathHelper.floor(box2.maxX);
            int i5 = MathHelper.floor(box2.minZ);
            int i6 = MathHelper.floor(box2.maxZ);
            int i7 = MathHelper.floor(vec3d.y - 0.01);
            int i8 = MathHelper.floor(box2.maxY);

            for (int i9 = i3; i9 <= i4; i9++) {
                for (int i10 = i5; i10 <= i6; i10++) {
                    mutable.set(i9, i7, i10);
                    if (!mc.world.isChunkLoaded(mutable) || !mc.world.getWorldBorder().contains(mutable)) {
                        return false;
                    }

                    BlockState blockState = mc.world.getBlockState(mutable);
                    if (!blockState.isFullCube(mc.world, mutable)) {
                        return false;
                    }

                    if (baritone.pathing.movement.MovementHelper.avoidWalkingInto(blockState)) {
                        return false;
                    }

                    for (int i11 = i7 + 1; i11 <= i8; i11++) {
                        mutable.set(i9, i11, i10);
                        if (baritone.pathing.movement.MovementHelper.avoidWalkingInto(mc.world.getBlockState(mutable))) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private void registerListener() {
        if (!listenerRegistered) {
            final IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (iBaritone != null) {
                listenerRegistered = true;
                iBaritone.getGameEventHandler()
                    .registerEventListener(
                        new AbstractGameEventListener() {
                            @Override
                            public void onTick(TickEvent tickEvent) {
                                Baritone baritone = Baritone.getInstance();
                                if (baritone != null) {
                                    String string = baritone.getPrefix();
                                    if (!string.equals(BaritoneAPI.getSettings().prefix.value)) {
                                        BaritoneAPI.getSettings().prefix.value = string;
                                    }

                                    boolean flag = baritone.isEnabled();
                                    if (!flag || !baritone.improved.getValue() || IMinecraft.mc.player == null || !iBaritone.getPathingBehavior().isPathing()) {
                                        baritone.jumpExecutor = null;
                                        baritone.jumpTick = -1000;
                                        baritone.lastStrafeCorrectionTick = -1;
                                        baritone.nextStrafeTick = 0;
                                        baritone.strafeDirection = 0;
                                        baritone.strafeHoldUntilTick = 0;
                                        baritone.lastSpeedUpdateTick = -1;
                                        baritone.yawSpeed = 0.0F;
                                        baritone.pitchSpeed = 0.0F;
                                    } else if (baritone.jumpExecutor != null
                                        && (
                                            IMinecraft.mc.player.age - baritone.jumpTick > 12
                                                || IMinecraft.mc.player.age > baritone.jumpTick && IMinecraft.mc.player.isOnGround()
                                        )) {
                                        if (IMinecraft.mc.player.isOnGround()) {
                                            baritone.nextStrafeTick = IMinecraft.mc.player.age + 3;
                                        }

                                        baritone.jumpExecutor = null;
                                    }

                                    if (BaritoneAPI.getSettings().prefixControl.value != flag) {
                                        BaritoneAPI.getSettings().prefixControl.value = flag;
                                    }

                                    if (BaritoneAPI.getSettings().chatControl.value) {
                                        BaritoneAPI.getSettings().chatControl.value = false;
                                    }

                                    if (BaritoneAPI.getSettings().chatControlAnyway.value) {
                                        BaritoneAPI.getSettings().chatControlAnyway.value = false;
                                    }

                                    if (BaritoneAPI.getSettings().doDeathWaypoints.value != flag) {
                                        BaritoneAPI.getSettings().doDeathWaypoints.value = flag;
                                    }

                                    baritone.applySettingRestrictions();
                                    if (tickEvent.getType() == Type.IN && IMinecraft.mc.player != null) {
                                        baritone.updateRotations(iBaritone);
                                    }

                                    if (!flag) {
                                        IPathingBehavior iPathingBehavior = iBaritone.getPathingBehavior();
                                        if (iPathingBehavior.isPathing() || iPathingBehavior.hasPath() || iPathingBehavior.getInProgress().isPresent() || iPathingBehavior.getGoal() != null) {
                                            iPathingBehavior.cancelEverything();
                                        }
                                    }
                                }
                            }
                        }
                    );
            }
        }
    }

    public void setAllowPlace(boolean flag) {
        this.allowPlace = flag;
    }
}
