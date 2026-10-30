package tech.sadnes.client.util.player;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.Rotation;

public final class RaycastUtil implements IMinecraft {
    private RaycastUtil() {
    }

    public static Entity raycastEntity(Entity entity2, float f, float f2, double d) {
        Entity entity3 = mc.getCameraEntity();
        if (entity3 != null && mc.world != null && entity2 != null) {
            Vec3d vec3d = entity3.getEyePos();
            Vec3d vec3d2 = getRotationVector(f2, f);
            Vec3d vec3d3 = vec3d.add(vec3d2.multiply(d));
            Box box = entity2.getBoundingBox().expand(entity2.getTargetingMargin());
            EntityHitResult entityHitResult = raycastEntities(entity3, vec3d, vec3d3, box, entity4 -> !entity4.isSpectator() && entity4.canHit(), d);
            return entityHitResult != null ? entityHitResult.getEntity() : null;
        } else {
            return null;
        }
    }

    public static boolean isLookingAt(LivingEntity livingEntity, float f, float f2, float f3, boolean flag) {
        return isLookingAtEntity(livingEntity, f, f2, f3, flag);
    }

    public static boolean isLookingAtEntity(Entity entity2, float f, float f2, float f3, boolean flag) {
        Entity entity3 = mc.getCameraEntity();
        if (entity3 != null && mc.world != null && entity2 != null) {
            double d = f3 * f3;
            Vec3d vec3d = entity3.getEyePos();
            Vec3d vec3d2 = getDirection(f, f2).multiply(f3);
            Vec3d vec3d3 = vec3d.add(vec3d2);
            Box box = entity2.getBoundingBox();
            if (!flag) {
                HitResult hitResult = raycast(vec3d, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE);
                if (hitResult.getType() == Type.BLOCK) {
                    double d2 = vec3d.squaredDistanceTo(hitResult.getPos());
                    if (d2 < d) {
                        d = d2;
                    }
                }
            }

            EntityHitResult entityHitResult = ProjectileUtil.raycast(entity3, vec3d, vec3d3, box, entity4 -> !entity4.isSpectator() && entity4.isAlive() && entity4 == entity2, d);
            return entityHitResult != null;
        } else {
            return false;
        }
    }

    public static boolean isLookingAtBox(Box box, float f, float f2, float f3) {
        Entity entity = mc.getCameraEntity();
        if (entity != null && mc.world != null && box != null) {
            Vec3d vec3d = entity.getEyePos();
            Vec3d vec3d2 = getDirection(f, f2).multiply(f3);
            Vec3d vec3d3 = vec3d.add(vec3d2);
            return box.raycast(vec3d, vec3d3).isPresent();
        } else {
            return false;
        }
    }

    public static boolean isLookingAtFrom(Vec3d vec3d, LivingEntity livingEntity, float f, float f2, float f3, boolean flag) {
        if (mc.world != null && mc.player != null && vec3d != null && livingEntity != null) {
            double d = f3 * f3;
            Vec3d vec3d2 = getDirection(f, f2).multiply(f3);
            Vec3d vec3d3 = vec3d.add(vec3d2);
            Box box = livingEntity.getBoundingBox();
            EntityHitResult entityHitResult = ProjectileUtil.raycast(
                mc.player, vec3d, vec3d3, box, entity2 -> !entity2.isSpectator() && entity2.isAlive() && entity2 == livingEntity, d
            );
            return entityHitResult != null;
        } else {
            return false;
        }
    }

    public static EntityHitResult raycastEntities(Entity entity, Vec3d vec3d, Vec3d vec3d2, Box box, Predicate<Entity> predicate, double d) {
        if (mc.world == null) {
            return null;
        }

        double d2 = d;
        Entity entity2 = null;
        Vec3d vec3d3 = null;

        for (Entity entity3 : mc.world.getOtherEntities(entity, box, predicate)) {
            Box box2 = entity3.getBoundingBox().expand(entity3.getTargetingMargin());
            Optional<Vec3d> optionalVec3d = box2.raycast(vec3d, vec3d2);
            if (box2.contains(vec3d) || optionalVec3d.isPresent()) {
                double d3 = optionalVec3d.map(vec3d::distanceTo).orElse(0.0);
                if ((d3 < d2 || d2 == 0.0) && entity3.getRootVehicle() != entity.getRootVehicle()) {
                    entity2 = entity3;
                    vec3d3 = optionalVec3d.orElse(vec3d);
                    d2 = d3;
                }
            }
        }

        return entity2 == null ? null : new EntityHitResult(entity2, vec3d3);
    }

    public static HitResult raycastWithEntities(double d, float f, float f2, Entity entity2, boolean flag) {
        if (mc.player != null && mc.world != null && entity2 != null) {
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = getRotationVector(f2, f);
            Vec3d vec3d3 = vec3d.add(vec3d2.multiply(d * 40.0));
            HitResult hitResult = raycast(vec3d, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE);
            double d2 = hitResult.getPos().squaredDistanceTo(vec3d);
            Box box = entity2.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0);
            EntityHitResult entityHitResult = ProjectileUtil.raycast(entity2, vec3d, vec3d3, box, entity3 -> !entity3.isSpectator() && entity3.isAlive() && entity3.canHit(), 160.0);
            return entityHitResult == null || !flag && !(entityHitResult.getPos().squaredDistanceTo(vec3d) < d2) ? hitResult : entityHitResult;
        } else {
            return BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
        }
    }

    public static boolean rayIntersectsEntity(float f, float f2, double d, Entity entity) {
        if (mc.player != null && entity != null) {
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = getRotationVector(f2, f);
            Vec3d vec3d3 = vec3d.add(vec3d2.multiply(d));
            Box box = entity.getBoundingBox();
            return box.contains(vec3d) || box.raycast(vec3d, vec3d3).isPresent();
        } else {
            return false;
        }
    }

    public static boolean isYawFacing(LivingEntity livingEntity, float f) {
        return isYawFacing(livingEntity, f, 6.0);
    }

    public static boolean isYawFacing(LivingEntity livingEntity, float f, double d) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ()).add(0.0, livingEntity.getHeight() / 2.0F, 0.0).subtract(mc.player.getEyePos()).normalize();
            float f2 = (float)Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0F;
            return rayIntersectsEntity(f2, f, d, livingEntity);
        } else {
            return false;
        }
    }

    public static boolean isPitchFacing(LivingEntity livingEntity, float f) {
        return isPitchFacing(livingEntity, f, 6.0);
    }

    public static boolean isPitchFacing(LivingEntity livingEntity, float f, double d) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ()).add(0.0, livingEntity.getHeight() / 2.0F, 0.0).subtract(mc.player.getEyePos()).normalize();
            float f2 = (float)(-Math.toDegrees(Math.atan2(vec3d.y, Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z))));
            return rayIntersectsEntity(f, f2, d, livingEntity);
        } else {
            return false;
        }
    }

    public static boolean isFacingServerRotation(LivingEntity livingEntity) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ()).add(0.0, livingEntity.getHeight() / 2.0F, 0.0).subtract(mc.player.getEyePos()).normalize();
            float f = (float)Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0F;
            return rayIntersectsEntity(f, Rotation.getCameraPitch(), 3.1, livingEntity);
        } else {
            return false;
        }
    }

    public static HitResult raycastBlocks(float f, float f2, double d, boolean flag) {
        return flag ? raycastBlocks(f, f2, d, FluidHandling.ANY) : raycastBlocks(f, f2, d, FluidHandling.NONE);
    }

    public static HitResult raycastBlocks(float f, float f2, double d, FluidHandling fluidHandling) {
        if (mc.player != null && mc.world != null) {
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = getRotationVector(f2, f);
            Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
            return mc.world.raycast(new RaycastContext(vec3d, vec3d3, ShapeType.OUTLINE, fluidHandling, mc.player));
        } else {
            return BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
        }
    }

    public static Vec3d getRotationVector(float f, float f2) {
        float f3 = -f2 * (float) (Math.PI / 180.0) - (float) Math.PI;
        float f4 = -f * (float) (Math.PI / 180.0);
        float f5 = MathHelper.cos(f3);
        float f6 = MathHelper.sin(f3);
        float f7 = -MathHelper.cos(f4);
        float f8 = MathHelper.sin(f4);
        return new Vec3d(f6 * f7, f8, f5 * f7);
    }

    public static Vec3d getDirection(float f, float f2) {
        float f3 = (float)Math.toRadians(f2);
        float f4 = (float)Math.toRadians(-f);
        float f5 = MathHelper.cos(f4);
        float f6 = MathHelper.sin(f4);
        float f7 = MathHelper.cos(f3);
        float f8 = MathHelper.sin(f3);
        return new Vec3d(f6 * f7, -f8, f5 * f7);
    }

    public static HitResult raycast(Vec3d vec3d, Vec3d vec3d2, ShapeType shapeType, FluidHandling fluidHandling) {
        AttackAura attackAura = AttackAura.getInstance();
        return Sadnes.panic && ServerManager.isSpookyTime() && attackAura != null && !attackAura.getThroughWallsSetting().getValue()
            ? raycast(vec3d, vec3d2, shapeType, fluidHandling, true)
            : raycast(vec3d, vec3d2, shapeType, fluidHandling, false);
    }

    public static HitResult raycast(Vec3d vec3d, Vec3d vec3d2, ShapeType shapeType, FluidHandling fluidHandling, boolean flag) {
        if (mc.world == null || mc.player == null) {
            return BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
        }

        if (!flag) {
            return mc.world.raycast(new RaycastContext(vec3d, vec3d2, shapeType, fluidHandling, mc.player));
        }

        Vec3d vec3d3 = vec3d;
        BlockHitResult blockHitResult = null;
        Vec3d vec3d4 = vec3d2.subtract(vec3d).normalize();

        for (int i = 0; i < 40; i++) {
            blockHitResult = mc.world.raycast(new RaycastContext(vec3d3, vec3d2, shapeType, fluidHandling, mc.player));
            if (blockHitResult.getType() != Type.BLOCK) {
                return blockHitResult;
            }

            BlockPos blockPos;
            VoxelShape voxelShape;
            label67: {
                BlockState blockState;
                label66: {
                    blockPos = blockHitResult.getBlockPos();
                    blockState = mc.world.getBlockState(blockPos);
                    voxelShape = blockState.getCollisionShape(mc.world, blockPos);
                    if (!voxelShape.isEmpty()) {
                        Box box = voxelShape.getBoundingBox();
                        if (box.maxX - box.minX >= 0.99 && box.maxY - box.minY >= 0.99 && box.maxZ - box.minZ >= 0.99) {
                            break label66;
                        }
                    }

                    if (!blockState.isSolidBlock(mc.world, blockPos)) {
                        break label67;
                    }
                }

                Block block = blockState.getBlock();
                if (!(block instanceof StairsBlock)
                    && !(block instanceof SlabBlock)
                    && !(block instanceof FenceBlock)
                    && !(block instanceof WallBlock)
                    && !(block instanceof PaneBlock)
                    && !(block instanceof TrapdoorBlock)
                    && !(block instanceof DoorBlock)) {
                    return blockHitResult;
                }
            }

            if (!voxelShape.isEmpty()) {
                Box box2 = voxelShape.getBoundingBox().offset(blockPos);
                Optional optional = box2.raycast(vec3d2, vec3d);
                if (!optional.isPresent()) {
                    break;
                }

                vec3d3 = ((Vec3d)optional.get()).add(vec3d4.multiply(0.01));
            } else {
                vec3d3 = blockHitResult.getPos().add(vec3d4.multiply(0.01));
            }

            if (vec3d3.squaredDistanceTo(vec3d) >= vec3d2.squaredDistanceTo(vec3d)) {
                break;
            }
        }

        return blockHitResult != null ? blockHitResult : BlockHitResult.createMissed(vec3d2, Direction.UP, BlockPos.ofFloored(vec3d2));
    }

    public static boolean canSee(Vec3d vec3d, Vec3d vec3d2) {
        return mc.world == null ? false : raycast(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE).getType() != Type.BLOCK;
    }

    public static boolean isLookingAtSafe(Entity entity, float f, float f2, double d, boolean flag) {
        return entity == null ? false : isLookingAtEntity(entity, f, f2, (float)d, flag);
    }

    public static boolean rayIntersectsBox(Entity entity, Box box, float f, float f2, double d, boolean flag) {
        if (entity != null && mc.world != null && box != null) {
            Entity entity2 = mc.getCameraEntity();
            if (entity2 == null) {
                return false;
            }

            Vec3d vec3d = entity2.getEyePos();
            Vec3d vec3d2 = getDirection(f, f2);
            Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
            if (!flag) {
                HitResult hitResult = raycast(vec3d, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE);
                if (hitResult.getType() == Type.BLOCK) {
                    vec3d3 = hitResult.getPos();
                }
            }

            return box.raycast(vec3d, vec3d3).isPresent();
        } else {
            return false;
        }
    }

    public static boolean isLookingAtShrunk(Entity entity2, boolean flag, float f, float f2, double d, boolean flag2) {
        if (entity2 != null && mc.world != null) {
            Entity entity3 = mc.getCameraEntity();
            if (entity3 == null) {
                return false;
            }

            Vec3d vec3d = entity3.getEyePos();
            Vec3d vec3d2 = getDirection(f, f2);
            Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
            double d2 = d * d;
            if (!flag2) {
                HitResult hitResult = raycast(vec3d, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE);
                if (hitResult.getType() == Type.BLOCK) {
                    double d3 = vec3d.squaredDistanceTo(hitResult.getPos());
                    if (d3 < d2) {
                        d2 = d3;
                    }
                }
            }

            Box box = flag ? entity2.getBoundingBox().expand(-1.0) : entity2.getBoundingBox();
            EntityHitResult entityHitResult = ProjectileUtil.raycast(entity3, vec3d, vec3d3, box, entity4 -> !entity4.isSpectator() && entity4.isAlive() && entity4 == entity2, d2);
            return entityHitResult != null;
        } else {
            return false;
        }
    }
}
