package tech.sadnes.client.util.player;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;

public final class FallUtil implements IMinecraft {
    public static double predictFallDistance(double d) {
        if (mc.player == null) {
            return 0.0;
        }

        double d2 = d;
        double d3 = mc.player.getVelocity().y;
        double d4 = mc.player.fallDistance;

        for (int i = 0; d2 > 0.0 && i < 200; i++) {
            d3 = (d3 - 0.08) * 0.98;
            if (d3 < 0.0) {
                d4 += Math.min(-d3, d2);
            }

            d2 += d3;
        }

        return d4;
    }

    public static double getFallDistanceToGround() {
        return predictFallDistance(getDistanceToGround(mc.player));
    }

    public static double predictFallDistanceAfterTicks(int i) {
        if (mc.player == null) {
            return 0.0;
        }

        double d = mc.player.getVelocity().y;
        double d2 = mc.player.fallDistance;

        for (int i2 = 0; i2 < i; i2++) {
            d = (d - 0.08) * 0.98;
            if (d < 0.0) {
                d2 += -d;
            }
        }

        return d2;
    }

    public static int getTicksToFall(double d) {
        return getTicksToFall(mc.player, d);
    }

    public static int getTicksToFall(LivingEntity livingEntity, double d) {
        if (livingEntity != null && !(d <= 1.0E-4)) {
            double d2 = d;
            double d3 = livingEntity.getVelocity().y;

            int i;
            for (i = 0; d2 > 0.0 && i < 200; i++) {
                d3 = (d3 - 0.08) * 0.98;
                d2 -= -d3;
            }

            return i;
        } else {
            return 0;
        }
    }

    public static int getTicksToGround() {
        return getTicksToGround(mc.player);
    }

    public static int getTicksToGround(LivingEntity livingEntity) {
        return livingEntity == null ? 0 : getTicksToFall(livingEntity, getDistanceToGround(livingEntity));
    }

    public static double getDistanceToGround(LivingEntity livingEntity) {
        if (mc.world != null && livingEntity != null) {
            Box box = livingEntity.getBoundingBox();
            double d = box.minY;
            int i = MathHelper.floor(box.minX);
            int i2 = MathHelper.floor(box.maxX);
            int i3 = MathHelper.floor(box.minZ);
            int i4 = MathHelper.floor(box.maxZ);
            int i5 = MathHelper.floor(d);
            double d2 = Double.NEGATIVE_INFINITY;

            for (int i6 = i5; i6 >= Math.max(mc.world.getBottomY(), i5 - 256); i6--) {
                for (int i7 = i; i7 <= i2; i7++) {
                    for (int i8 = i3; i8 <= i4; i8++) {
                        BlockPos blockPos = new BlockPos(i7, i6, i8);
                        BlockState blockState = mc.world.getBlockState(blockPos);
                        VoxelShape voxelShape = blockState.getCollisionShape(mc.world, blockPos);
                        if (!voxelShape.isEmpty()) {
                            for (Box box2 : voxelShape.getBoundingBoxes()) {
                                double d3 = blockPos.getY() + box2.maxY;
                                if (d3 <= d + 0.001 && d3 > d2) {
                                    d2 = d3;
                                }
                            }
                        }

                        FluidState fluidState = mc.world.getFluidState(blockPos);
                        if (!fluidState.isEmpty()) {
                            VoxelShape voxelShape2 = fluidState.getShape(mc.world, blockPos);
                            if (!voxelShape2.isEmpty()) {
                                for (Box box3 : voxelShape2.getBoundingBoxes()) {
                                    double d4 = blockPos.getY() + box3.maxY;
                                    if (d4 <= d + 0.001 && d4 > d2) {
                                        d2 = d4;
                                    }
                                }
                            }
                        }
                    }
                }

                if (d2 != Double.NEGATIVE_INFINITY) {
                    return Math.max(0.0, d - d2);
                }
            }

            double d5 = raycastDistanceDown(livingEntity, livingEntity.getX(), d, livingEntity.getZ());
            double d6 = Math.min(d5, raycastDistanceDown(livingEntity, box.minX + 0.001, d, box.minZ + 0.001));
            double d7 = Math.min(d6, raycastDistanceDown(livingEntity, box.minX + 0.001, d, box.maxZ - 0.001));
            double d8 = Math.min(d7, raycastDistanceDown(livingEntity, box.maxX - 0.001, d, box.minZ + 0.001));
            return Math.min(d8, raycastDistanceDown(livingEntity, box.maxX - 0.001, d, box.maxZ - 0.001));
        } else {
            return 0.0;
        }
    }

    private static double raycastDistanceDown(LivingEntity livingEntity, double d, double d2, double d3) {
        Vec3d vec3d = new Vec3d(d, d2, d3);
        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d.add(0.0, -256.0, 0.0), ShapeType.COLLIDER, FluidHandling.ANY, livingEntity));
        return blockHitResult.getType() == Type.MISS ? 256.0 : vec3d.y - blockHitResult.getPos().y;
    }

    public static boolean isInLiquid() {
        return mc.player != null && (mc.player.isTouchingWater() || mc.player.isInLava());
    }

    public static boolean isFallDamageExempt() {
        return mc.player == null
            ? true
            : isInLiquid()
                || mc.player.isClimbing()
                || mc.player.isGliding()
                || mc.player.hasVehicle()
                || mc.player.getAbilities().flying
                || mc.player.inPowderSnow
                || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || ServerManager.isInCobweb()
                || ServerManager.isInBerryBush();
    }

    private FallUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
