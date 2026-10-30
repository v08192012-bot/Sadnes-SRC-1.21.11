package tech.sadnes.client.util.player;

import java.util.ArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import org.joml.Vector2f;
import tech.sadnes.api.mixin.accessor.EntityAccessor;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.client.util.world.HoleFinder;

public final class MoveUtil implements IMinecraft {
    private static float centerYaw = 0.0F;
    private static boolean centering = false;
    private static BlockPos edgeTargetPos;
    private static float lastForward;
    private static float lastStrafe;
    private static float lastInputForward;
    private static float lastInputStrafe;
    private static int lastInputAge;
    private static float aimOffsetHorizontal;
    private static float aimOffsetVertical;
    private static float aimSpeedHorizontal = 0.5F;
    private static float aimSpeedVertical = 0.4F;
    private static long lastAimUpdateTime;
    private static int aimTargetId = Integer.MIN_VALUE;

    public static double getSpeed() {
        if (mc.player == null) {
            return 0.0;
        }

        Vec3d vec3d = mc.player.getVelocity();
        return Math.hypot(vec3d.x, vec3d.z);
    }

    public static void setSpeed(double d) {
        if (isMoving()) {
            double d2 = getDirection(true);
            mc.player.setVelocity(-Math.sin(d2) * d, mc.player.getVelocity().y, Math.cos(d2) * d);
        }
    }

    public static double getDirection(boolean flag) {
        float f = mc.player.getYaw();
        if (mc.player.forwardSpeed < 0.0F) {
            f += 180.0F;
        }

        float f2 = 1.0F;
        if (mc.player.forwardSpeed < 0.0F) {
            f2 = -0.5F;
        } else if (mc.player.forwardSpeed > 0.0F) {
            f2 = 0.5F;
        }

        if (mc.player.sidewaysSpeed > 0.0F) {
            f -= 90.0F * f2;
        }

        if (mc.player.sidewaysSpeed < 0.0F) {
            f += 90.0F * f2;
        }

        return flag ? Math.toRadians(f) : f;
    }

    public static double getHorizontalSpeed() {
        return getSpeed();
    }

    public static double getDeltaSpeed() {
        if (mc.player == null) {
            return 0.0;
        }

        double d = mc.player.getX() - mc.player.lastX;
        double d2 = mc.player.getZ() - mc.player.lastZ;
        return Math.sqrt(d * d + d2 * d2);
    }

    public static double getHorizontalDelta(Entity entity) {
        if (entity == null) {
            return 0.0;
        }

        double d = entity.getX() - entity.lastX;
        double d2 = entity.getZ() - entity.lastZ;
        return Math.sqrt(d * d + d2 * d2);
    }

    public static double getDelta3d(Entity entity) {
        if (entity == null) {
            return 0.0;
        }

        double d = entity.getX() - entity.lastX;
        double d2 = entity.getZ() - entity.lastZ;
        double d3 = entity.getY() - entity.lastY;
        return Math.sqrt(d * d + d2 * d2 + d3 * d3);
    }

    public static boolean isMoving() {
        return mc.player == null || mc.player.input == null
            ? false
            : mc.player.input.getMovementInput().x != 0.0F || mc.player.input.getMovementInput().y != 0.0F;
    }

    public static boolean isInCobwebMultiplier() {
        return mc.player == null ? false : ((EntityAccessor)mc.player).sadnes$getMovementMultiplier().lengthSquared() == 0.1275000000745058;
    }

    public static boolean isInPowderSnowMultiplier() {
        return mc.player == null ? false : ((EntityAccessor)mc.player).sadnes$getMovementMultiplier().lengthSquared() == 1.842500038146973;
    }

    public static void setMotion(double d, float f, double d2, double d3) {
        if (mc.player != null) {
            if (d3 != 0.0) {
                if (d2 > 0.0) {
                    f += d3 > 0.0 ? -45.0F : 45.0F;
                }

                if (d2 < 0.0) {
                    f += d3 > 0.0 ? 45.0F : -45.0F;
                }

                d2 = 0.0;
                if (d3 > 0.0) {
                    d3 = 1.0;
                }

                if (d3 < 0.0) {
                    d3 = -1.0;
                }
            }

            if (d2 > 0.0) {
                d2 = 1.0;
            }

            if (d2 < 0.0) {
                d2 = -1.0;
            }

            double d4 = Math.cos(Math.toRadians(f + 90.0F));
            double d5 = Math.sin(Math.toRadians(f + 90.0F));
            double d6 = d3 * d * d4 + d2 * d * d5;
            double d7 = d3 * d * d5 - d2 * d * d4;
            double d8 = mc.player.getVelocity().y;
            mc.player.setVelocity(d6, d8, d7);
        }
    }

    public static void setMotion(double d) {
        if (mc.player != null && mc.player.input != null) {
            Vec2f vec2f = mc.player.input.getMovementInput();
            setMotion(d, mc.player.getYaw(), vec2f.x, vec2f.y);
        }
    }

    public static void setMotionWithYaw(double d, float f) {
        if (mc.player != null && mc.player.input != null) {
            Vec2f vec2f = mc.player.input.getMovementInput();
            setMotion(d, f, vec2f.x, vec2f.y);
        }
    }

    public static double[] getMotionForYaw(double d, float f) {
        double d2 = Math.toRadians(f);
        double d3 = -Math.sin(d2) * d;
        double d4 = Math.cos(d2) * d;
        return new double[]{d3, d4};
    }

    public static double[] getMotionForInput(double d) {
        if (mc.player != null && mc.player.input != null) {
            Vec2f vec2f = mc.player.input.getMovementInput();
            float f = vec2f.y;
            float f2 = vec2f.x;
            float f3 = mc.player.getYaw();
            if (f != 0.0F) {
                if (f2 > 0.0F) {
                    f3 += f > 0.0F ? -45.0F : 45.0F;
                    f2 = 0.0F;
                } else {
                    if (f2 < 0.0F) {
                        f3 += f > 0.0F ? 45.0F : -45.0F;
                    }

                    f2 = 0.0F;
                }

                if (f > 0.0F) {
                    f = 1.0F;
                } else if (f < 0.0F) {
                    f = -1.0F;
                }
            }

            double d2 = Math.sin(Math.toRadians(f3 + 90.0F));
            double d3 = Math.cos(Math.toRadians(f3 + 90.0F));
            return new double[]{f * d * d3 + f2 * d * d2, f * d * d2 - f2 * d * d3};
        } else {
            return new double[]{0.0, 0.0};
        }
    }

    public static void stopHorizontal() {
        if (mc.player != null) {
            Vec3d vec3d = mc.player.getVelocity();
            mc.player.setVelocity(0.0, vec3d.y, 0.0);
        }
    }

    public static double getDirectionFromInput(float f, float f2, float f3) {
        if (f2 < 0.0F) {
            f += 180.0F;
        }

        float f4 = 1.0F;
        if (f2 < 0.0F) {
            f4 = -0.5F;
        }

        if (f2 > 0.0F) {
            f4 = 0.5F;
        }

        if (f3 > 0.0F) {
            f -= 90.0F * f4;
        }

        if (f3 < 0.0F) {
            f += 90.0F * f4;
        }

        return Math.toRadians(f);
    }

    public static void centerOnBlock(MoveInputEvent moveInputEvent, BlockPos blockPos) {
        if (mc.player != null) {
            double d = mc.player.getX() - blockPos.getX();
            double d2 = mc.player.getZ() - blockPos.getZ();
            double d3 = d - 0.5;
            double d4 = d2 - 0.5;
            double d5 = d3 * d3 + d4 * d4;
            if (d5 > 0.425 * 0.425) {
                centering = false;
            } else {
                if (!centering) {
                    centerYaw = (float)Math.toDegrees(Math.atan2(-d3, d4));
                    centering = true;
                }

                float f = mc.player.getYaw();
                double d6 = Math.toRadians(f - centerYaw);
                float f2 = (float)Math.cos(d6);
                float f3 = (float)Math.sin(d6);
                moveInputEvent.setForward(Math.signum(f2));
                moveInputEvent.setStrafe(Math.signum(-f3));
            }
        }
    }

    public static boolean steerToSafeEdge(MoveInputEvent moveInputEvent) {
        if (mc.player == null || mc.world == null) {
            return false;
        } else if (mc.player.isOnGround() && !mc.player.input.playerInput.jump()) {
            BlockPos blockPos = findSafeEdgeBlock(moveInputEvent);
            return blockPos != null && steerToBlock(moveInputEvent, blockPos);
        } else {
            edgeTargetPos = null;
            return false;
        }
    }

    public static boolean steerToBlock(MoveInputEvent moveInputEvent, BlockPos blockPos) {
        if (mc.player != null && mc.world != null && blockPos != null) {
            Box box = mc.player.getBoundingBox().offset(0.0, -0.1, 0.0);
            boolean flag = !mc.world.getBlockCollisions(mc.player, box).iterator().hasNext();
            if (flag) {
                moveInputEvent.setForward(0.0F);
                moveInputEvent.setStrafe(0.0F);
                return true;
            } else {
                double d = blockPos.getX() + 0.5 - mc.player.getX();
                double d2 = blockPos.getZ() + 0.5 - mc.player.getZ();
                double d3 = Math.max(Math.sqrt(d * d + d2 * d2), 1.0E-4);
                double d4 = d3 * (1.0 - getGroundFriction());
                Vec3d vec3d = mc.player.getVelocity();
                double d5 = d / d3 * d4 - vec3d.x;
                double d6 = d2 / d3 * d4 - vec3d.z;
                if (Math.sqrt(d5 * d5 + d6 * d6) < 0.04) {
                    moveInputEvent.setForward(0.0F);
                    moveInputEvent.setStrafe(0.0F);
                    return true;
                } else {
                    float f = mc.player.getYaw() * (float) (Math.PI / 180.0);
                    double d7 = MathHelper.sin(f);
                    double d8 = MathHelper.cos(f);
                    double d9 = d6 * d8 - d5 * d7;
                    double d10 = d5 * d8 + d6 * d7;
                    Vec2f vec2f = mc.player.horizontalCollision
                        ? new Vec2f((float)Math.signum(d10), (float)Math.signum(d9))
                        : getBestInputForVector(d9, d10);
                    moveInputEvent.setForward(vec2f.y);
                    moveInputEvent.setStrafe(vec2f.x);
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private static BlockPos findSafeEdgeBlock(MoveInputEvent moveInputEvent) {
        int i = MathHelper.floor(mc.player.getY()) - 1;
        if (edgeTargetPos != null) {
            boolean flag;
            if (edgeTargetPos.getY() == i
                && Math.abs(edgeTargetPos.getX() + 0.5 - mc.player.getX()) <= 3.0
                && Math.abs(edgeTargetPos.getZ() + 0.5 - mc.player.getZ()) <= 3.0) {
                flag = isIsolatedPillar(edgeTargetPos);
            } else {
                flag = false;
            }

            if (flag) {
                return edgeTargetPos;
            }

            edgeTargetPos = null;
        }

        if (!moveInputEvent.isMoving()) {
            return null;
        }

        int i2 = MathHelper.floor(mc.player.getX());
        int i3 = MathHelper.floor(mc.player.getZ());
        float f = mc.player.getYaw() * (float) (Math.PI / 180.0);
        double d = MathHelper.sin(f);
        double d2 = MathHelper.cos(f);
        Mutable mutable = new Mutable();
        double d3 = Double.MAX_VALUE;

        for (int i4 = -2; i4 <= 2; i4++) {
            for (int i5 = -2; i5 <= 2; i5++) {
                mutable.set(i2 + i4, i, i3 + i5);
                if (isIsolatedPillar(mutable)) {
                    double d4 = mutable.getX() + 0.5 - mc.player.getX();
                    double d5 = mutable.getZ() + 0.5 - mc.player.getZ();
                    double d6 = Math.max(Math.sqrt(d4 * d4 + d5 * d5), 1.0E-4);
                    if (!(d6 >= d3)) {
                        if (i4 == 0 && i5 == 0) {
                            d3 = d6;
                        } else {
                            double d7 = (d5 * d2 - d4 * d) / d6;
                            if (d7 <= 0.25) {
                                continue;
                            }

                            d3 = d6;
                        }

                        edgeTargetPos = mutable.toImmutable();
                    }
                }
            }
        }

        return edgeTargetPos;
    }

    public static BlockPos findNearestBlockInRange(BlockPos blockPos, double d, int i, Vec3d vec3d) {
        if (mc.world == null) {
            return null;
        }

        int i2 = MathHelper.ceil(d);
        int i3 = Math.min(blockPos.getY(), i);
        int i4 = Math.max(i3 - 2, blockPos.getY() - 3);
        if (i4 > i3) {
            return null;
        }

        BlockPos blockPos2 = new BlockPos(blockPos.getX() - i2, i4, blockPos.getZ() - i2);
        BlockPos blockPos3 = new BlockPos(blockPos.getX() + i2, i3, blockPos.getZ() + i2);
        BlockPos blockPos4 = null;
        double d2 = Double.MAX_VALUE;

        for (BlockPos blockPos5 : HoleFinder.findHoles(blockPos2, blockPos3)) {
            boolean flag = blockPos.getSquaredDistance(blockPos5.getX(), blockPos.getY(), blockPos5.getZ()) > d * d;
            double d3 = vec3d.squaredDistanceTo(blockPos5.getX() + 0.5, vec3d.y, blockPos5.getZ() + 0.5);
            if (!flag && !(d3 > d2)) {
                d2 = d3;
                blockPos4 = blockPos5;
            }
        }

        return blockPos4;
    }

    public static boolean canWalkTo(BlockPos blockPos) {
        if (mc.player != null && mc.world != null && blockPos != null) {
            Box box = mc.player.getBoundingBox();
            double d = blockPos.getX() + 0.5 - mc.player.getX();
            double d2 = blockPos.getZ() + 0.5 - mc.player.getZ();
            double d3 = Math.sqrt(d * d + d2 * d2);
            if (d3 < 0.5) {
                return true;
            }

            double d4 = d / d3;
            double d5 = d2 / d3;
            double d6 = box.minY;

            for (double d7 = 0.5; d7 < d3; d7 += 0.5) {
                Box box2 = box.offset(d4 * d7, d6 - box.minY, d5 * d7);
                if (mc.world.getBlockCollisions(mc.player, box2).iterator().hasNext()) {
                    if (++d6 - box.minY > 1.0) {
                        return false;
                    }

                    box2 = box.offset(d4 * d7, d6 - box.minY, d5 * d7);
                    if (mc.world.getBlockCollisions(mc.player, box2).iterator().hasNext()) {
                        return false;
                    }
                }

                for (Box box3 = new Box(box2.minX, d6 - 1.0, box2.minZ, box2.maxX, d6, box2.maxZ);
                    !mc.world.getBlockCollisions(mc.player, box3).iterator().hasNext();
                    box3 = box3.offset(0.0, -1.0, 0.0)
                ) {
                    boolean flag = d6 <= blockPos.getY();
                    if (flag) {
                        return false;
                    }

                    d6--;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private static double getGroundFriction() {
        BlockPos blockPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getBoundingBox().minY - 0.500001, mc.player.getZ());
        return mc.world.getBlockState(blockPos).getBlock().getSlipperiness() * 0.91;
    }

    public static Vec3d findEscapeDirection(Entity entity) {
        if (mc.player != null && mc.world != null && entity != null) {
            double d = mc.player.getX() - entity.getX();
            double d2 = mc.player.getZ() - entity.getZ();
            double d3 = Math.sqrt(d * d + d2 * d2);
            double d4;
            double d5;
            if (d3 < 0.1) {
                float f = mc.player.getYaw() * (float) (Math.PI / 180.0);
                d4 = -MathHelper.sin(f);
                d5 = MathHelper.cos(f);
            } else {
                d4 = d / d3;
                d5 = d2 / d3;
            }

            Vec3d vec3d = null;
            double d6 = 0.0;

            for (int i = 0; i < 16; i++) {
                double d7 = i * 0.39269908;
                double d8 = Math.cos(d7);
                double d9 = Math.sin(d7);
                double d10 = d8 * d4 + d9 * d5;
                if (!(d10 <= 0.0)) {
                    double d11 = d10 * getWalkableDistance(d8, d9);
                    if (!(d11 <= d6)) {
                        d6 = d11;
                        vec3d = new Vec3d(d8, 0.0, d9);
                    }
                }
            }

            return vec3d;
        } else {
            return null;
        }
    }

    public static void steerToDirection(MoveInputEvent moveInputEvent, Vec3d vec3d) {
        if (mc.player != null && vec3d != null) {
            float f = mc.player.getYaw() * (float) (Math.PI / 180.0);
            double d = MathHelper.sin(f);
            double d2 = MathHelper.cos(f);
            Vec2f vec2f = getBestInputForVector(vec3d.z * d2 - vec3d.x * d, vec3d.x * d2 + vec3d.z * d);
            moveInputEvent.setForward(vec2f.y);
            moveInputEvent.setStrafe(vec2f.x);
        }
    }

    private static double getWalkableDistance(double d, double d2) {
        Box box = mc.player.getBoundingBox();
        double d3 = 0.0;

        for (double d4 = 0.5; d4 <= 3.0; d4 += 0.5) {
            Box box2 = box.offset(d * d4, 0.6, d2 * d4);
            if (mc.world.getBlockCollisions(mc.player, box2).iterator().hasNext()) {
                return d3;
            }

            Box box3 = new Box(box2.minX, box.minY - 3.0, box2.minZ, box2.maxX, box.minY, box2.maxZ);
            if (!mc.world.getBlockCollisions(mc.player, box3).iterator().hasNext()) {
                return d3;
            }

            d3 = d4;
        }

        return d3;
    }

    private static Vec2f getBestInputForVector(double d, double d2) {
        float f = 0.0F;
        float f2 = 0.0F;
        double d3 = -Double.MAX_VALUE;

        for (float f3 = -1.0F; f3 <= 1.0F; f3++) {
            for (float f4 = -1.0F; f4 <= 1.0F; f4++) {
                if (f3 != 0.0F || f4 != 0.0F) {
                    double d4 = Math.sqrt(f3 * f3 + f4 * f4);
                    double d5 = (f3 * d + f4 * d2) / d4;
                    if (!(d5 <= d3)) {
                        d3 = d5;
                        f = f3;
                        f2 = f4;
                    }
                }
            }
        }

        return new Vec2f(f2, f);
    }

    private static boolean isIsolatedPillar(BlockPos blockPos) {
        return !isEmptyBlock(blockPos)
            ? false
            : !isEmptyBlock(blockPos.north()) && !isEmptyBlock(blockPos.south()) && !isEmptyBlock(blockPos.east()) && !isEmptyBlock(blockPos.west());
    }

    private static boolean isEmptyBlock(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
    }

    public static void steerTowardsBlock(MoveInputEvent moveInputEvent, BlockPos blockPos) {
        if (mc.player != null && mc.world != null) {
            new Box(blockPos);
            if (mc.player.horizontalCollision && mc.player.getBlockPos() == blockPos) {
                moveInputEvent.setForward(0.0F);
                moveInputEvent.setStrafe(0.0F);
            } else {
                double d = blockPos.getX() + 0.5 - mc.player.getX();
                double d2 = blockPos.getZ() + 0.5 - mc.player.getZ();
                double d3 = Math.sqrt(d * d + d2 * d2);
                if (d3 < 0.05) {
                    moveInputEvent.setForward(0.0F);
                    moveInputEvent.setStrafe(0.0F);
                } else {
                    double d4 = d / d3;
                    double d5 = d2 / d3;
                    float f = mc.player.getYaw();
                    double d6 = Math.toRadians(f);
                    float f2 = (float)(d5 * Math.cos(d6) + d4 * Math.sin(d6));
                    float f3 = (float)(d4 * Math.cos(d6) - d5 * Math.sin(d6));
                    if (Math.abs(f2) > Math.abs(f3)) {
                        f3 = 0.0F;
                    } else {
                        f2 = 0.0F;
                    }

                    moveInputEvent.setForward(Math.signum(f2));
                    moveInputEvent.setStrafe(Math.signum(f3));
                }
            }
        }
    }

    public static boolean isRidingNautilus() {
        return mc.player != null && mc.player.getVehicle() instanceof AbstractBoatEntity;
    }

    public static void fixMovement(MoveInputEvent moveInputEvent, float f) {
        if (mc.player != null && !isRidingNautilus()) {
            float f2 = moveInputEvent.getForward();
            float f3 = moveInputEvent.getStrafe();
            if (f2 == 0.0F && f3 == 0.0F) {
                lastForward = 0.0F;
                lastStrafe = 0.0F;
            } else {
                double d = MathHelper.wrapDegrees(Math.toDegrees(getDirectionFromInput(mc.player.isGliding() ? mc.player.getYaw() : f, f2, f3)));
                boolean flag = mc.player.isSprinting() && f2 > 0.0F;
                float f4 = 0.0F;
                float f5 = 0.0F;
                double d2 = Double.MAX_VALUE;
                float f6 = 0.0F;
                float f7 = 0.0F;
                double d3 = Double.MAX_VALUE;

                for (float f8 = -1.0F; f8 <= 1.0F; f8++) {
                    for (float f9 = -1.0F; f9 <= 1.0F; f9++) {
                        if (f9 != 0.0F || f8 != 0.0F) {
                            double d4 = getInputAngleDiff(d, f8, f9);
                            if (d4 < d2) {
                                d2 = d4;
                                f4 = f8;
                                f5 = f9;
                            }

                            if (f8 > 0.0F && d4 < d3) {
                                d3 = d4;
                                f6 = f8;
                                f7 = f9;
                            }
                        }
                    }
                }

                if (flag && f4 <= 0.0F && d3 - d2 <= 45.0) {
                    f4 = f6;
                    f5 = f7;
                    d2 = d3;
                }

                boolean flag2 = f2 == lastInputForward && f3 == lastInputStrafe && mc.player.age - lastInputAge <= 1;
                boolean flag3 = lastForward != 0.0F || lastStrafe != 0.0F;
                boolean flag4 = !flag || lastForward > 0.0F;
                if (flag2 && flag3 && flag4 && (f4 != lastForward || f5 != lastStrafe) && getInputAngleDiff(d, lastForward, lastStrafe) - d2 < 15.0) {
                    f4 = lastForward;
                    f5 = lastStrafe;
                }

                lastInputForward = f2;
                lastInputStrafe = f3;
                lastForward = f4;
                lastStrafe = f5;
                lastInputAge = mc.player.age;
                moveInputEvent.setForward(f4);
                moveInputEvent.setStrafe(f5);
            }
        }
    }

    private static double getInputAngleDiff(double d, float f, float f2) {
        double d2 = MathHelper.wrapDegrees(Math.toDegrees(getDirectionFromInput(mc.player.getYaw(), f, f2)));
        return Math.abs(MathHelper.wrapDegrees((float)(d - d2)));
    }

    public static void fixMovementToTarget(MoveInputEvent moveInputEvent, float f, LivingEntity livingEntity) {
        if (mc.player != null && !isRidingNautilus()) {
            if (livingEntity == null) {
                fixMovement(moveInputEvent, f);
            } else {
                float f2 = moveInputEvent.getForward();
                float f3 = moveInputEvent.getStrafe();
                Vec3d vec3d = predictPosition(livingEntity, 3.0F);
                Vec2f vec2f = RotationUtil.getRotationsTo(vec3d);
                double d = MathHelper.wrapDegrees(Math.toDegrees(getDirectionFromInput(vec2f.x, f2, f3)));
                if (f2 != 0.0F || f3 != 0.0F) {
                    float f4 = 0.0F;
                    float f5 = 0.0F;
                    float f6 = Float.MAX_VALUE;

                    for (float f7 = -1.0F; f7 <= 1.0F; f7++) {
                        for (float f8 = -1.0F; f8 <= 1.0F; f8++) {
                            if (f8 != 0.0F || f7 != 0.0F) {
                                double d2 = MathHelper.wrapDegrees(Math.toDegrees(getDirectionFromInput(mc.player.getYaw(), f7, f8)));
                                double d3 = Math.abs(MathHelper.wrapDegrees((float)(d - d2)));
                                if (d3 < f6) {
                                    f6 = (float)d3;
                                    f4 = f7;
                                    f5 = f8;
                                }
                            }
                        }
                    }

                    moveInputEvent.setForward(f4);
                    moveInputEvent.setStrafe(f5);
                }
            }
        }
    }

    public static void moveTowardsTarget(MoveInputEvent moveInputEvent, LivingEntity livingEntity) {
        if (mc.player != null && livingEntity != null && !isRidingNautilus()) {
            float f = moveInputEvent.getForward();
            float f2 = moveInputEvent.getStrafe();
            Vec3d vec3d = predictPosition(livingEntity, 0.0F);
            double d = MathHelper.wrapDegrees(Math.toDegrees(getDirectionFromInput(RotationUtil.getRotationsTo(vec3d).x, f, f2)));
            if (f != 0.0F || f2 != 0.0F) {
                float f3 = 0.0F;
                float f4 = 0.0F;
                float f5 = Float.MAX_VALUE;

                for (float f6 = -1.0F; f6 <= 1.0F; f6++) {
                    for (float f7 = -1.0F; f7 <= 1.0F; f7++) {
                        if (f7 != 0.0F || f6 != 0.0F) {
                            double d2 = MathHelper.wrapDegrees(Math.toDegrees(getDirectionFromInput(mc.player.getYaw(), f6, f7)));
                            double d3 = Math.abs(MathHelper.wrapDegrees((float)(d - d2)));
                            if (d3 < f5) {
                                f5 = (float)d3;
                                f3 = f6;
                                f4 = f7;
                            }
                        }
                    }
                }

                moveInputEvent.setForward(f3);
                moveInputEvent.setStrafe(f4);
            }
        }
    }

    public static Vec3d getWanderingAimPoint(LivingEntity livingEntity) {
        Box box = livingEntity.getBoundingBox();
        long l = System.currentTimeMillis();
        if (livingEntity.getId() != aimTargetId) {
            aimTargetId = livingEntity.getId();
            aimOffsetHorizontal = CombatUtil.randomFloat(-0.6F, 0.6F);
            aimOffsetVertical = CombatUtil.randomFloat(-0.6F, 0.6F);
            aimSpeedHorizontal = randomAimSpeed();
            aimSpeedVertical = randomAimSpeed();
            lastAimUpdateTime = l;
        }

        float f = (float)Math.min(l - lastAimUpdateTime, 100L) / 1000.0F;
        lastAimUpdateTime = l;
        aimOffsetHorizontal = aimOffsetHorizontal + aimSpeedHorizontal * f;
        aimOffsetVertical = aimOffsetVertical + aimSpeedVertical * f;
        if (Math.abs(aimOffsetHorizontal) > 1.0F) {
            aimOffsetHorizontal = Math.signum(aimOffsetHorizontal);
            aimSpeedHorizontal = -Math.signum(aimOffsetHorizontal) * randomAimSpeed();
            aimSpeedVertical = Math.signum(aimSpeedVertical) * randomAimSpeed();
        }

        if (Math.abs(aimOffsetVertical) > 1.0F) {
            aimOffsetVertical = Math.signum(aimOffsetVertical);
            aimSpeedVertical = -Math.signum(aimOffsetVertical) * randomAimSpeed();
            aimSpeedHorizontal = Math.signum(aimSpeedHorizontal) * randomAimSpeed();
        }

        Vec3d vec3d = mc.player.getEyePos();
        double d = (box.minX + box.maxX) / 2.0;
        double d2 = (box.minZ + box.maxZ) / 2.0;
        double d3 = d - vec3d.x;
        double d4 = d2 - vec3d.z;
        double d5 = Math.max(Math.hypot(d3, d4), 1.0E-4);
        return new Vec3d(
            d + -d4 / d5 * aimOffsetHorizontal, (box.minY + box.maxY) / 2.0 + aimOffsetVertical, d2 + d3 / d5 * aimOffsetHorizontal
        );
    }

    private static float randomAimSpeed() {
        return CombatUtil.randomFloat(0.8F, 1.6F);
    }

    public static Vec3d predictPosition(LivingEntity livingEntity, float f) {
        Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura != null && !attackAura.shouldRotateTo(attackAura.getCurrentTarget())) {
            f = 0.0F;
        }

        double d = vec3d.x + (livingEntity.getX() - livingEntity.lastX) * f;
        double d2 = vec3d.y + (livingEntity.getY() - livingEntity.lastY) * f;
        double d3 = vec3d.z + (livingEntity.getZ() - livingEntity.lastZ) * f;
        return new Vec3d(d, d2, d3);
    }

    public static MoveUtil.Direction findStrafeDirection(LivingEntity livingEntity, Vec3d vec3d) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d2 = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            Box box = livingEntity.getBoundingBox().expand(0.3, 0.0, 0.3);
            double d = vec3d2.squaredDistanceTo(vec3d);
            MoveUtil.Direction[] directions = new MoveUtil.Direction[]{
                MoveUtil.Direction.UP,
                MoveUtil.Direction.UP_RIGHT,
                MoveUtil.Direction.RIGHT,
                MoveUtil.Direction.DOWN_RIGHT,
                MoveUtil.Direction.DOWN,
                MoveUtil.Direction.DOWN_LEFT,
                MoveUtil.Direction.LEFT,
                MoveUtil.Direction.UP_LEFT
            };
            ArrayList<MoveUtil.Direction> directions2 = new ArrayList<>();

            for (MoveUtil.Direction direction : directions) {
                Vec3d vec3d3 = vec3d.add(direction.getOffsetX(), 0.0, direction.getOffsetZ());
                if (!(vec3d2.squaredDistanceTo(vec3d3) < d)) {
                    Vec3d vec3d4 = new Vec3d(vec3d3.x, vec3d2.y + mc.player.getStandingEyeHeight(), vec3d3.z);
                    if (RaycastUtil.canSee(mc.player.getEyePos(), vec3d4)) {
                        Vec3d vec3d5 = vec3d3.subtract(vec3d2).normalize();
                        double d2 = vec3d2.distanceTo(vec3d3);
                        Vec3d vec3d6 = vec3d2.add(vec3d5.multiply(Math.min(d2, 1.5)));
                        float f = mc.player.getWidth() / 2.0F;
                        Box box2 = new Box(vec3d6.x - f, vec3d2.y, vec3d6.z - f, vec3d6.x + f, vec3d2.y + mc.player.getHeight(), vec3d6.z + f);
                        if (!box2.intersects(box)) {
                            directions2.add(direction);
                        }
                    }
                }
            }

            if (directions2.isEmpty()) {
                return MoveUtil.Direction.random();
            }

            MoveUtil.Direction direction2 = (MoveUtil.Direction)directions2.get(0);
            double d3 = Double.MAX_VALUE;

            for (MoveUtil.Direction direction3 : directions2) {
                double d4 = vec3d2.squaredDistanceTo(vec3d.add(direction3.getOffsetX(), 0.0, direction3.getOffsetZ()));
                if (d4 < d3) {
                    d3 = d4;
                    direction2 = direction3;
                }
            }

            return direction2;
        } else {
            return MoveUtil.Direction.random();
        }
    }

    public static double getKeyboardMoveYaw(float f) {
        if (mc.options == null) {
            return f;
        }

        boolean flag = mc.options.forwardKey.isPressed();
        boolean flag2 = mc.options.leftKey.isPressed();
        boolean flag3 = mc.options.backKey.isPressed();
        boolean flag4 = mc.options.rightKey.isPressed();
        if (!flag2 || !flag4 || flag && flag3 || !flag && !flag3) {
            if (!flag || !flag3 || flag2 && flag4 || !flag2 && !flag4) {
                if ((!flag2 || !flag4 || flag) && (!flag || !flag3 || flag2)) {
                    if (!flag2 && !flag4 && !flag3) {
                        return f + 0;
                    } else {
                        return flag2
                            ? f + (flag && !flag3 ? 45 : (flag3 && !flag ? (!flag2 && !flag4 ? 180 : 135) : 90)) * -1
                            : f + (flag && !flag3 ? 45 : (flag3 && !flag ? (!flag2 && !flag4 ? 180 : 135) : 90)) * 1;
                    }
                } else {
                    return f + 0;
                }
            } else {
                return flag2 ? f + -90 : f + 90;
            }
        } else {
            return flag ? f + 0 : f + 180;
        }
    }

    public static double getMoveYaw(double d, double d2, float f) {
        boolean flag = d > 0.0;
        boolean flag2 = d2 > 0.0;
        boolean flag3 = d < 0.0;
        boolean flag4 = d2 < 0.0;
        if (!flag2 && !flag4 && !flag3) {
            return f + 0;
        } else {
            return flag2
                ? f + (flag ? 45 : (flag3 ? (!flag2 && !flag4 ? 180 : 135) : 90)) * -1
                : f + (flag ? 45 : (flag3 ? (!flag2 && !flag4 ? 180 : 135) : 90)) * 1;
        }
    }

    private MoveUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public enum Direction {
        UP(0, -1),
        UP_RIGHT(1, -1),
        RIGHT(1, 0),
        DOWN_RIGHT(1, 1),
        DOWN(0, 1),
        DOWN_LEFT(-1, 1),
        LEFT(-1, 0),
        UP_LEFT(-1, -1),
        READY(0, 0);

        private final int offsetX;
        private final int offsetZ;

        public static MoveUtil.Direction random() {
            MoveUtil.Direction[] directions = new MoveUtil.Direction[]{UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT, LEFT, UP_LEFT};
            return directions[CombatUtil.randomInt(0, directions.length - 1)];
        }

        public static Vector2f scale(MoveUtil.Direction direction, float f) {
            return scale(direction, f, f);
        }

        public static Vector2f scale(MoveUtil.Direction direction, float f, float f2) {
            return new Vector2f(direction.offsetX * f, direction.offsetZ * f2);
        }

        public boolean isDiagonal() {
            return this.offsetX != 0 && this.offsetZ != 0;
        }

        public int getOffsetX() {
            return this.offsetX;
        }

        public int getOffsetZ() {
            return this.offsetZ;
        }

        Direction(int i, int i2) {
            this.offsetX = i;
            this.offsetZ = i2;
        }
    }
}
