package tech.sadnes.client.util.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.rotation.RotationUtil;

public final class AimPointUtil {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final double[][] FACE_OFFSETS = new double[][]{
        {0.0, 0.0}, {-0.25, 0.0}, {0.25, 0.0}, {0.0, -0.25}, {0.0, 0.25}, {-0.4, -0.4}, {-0.4, 0.4}, {0.4, -0.4}, {0.4, 0.4}
    };
    private static Entity cachedEntity;
    private static Box cachedBox;
    private static double cachedX;
    private static double cachedY;
    private static double cachedZ;
    private static List<Vec3d> cachedPoints;
    private static int pointIndex;
    private static int bandPointIndex;
    private static final StopWatch TIMER = new StopWatch();
    private static float yOffset = 0.433F;

    private AimPointUtil() {
    }

    public static int clamp(int i, int i2, int i3) {
        return Math.min(i3, Math.max(i, i2));
    }

    public static float clamp(float f, float f2, float f3) {
        return Math.min(f3, Math.max(f, f2));
    }

    public static double clamp(double d, double d2, double d3) {
        return Math.min(d3, Math.max(d, d2));
    }

    public static int lerp(int i, int i2, float f) {
        return i + (int)(f * (i2 - i));
    }

    public static float lerp(float f, float f2, float f3) {
        return f + f3 * (f2 - f);
    }

    public static double lerp(double d, double d2, double d3) {
        return d + d3 * (d2 - d);
    }

    public static Vec2f getRotationTo(Vec3d vec3d) {
        if (MC.player == null) {
            return new Vec2f(0.0F, 0.0F);
        }

        Vec3d vec3d2 = MC.player.getEyePos();
        Vec3d vec3d3 = vec3d.subtract(vec3d2);
        double d = MathHelper.sqrt((float)(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z));
        float f = (float)(Math.atan2(vec3d3.z, vec3d3.x) * 180.0 / Math.PI - 90.0);
        float f2 = (float)Math.toDegrees(-Math.atan2(vec3d3.y, d));
        return new Vec2f(f, f2);
    }

    public static HitResult raycast(Vec3d vec3d, Vec3d vec3d2, ShapeType shapeType, FluidHandling fluidHandling) {
        return RaycastUtil.raycast(vec3d, vec3d2, shapeType, fluidHandling);
    }

    public static BlockHitResult getBlockHit(BlockPos blockPos, float f) {
        return getBlockHit(blockPos, f, null);
    }

    public static BlockHitResult getBlockHit(BlockPos blockPos, float f, boolean flag) {
        BlockHitResult blockHitResult = getBlockHit(blockPos, f);
        return blockHitResult == null && flag ? getShapeHit(blockPos, f) : blockHitResult;
    }

    public static BlockHitResult getBlockHit(BlockPos blockPos, Direction direction, float f, boolean flag) {
        BlockHitResult blockHitResult = getBlockHit(blockPos, direction, f);
        return blockHitResult == null && flag ? getShapeHit(blockPos, direction, f) : blockHitResult;
    }

    public static BlockHitResult raycastBlock(BlockPos blockPos, float f, float f2, float f3) {
        if (MC.player != null && MC.world != null && blockPos != null) {
            VoxelShape voxelShape = MC.world.getBlockState(blockPos).getOutlineShape(MC.world, blockPos);
            if (voxelShape.isEmpty()) {
                return null;
            }

            Vec3d vec3d = MC.player.getEyePos();
            Vec3d vec3d2 = vec3d.add(RaycastUtil.getDirection(f, f2).multiply(f3));
            BlockHitResult blockHitResult = voxelShape.raycast(vec3d, vec3d2, blockPos);
            return blockHitResult != null && blockHitResult.getBlockPos().equals(blockPos) && vec3d.squaredDistanceTo(blockHitResult.getPos()) <= f3 * f3 ? blockHitResult : null;
        } else {
            return null;
        }
    }

    public static BlockHitResult getBlockHit(BlockPos blockPos, float f, Box box) {
        if (MC.player != null && MC.world != null && blockPos != null) {
            BlockHitResult blockHitResult = null;
            double d = Double.MAX_VALUE;

            for (Direction direction : Direction.values()) {
                BlockHitResult blockHitResult2 = getBlockHit(blockPos, direction, f, box);
                if (blockHitResult2 != null) {
                    double d2 = MC.player.getEyePos().squaredDistanceTo(blockHitResult2.getPos());
                    if (!(d2 >= d)) {
                        d = d2;
                        blockHitResult = blockHitResult2;
                    }
                }
            }

            return blockHitResult;
        } else {
            return null;
        }
    }

    public static BlockHitResult getBlockHit(BlockPos blockPos, Direction direction, float f) {
        return getBlockHit(blockPos, direction, f, null);
    }

    public static BlockHitResult getBlockHit(BlockPos blockPos, Direction direction, float f, Box box) {
        if (MC.player != null && MC.world != null && blockPos != null && direction != null) {
            VoxelShape voxelShape = MC.world.getBlockState(blockPos).getOutlineShape(MC.world, blockPos);
            if (voxelShape.isEmpty()) {
                return null;
            }

            Box box2 = voxelShape.getBoundingBox().offset(blockPos);
            Vec3d vec3d = MC.player.getEyePos();
            BlockHitResult blockHitResult = checkFaceHit(blockPos, direction, f, box, getClosestFacePoint(box2, direction, vec3d));
            if (blockHitResult != null) {
                return blockHitResult;
            }

            BlockHitResult blockHitResult2 = null;
            double d = Double.MAX_VALUE;

            for (double[] doubles : FACE_OFFSETS) {
                Vec3d vec3d2 = getFacePoint(box2, direction, doubles[0], doubles[1]);
                double d2 = vec3d.squaredDistanceTo(vec3d2);
                if (!(d2 >= d)) {
                    BlockHitResult blockHitResult3 = checkFaceHit(blockPos, direction, f, box, vec3d2);
                    if (blockHitResult3 != null) {
                        d = d2;
                        blockHitResult2 = blockHitResult3;
                    }
                }
            }

            return blockHitResult2;
        } else {
            return null;
        }
    }

    private static BlockHitResult checkFaceHit(BlockPos blockPos, Direction direction, float f, Box box, Vec3d vec3d) {
        Vec3d vec3d2 = MC.player.getEyePos();
        if (!isFaceReachable(vec3d2, vec3d, direction, f)) {
            return null;
        }

        if (box != null && box.raycast(vec3d2, vec3d).isPresent()) {
            return null;
        }

        Vec2f vec2f = RotationUtil.getRotations(vec3d2, vec3d);
        return RaycastUtil.raycastBlocks(vec2f.x, vec2f.y, f, false) instanceof BlockHitResult blockHitResult
                && blockHitResult.getBlockPos().equals(blockPos)
                && blockHitResult.getSide() == direction
            ? blockHitResult
            : null;
    }

    private static BlockHitResult getShapeHit(BlockPos blockPos, float f) {
        BlockHitResult blockHitResult = null;
        double d = Double.MAX_VALUE;

        for (Direction direction : Direction.values()) {
            BlockHitResult blockHitResult2 = getShapeHit(blockPos, direction, f);
            if (blockHitResult2 != null) {
                double d2 = MC.player.getEyePos().squaredDistanceTo(blockHitResult2.getPos());
                if (!(d2 >= d)) {
                    d = d2;
                    blockHitResult = blockHitResult2;
                }
            }
        }

        return blockHitResult;
    }

    private static BlockHitResult getShapeHit(BlockPos blockPos, Direction direction, float f) {
        if (MC.player != null && MC.world != null && blockPos != null && direction != null) {
            VoxelShape voxelShape = MC.world.getBlockState(blockPos).getOutlineShape(MC.world, blockPos);
            if (voxelShape.isEmpty()) {
                return null;
            }

            Box box = voxelShape.getBoundingBox().offset(blockPos);
            Vec3d vec3d = MC.player.getEyePos();
            BlockHitResult blockHitResult = raycastShape(voxelShape, blockPos, direction, f, getClosestFacePoint(box, direction, vec3d));
            if (blockHitResult != null) {
                return blockHitResult;
            }

            BlockHitResult blockHitResult2 = null;
            double d = Double.MAX_VALUE;

            for (double[] doubles : FACE_OFFSETS) {
                Vec3d vec3d2 = getFacePoint(box, direction, doubles[0], doubles[1]);
                double d2 = vec3d.squaredDistanceTo(vec3d2);
                if (!(d2 >= d)) {
                    BlockHitResult blockHitResult3 = raycastShape(voxelShape, blockPos, direction, f, vec3d2);
                    if (blockHitResult3 != null) {
                        d = d2;
                        blockHitResult2 = blockHitResult3;
                    }
                }
            }

            return blockHitResult2;
        } else {
            return null;
        }
    }

    private static BlockHitResult raycastShape(VoxelShape voxelShape, BlockPos blockPos, Direction direction, float f, Vec3d vec3d) {
        Vec3d vec3d2 = MC.player.getEyePos();
        if (!isFaceReachable(vec3d2, vec3d, direction, f)) {
            return null;
        }

        Vec3d vec3d3 = vec3d.subtract(Vec3d.of(direction.getVector()).multiply(0.01));
        BlockHitResult blockHitResult = voxelShape.raycast(vec3d2, vec3d3, blockPos);
        return blockHitResult != null && blockHitResult.getBlockPos().equals(blockPos) && blockHitResult.getSide() == direction ? blockHitResult : null;
    }

    private static boolean isFaceReachable(Vec3d vec3d, Vec3d vec3d2, Direction direction, float f) {
        return vec3d.squaredDistanceTo(vec3d2) <= f * f && vec3d.subtract(vec3d2).dotProduct(Vec3d.of(direction.getVector())) > 0.0;
    }

    private static Vec3d getClosestFacePoint(Box box, Direction direction, Vec3d vec3d) {
        double d = MathHelper.clamp(vec3d.x, box.minX + 0.05, box.maxX - 0.05);
        double d2 = MathHelper.clamp(vec3d.y, box.minY + 0.05, box.maxY - 0.05);
        double d3 = MathHelper.clamp(vec3d.z, box.minZ + 0.05, box.maxZ - 0.05);
        switch (direction) {
            case DOWN:
                return new Vec3d(d, box.minY, d3);
            case UP:
                return new Vec3d(d, box.maxY, d3);
            case NORTH:
                return new Vec3d(d, d2, box.minZ);
            case SOUTH:
                return new Vec3d(d, d2, box.maxZ);
            case WEST:
                return new Vec3d(box.minX, d2, d3);
            case EAST:
                return new Vec3d(box.maxX, d2, d3);
            default:
                throw new MatchException(null, null);
        }
    }

    private static Vec3d getFacePoint(Box box, Direction direction, double d, double d2) {
        double d3 = lerpInside(box.minX, box.maxX, d);
        double d4 = lerpInside(box.minY, box.maxY, d);
        double d5 = lerpInside(box.minY, box.maxY, d2);
        double d6 = lerpInside(box.minZ, box.maxZ, d2);
        switch (direction) {
            case DOWN:
                return new Vec3d(d3, box.minY, d6);
            case UP:
                return new Vec3d(d3, box.maxY, d6);
            case NORTH:
                return new Vec3d(d3, d5, box.minZ);
            case SOUTH:
                return new Vec3d(d3, d5, box.maxZ);
            case WEST:
                return new Vec3d(box.minX, d4, d6);
            case EAST:
                return new Vec3d(box.maxX, d4, d6);
            default:
                throw new MatchException(null, null);
        }
    }

    private static double lerpInside(double d, double d2, double d3) {
        return MathHelper.lerp(0.5 + d3, d + 0.001, d2 - 0.001);
    }

    private static double horizontalDistance(ClientPlayerEntity clientPlayerEntity, double d, double d2) {
        double d3 = clientPlayerEntity.getX() - d;
        double d4 = clientPlayerEntity.getZ() - d2;
        return Math.sqrt(d3 * d3 + d4 * d4);
    }

    public static boolean canSee(ClientPlayerEntity clientPlayerEntity, double d, double d2, double d3) {
        if (MC.world != null && clientPlayerEntity != null) {
            Vec3d vec3d = new Vec3d(d, d2, d3);
            return raycast(clientPlayerEntity.getEyePos(), vec3d, ShapeType.COLLIDER, FluidHandling.NONE).getType() != Type.BLOCK;
        } else {
            return false;
        }
    }

    public static boolean canSee(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d) {
        return MC.world != null && clientPlayerEntity != null && vec3d != null
            ? raycast(clientPlayerEntity.getEyePos(), vec3d, ShapeType.COLLIDER, FluidHandling.NONE).getType() != Type.BLOCK
            : false;
    }

    public static boolean canSee(Vec3d vec3d, Vec3d vec3d2) {
        return MC.world != null && vec3d != null && vec3d2 != null ? raycast(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE).getType() != Type.BLOCK : false;
    }

    private static boolean canSeeWithSpread(ClientPlayerEntity clientPlayerEntity, Vec3d vec3d, float f) {
        return f == 0.0F
            ? canSee(clientPlayerEntity, vec3d.x, vec3d.y, vec3d.z)
            : canSee(clientPlayerEntity, vec3d.x, vec3d.y, vec3d.z)
                && canSee(clientPlayerEntity, vec3d.x, vec3d.y + f, vec3d.z)
                && canSee(clientPlayerEntity, vec3d.x, vec3d.y - f, vec3d.z)
                && canSee(clientPlayerEntity, vec3d.x + f, vec3d.y, vec3d.z)
                && canSee(clientPlayerEntity, vec3d.x - f, vec3d.y, vec3d.z)
                && canSee(clientPlayerEntity, vec3d.x, vec3d.y, vec3d.z + f)
                && canSee(clientPlayerEntity, vec3d.x, vec3d.y, vec3d.z - f);
    }

    public static List<Vec3d> getVisiblePoints(Entity entity) {
        return entity != null ? getVisiblePoints(entity, entity.getBoundingBox()) : getVisiblePoints(entity, null);
    }

    public static List<Vec3d> getVisiblePoints(Entity entity, Box box) {
        if (entity != null && MC.player != null) {
            ClientPlayerEntity clientPlayerEntity = MC.player;
            double d = clientPlayerEntity.getX();
            double d2 = clientPlayerEntity.getY();
            double d3 = clientPlayerEntity.getZ();
            if (entity == cachedEntity && boxesEqual(box, cachedBox) && d == cachedX && d2 == cachedY && d3 == cachedZ) {
                return cachedPoints;
            }

            double[] doubles = new double[]{entity.getWidth() - 0.02F * 2.0, entity.getHeight() - 0.02F * 2.0, (entity.getHeight() - 0.02F * 2.0) / 1.05};
            double[] doubles2 = new double[]{entity.getX(), entity.getY(), entity.getZ()};
            double[] doubles3 = new double[]{doubles2[0] + doubles[0] / 2.0, doubles2[1] + doubles[1], doubles2[2] + doubles[0] / 2.0};
            doubles3 = new double[]{doubles2[0] - doubles[0] / 2.0, doubles2[1], doubles2[2] - doubles[0] / 2.0};
            double[] doubles4;
            double[] doubles5;
            if (box != null) {
                Box box2 = box.expand(-0.02F);
                doubles = new double[]{box2.maxX - box2.minX, box2.maxY - box2.minY, (box2.maxY - box2.minY) / 1.05};
                doubles2 = new double[]{box2.minX + doubles[0] / 2.0, box2.minY, box2.minZ + doubles[0] / 2.0};
                doubles4 = new double[]{box2.minX, box2.minY, box2.minZ};
                doubles5 = new double[]{box2.maxX, box2.maxY, box2.maxZ};
            } else {
                doubles5 = new double[]{doubles2[0] + doubles[0] / 2.0, doubles2[1] + doubles[1], doubles2[2] + doubles[0] / 2.0};
                doubles4 = new double[]{doubles2[0] - doubles[0] / 2.0, doubles2[1], doubles2[2] - doubles[0] / 2.0};
            }

            float f = (float)Math.sqrt(doubles[0] * doubles[0] + doubles[0] * doubles[0] + doubles[0] * doubles[0]) / 2.0F;
            float f2 = (1.0F - Math.min((float)distanceTo(clientPlayerEntity, doubles2[0], doubles2[1], doubles2[2]) / 5.0F, 1.0F))
                * Math.min((float)distanceTo(clientPlayerEntity, doubles2[0], clientPlayerEntity.getY(), doubles2[2]) / 0.6F, 1.0F);
            int i = lerp(5, 14, f2);
            int i2 = lerp(9, 27, f2);
            double d4 = clientPlayerEntity.getEyeHeight(clientPlayerEntity.getPose());
            float f3 = f * f;
            float f4 = i > 1 ? 1.0F / (i - 1) : 1.0F;
            float f5 = i2 > 1 ? 1.0F / (i2 - 1) : 1.0F;
            ArrayList arrayList = new ArrayList();

            for (int i3 = 0; i3 < i; i3++) {
                boolean flag = i3 == 0 || i3 == i - 1;
                double d5 = lerp(doubles4[0], doubles5[0], i3 * f4);

                for (int i4 = 0; i4 < i; i4++) {
                    boolean flag2 = i4 == 0 || i4 == i - 1;
                    double d6 = lerp(doubles4[2], doubles5[2], i4 * f4);

                    for (int i5 = 0; i5 < i2; i5++) {
                        if (i5 == 0 || i5 == i2 - 1 || flag || flag2) {
                            double d7 = lerp(doubles4[1], doubles5[1], i5 * f5);
                            double d8 = d5 - d;
                            double d9 = d7 - d4 - d2;
                            double d10 = d6 - d3;
                            if (!(d8 * d8 + d9 * d9 + d10 * d10 < f3)) {
                                Vec3d vec3d = new Vec3d(d5, d7, d6);
                                if (canSeeWithSpread(clientPlayerEntity, vec3d, 0.0F)) {
                                    arrayList.add(vec3d);
                                }
                            }
                        }
                    }
                }
            }

            cachedEntity = entity;
            cachedBox = box;
            cachedX = d;
            cachedY = d2;
            cachedZ = d3;
            cachedPoints = arrayList;
            return arrayList;
        } else {
            return null;
        }
    }

    private static boolean boxesEqual(Box box, Box box2) {
        if (box == box2) {
            return true;
        } else {
            return box == null || box2 == null
                ? false
                : box.minX == box2.minX
                    && box.minY == box2.minY
                    && box.minZ == box2.minZ
                    && box.maxX == box2.maxX
                    && box.maxY == box2.maxY
                    && box.maxZ == box2.maxZ;
        }
    }

    public static boolean hasVisiblePoint(Entity entity, Box box) {
        if (entity != null && MC.player != null) {
            ClientPlayerEntity clientPlayerEntity = MC.player;
            double[] doubles = new double[]{entity.getWidth() - 0.02F * 2.0, entity.getHeight() - 0.02F * 2.0, (entity.getHeight() - 0.02F * 2.0) / 1.05};
            double[] doubles2 = new double[]{entity.getX(), entity.getY(), entity.getZ()};
            double[] doubles3;
            double[] doubles4;
            if (box != null) {
                Box box2 = box.expand(-0.02F);
                doubles = new double[]{box2.maxX - box2.minX, box2.maxY - box2.minY, (box2.maxY - box2.minY) / 1.05};
                doubles2 = new double[]{box2.minX + doubles[0] / 2.0, box2.minY, box2.minZ + doubles[0] / 2.0};
                doubles3 = new double[]{box2.minX, box2.minY, box2.minZ};
                doubles4 = new double[]{box2.maxX, box2.maxY, box2.maxZ};
            } else {
                doubles3 = new double[]{doubles2[0] - doubles[0] / 2.0, doubles2[1], doubles2[2] - doubles[0] / 2.0};
                doubles4 = new double[]{doubles2[0] + doubles[0] / 2.0, doubles2[1] + doubles[1], doubles2[2] + doubles[0] / 2.0};
            }

            float f = (float)Math.sqrt(doubles[0] * doubles[0] + doubles[0] * doubles[0] + doubles[0] * doubles[0]) / 2.0F;
            float f2 = (1.0F - Math.min((float)distanceTo(clientPlayerEntity, doubles2[0], doubles2[1], doubles2[2]) / 10.0F, 1.0F))
                * Math.min((float)distanceTo(clientPlayerEntity, doubles2[0], clientPlayerEntity.getY(), doubles2[2]) / 0.6F, 1.0F);
            int i = lerp(1, 4, f2);
            int i2 = lerp(1, 5, f2);
            double d = clientPlayerEntity.getEyeHeight(clientPlayerEntity.getPose());
            double d2 = clientPlayerEntity.getX();
            double d3 = clientPlayerEntity.getY();
            double d4 = clientPlayerEntity.getZ();
            float f3 = f * f;
            float f4 = i > 1 ? 1.0F / (i - 1) : 1.0F;
            float f5 = i2 > 1 ? 1.0F / (i2 - 1) : 1.0F;

            for (int i3 = 0; i3 < i; i3++) {
                boolean flag = i3 == 0 || i3 == i - 1;
                double d5 = lerp(doubles3[0], doubles4[0], i3 * f4);

                for (int i4 = 0; i4 < i; i4++) {
                    boolean flag2 = i4 == 0 || i4 == i - 1;
                    double d6 = lerp(doubles3[2], doubles4[2], i4 * f4);

                    for (int i5 = 0; i5 < i2; i5++) {
                        boolean flag3 = i5 == 0 || i5 == i2 - 1;
                        if (flag || flag2 || flag3) {
                            double d7 = lerp(doubles3[1], doubles4[1], i5 * f5);
                            double d8 = d5 - d2;
                            double d9 = d7 - d - d3;
                            double d10 = d6 - d4;
                            double d11 = d8 * d8 + d9 * d9 + d10 * d10;
                            if (!(d11 < f3)) {
                                Vec3d vec3d = new Vec3d(d5, d7, d6);
                                if (canSeeWithSpread(clientPlayerEntity, vec3d, 0.0F)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public static double distance(Vec3d vec3d, Vec3d vec3d2) {
        double d = vec3d.x - vec3d2.x;
        double d2 = vec3d.y - vec3d2.y;
        double d3 = vec3d.z - vec3d2.z;
        return Math.sqrt(d * d + d2 * d2 + d3 * d3);
    }

    public static Vec3d getRandomVisiblePoint(Entity entity, boolean flag) {
        if (entity != null && MC.player != null) {
            List<Vec3d> vec3ds = getVisiblePoints(entity, entity.getBoundingBox().expand(-0.2F));
            if (vec3ds != null && !vec3ds.isEmpty()) {
                List<Vec3d> vec3ds2 = vec3ds.stream().filter(vec3d -> canSee(MC.player, vec3d)).collect(Collectors.toList());
                if (vec3ds2.isEmpty()) {
                    vec3ds2 = vec3ds;
                }

                if (flag || vec3ds2.size() < pointIndex) {
                    pointIndex = (int)(vec3ds2.size() * Math.random());
                }

                return (Vec3d)vec3ds2.get(Math.max(0, Math.min(vec3ds2.size() - 1, pointIndex)));
            } else {
                return getAimPoint(entity, getDefaultRange(), true);
            }
        } else {
            return null;
        }
    }

    public static Vec3d getRandomBandPoint(Entity entity, float f, boolean flag) {
        if (entity != null && MC.player != null) {
            List<Vec3d> vec3ds = getBandPoints(entity, f);
            if (vec3ds.isEmpty()) {
                return getAimPoint(entity, getDefaultRange(), true);
            }

            List<Vec3d> vec3ds2 = vec3ds.stream().filter(vec3d -> canSee(MC.player, vec3d)).collect(Collectors.toList());
            if (vec3ds2.isEmpty()) {
                vec3ds2 = vec3ds;
            }

            if (flag || vec3ds2.size() < bandPointIndex) {
                bandPointIndex = (int)(vec3ds2.size() * Math.random());
            }

            return (Vec3d)vec3ds2.get(Math.max(0, Math.min(vec3ds2.size() - 1, bandPointIndex)));
        } else {
            return null;
        }
    }

    public static Vec3d getAimPoint(Entity entity, float f, boolean flag) {
        if (entity != null && MC.player != null) {
            Box box = entity.getBoundingBox();
            double[] doubles = new double[]{entity.getWidth(), entity.getHeight(), entity.getHeight() / 1.05F};
            double[] doubles2 = new double[]{entity.getX(), entity.getY(), entity.getZ()};
            if (box != null) {
                doubles = new double[]{box.maxX - box.minX, box.maxY - box.minY, (box.maxY - box.minY) / 1.1F};
                doubles2 = new double[]{box.minX + doubles[0] / 2.0, box.minY, box.minZ + doubles[0] / 2.0};
            }

            double[] doubles3 = new double[]{MC.player.getY() - doubles2[1], horizontalDistance(MC.player, doubles2[0], doubles2[2])};
            double d = clamp(Easings.QUART_OUT.ease((doubles3[1] - doubles[0] / 2.0) / (5.0 + doubles[0] / 2.0)), 0.1, 0.95);
            double d2 = clamp(d * d, 0.0, 1.0);
            double d3 = clamp(doubles[2] / 2.0 * d2 + doubles[2] / 2.0 * clamp(doubles3[0] + d2, 0.0, 1.0), 0.0, doubles[2]);
            Vec3d vec3d = new Vec3d(doubles2[0], doubles2[1] + d3, doubles2[2]);
            if (!flag && !canSee(MC.player, vec3d)) {
                vec3d = vec3d.add(0.0, -d3, 0.0);
            }

            float[] floats = RotationUtil.getRotationArrayTo(vec3d);
            if (!(doubles[1] <= 1.0) && (flag || !canSee(MC.player, vec3d) || !RaycastUtil.isLookingAtEntity(entity, floats[0], floats[1], f, true))) {
                List<Vec3d> vec3ds = getVisiblePoints(entity, box);
                float f2 = 1.0F - (float)Math.max(Math.min((doubles3[1] - 2.0) / 3.0, 1.0), 0.0);
                Vec3d vec3d2 = new Vec3d(MC.player.getX(), MC.player.getY() + 0.6F + lerp((float)d3, (float)(d3 / 2.5), f2), MC.player.getZ());
                if (vec3ds != null && vec3ds.size() > 1) {
                    vec3ds.sort(Comparator.comparing(vec3d3 -> distance(vec3d2, vec3d3)));
                }

                return vec3ds != null && !vec3ds.isEmpty() ? (Vec3d)vec3ds.get(0) : vec3d;
            } else {
                return vec3d;
            }
        } else {
            return MC.player != null ? MC.player.getEyePos() : Vec3d.ZERO;
        }
    }

    public static Vec3d getAimPoint(Entity entity, boolean flag) {
        if (entity != null && MC.player != null) {
            Box box = entity.getBoundingBox();
            double[] doubles = new double[]{box.maxX - box.minX, box.maxY - box.minY, (box.maxY - box.minY) / 1.1F};
            double[] doubles2 = new double[]{box.minX + doubles[0] / 2.0, box.minY, box.minZ + doubles[0] / 2.0};
            double[] doubles3 = new double[]{MC.player.getY() - doubles2[1], horizontalDistance(MC.player, doubles2[0], doubles2[2])};
            boolean flag2;
            if (!CombatUtil.isUnderLowCeiling()) {
                flag2 = !MoveUtil.isMoving();
            } else {
                flag2 = true;
            }

            boolean flag3;
            if (entity instanceof LivingEntity) {
                flag3 = ServerManager.isIntersecting((LivingEntity)entity);
            } else {
                flag3 = false;
            }

            double d = doubles[2] * (flag2 && flag3 ? 0.5F : 1.0F);
            Vec3d vec3d = new Vec3d(doubles2[0], doubles2[1] + d, doubles2[2]);
            if (!flag && !canSee(MC.player, vec3d)) {
                vec3d = vec3d.add(0.0, -d, 0.0);
            }

            if (!(doubles[1] <= 1.0) && (flag || !canSee(MC.player, vec3d) || !(CombatUtil.getDistance(entity) <= getDefaultRange()))) {
                List<Vec3d> vec3ds = getVisiblePoints(entity, box);
                float f = 1.0F - (float)Math.max(Math.min((doubles3[1] - 2.0) / 3.0, 1.0), 0.0);
                Vec3d vec3d2 = new Vec3d(MC.player.getX(), MC.player.getY() + 0.6F + lerp((float)d, (float)(d / 2.5), f), MC.player.getZ());
                if (vec3ds != null && vec3ds.size() > 1) {
                    vec3ds.sort(Comparator.comparing(vec3d3 -> distance(vec3d2, vec3d3)));
                }

                return vec3ds != null && !vec3ds.isEmpty() ? (Vec3d)vec3ds.get(0) : vec3d;
            } else {
                return vec3d;
            }
        } else {
            return MC.player != null ? MC.player.getEyePos() : Vec3d.ZERO;
        }
    }

    public static List<Vec3d> getBandPoints(Entity entity, float f) {
        if (entity != null && MC.player != null) {
            Box box = entity.getBoundingBox().expand(-0.1);
            ArrayList arrayList = new ArrayList();
            float f2 = clamp(f, 0.0F, 1.0F);
            double d = box.maxY - box.minY;
            double d2 = d / 4.0;
            double d3 = lerp(0.0, d - d2, f2);
            double d4 = box.minY + d3;
            double d5 = d4 + d2;

            for (int i = 0; i < 5; i++) {
                double d6 = lerp(box.minX, box.maxX, (float)i / 4);

                for (int i2 = 0; i2 < 5; i2++) {
                    double d7 = lerp(box.minZ, box.maxZ, (float)i2 / 4);

                    for (int i3 = 0; i3 < 5; i3++) {
                        double d8 = lerp(d4, d5, (float)i3 / 4);
                        Vec3d vec3d = new Vec3d(d6, d8, d7);
                        if (canSee(MC.player, vec3d)) {
                            arrayList.add(vec3d);
                        }
                    }
                }
            }

            if (arrayList.isEmpty()) {
                arrayList.add(new Vec3d(entity.getX(), d4 + d2 / 2.0, entity.getZ()));
            }

            return arrayList;
        } else {
            return new ArrayList<>();
        }
    }

    public static Vec3d getAimPoint(Entity entity) {
        if (MC.player != null && entity != null) {
            return distanceTo(MC.player, entity.getX(), MC.player.getY(), entity.getZ()) > entity.getWidth() * 1.37F
                ? getAimPoint(entity, 3.1F, true)
                : getAimPoint(entity, 3.1F, false);
        } else {
            return Vec3d.ZERO;
        }
    }

    public static Vec3d getLowAimPoint(Entity entity, float f, boolean flag) {
        if (entity != null && MC.player != null) {
            Box box = entity.getBoundingBox();
            double[] doubles = new double[]{entity.getWidth(), entity.getHeight(), entity.getHeight() / 1.05F};
            double[] doubles2 = new double[]{entity.getX(), entity.getY(), entity.getZ()};
            if (box != null) {
                doubles = new double[]{box.maxX - box.minX, box.maxY - box.minY, (box.maxY - box.minY) / 1.1F};
                doubles2 = new double[]{box.minX + doubles[0] / 2.0, box.minY, box.minZ + doubles[0] / 2.0};
            }

            double[] doubles3 = new double[]{MC.player.getY() - doubles2[1], horizontalDistance(MC.player, doubles2[0], doubles2[2])};
            double d = clamp(Easings.QUART_OUT.ease((doubles3[1] - doubles[0] / 2.0) / (5.0 + doubles[0] / 2.0)), 0.1, 0.95);
            double d2 = clamp(d * d, 0.0, 1.0);
            double d3 = flag
                ? clamp(MC.player.getEyeY() - doubles2[1], doubles[2] * 0.2, doubles[2] * 0.9)
                : clamp(doubles[2] * 0.15 + doubles[2] * 0.2 * d2, 0.0, doubles[2] * 0.4);
            Vec3d vec3d = new Vec3d(doubles2[0], doubles2[1] + d3, doubles2[2]);
            if (!canSee(MC.player, vec3d)) {
                vec3d = vec3d.add(0.0, -d3, 0.0);
            }

            float[] floats = RotationUtil.getRotationArrayTo(vec3d);
            if (!(doubles[1] <= 1.0) && (!canSee(MC.player, vec3d) || !RaycastUtil.isLookingAtEntity(entity, floats[0], floats[1], f, true))) {
                List<Vec3d> vec3ds = getVisiblePoints(entity, box);
                Vec3d vec3d2 = new Vec3d(MC.player.getX(), doubles2[1] + d3, MC.player.getZ());
                if (vec3ds != null && vec3ds.size() > 1) {
                    vec3ds.sort(Comparator.comparing(vec3d3 -> distance(vec3d2, vec3d3)));
                }

                return vec3ds != null && !vec3ds.isEmpty() ? (Vec3d)vec3ds.get(0) : vec3d;
            } else {
                return vec3d;
            }
        } else {
            return MC.player != null ? MC.player.getEyePos() : Vec3d.ZERO;
        }
    }

    public static Vec3d getAdaptiveAimPoint(LivingEntity livingEntity) {
        if (livingEntity == null) {
            return MC.player.getEyePos();
        }

        Box box = livingEntity.getBoundingBox();
        livingEntity.getBoundingBox();
        double d = (box.minX + box.maxX) * 0.5;
        double d2 = (box.minZ + box.maxZ) * 0.5;
        double d3 = CombatUtil.getRayDistance(livingEntity);
        double d4 = clamp(d3, 0.0, 3.0);
        double d5 = lerp(0.84, 0.56, d4 / 3.0);
        double d6 = clamp(MC.player.getY() + MC.player.getEyeHeight(MC.player.getPose()) * d5, box.minY, box.maxY);
        double d7 = clamp(d, box.minX + 0.02, box.maxX - 0.02);
        double d8 = clamp(d6, box.minY + 0.02, box.maxY - 0.02);
        double d9 = clamp(d2, box.minZ + 0.02, box.maxZ - 0.02);
        Vec3d vec3d = new Vec3d(d7, d8, d9);
        return !canSee(MC.player, vec3d) ? getAimPoint(livingEntity, (float)AttackAura.getInstance().getAttackRange(), false) : vec3d;
    }

    public static Vec2f getAimRotation(Entity entity, float f) {
        if (MC.player != null && entity != null) {
            if (distanceTo(MC.player, entity.getX(), MC.player.getY(), entity.getZ()) > entity.getWidth() * 1.37F) {
                return getRotationTo(getAimPoint(entity, f, true));
            }

            Vec3d vec3d = getAimPoint(entity, f, false);
            return getRotationTo(vec3d);
        } else {
            return new Vec2f(0.0F, 0.0F);
        }
    }

    public static Vec2f getAimRotation(Entity entity, boolean flag) {
        Vec3d vec3d = getAimPoint(entity, 3.1F, flag);
        return getRotationTo(vec3d);
    }

    public static Vec2f getAimRotation(Entity entity) {
        return getAimRotation(entity, true);
    }

    private static double distanceTo(Entity entity, double d, double d2, double d3) {
        return Math.sqrt(entity.squaredDistanceTo(d, d2, d3));
    }

    private static double distanceTo(Entity entity, Vec3d vec3d) {
        return Math.sqrt(entity.squaredDistanceTo(vec3d));
    }

    private static float getDefaultRange() {
        return 3.1F;
    }
}
