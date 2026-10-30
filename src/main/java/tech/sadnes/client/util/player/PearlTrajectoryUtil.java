package tech.sadnes.client.util.player;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector2f;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.compat.ProtocolUtil;

public final class PearlTrajectoryUtil implements IMinecraft {
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = 0.03;

    public static Vector2f calculateThrowRotation(Vec3d vec3d) {
        if (mc.player != null && vec3d != null) {
            Vec3d vec3d2 = mc.player.getEntityPos();
            double d = vec3d.x - vec3d2.x;
            double d2 = vec3d.y - (vec3d2.y + mc.player.getEyeHeight(mc.player.getPose()));
            double d3 = vec3d.z - vec3d2.z;
            double d4 = Math.sqrt(d * d + d3 * d3);
            double d5 = Math.sqrt(2.25 * 2.25 - 0.03 * (0.03 * d4 * d4 + 2.0 * d2 * 2.25));
            float f = (float)(-Math.toDegrees(Math.atan((2.25 - d5) / (0.03 * d4))));
            if (Float.isNaN(f)) {
                return null;
            }

            float f2 = (float)Math.toDegrees(Math.atan2(d3, d)) - 90.0F;
            return new Vector2f(f2, f);
        } else {
            return null;
        }
    }

    public static PearlTrajectoryUtil.Solution solveThrow(Vec3d vec3d, Entity entity) {
        Vector2f vector2f = calculateThrowRotation(vec3d);
        if (vector2f == null) {
            return null;
        }

        Vec3d vec3d2 = vec3d;
        PearlTrajectoryUtil.Solution solution = null;
        double d = Double.MAX_VALUE;

        for (int i = 0; i < 4 && d > 0.25; i++) {
            Vector2f vector2f2 = calculateThrowRotation(vec3d2);
            if (vector2f2 == null) {
                break;
            }

            PearlTrajectoryUtil.Flight flight2 = simulateThrow(vector2f2.x, vector2f2.y, entity);
            if (flight2 == null) {
                break;
            }

            double d2 = flight2.impact().distanceTo(vec3d);
            if (d2 < d) {
                d = d2;
                solution = new PearlTrajectoryUtil.Solution(vector2f2, flight2);
            }

            if (flight2.hitEntity() != null) {
                break;
            }

            vec3d2 = vec3d2.add(vec3d.subtract(flight2.impact()));
        }

        if (d < 1.0) {
            return solution;
        }

        PearlTrajectoryUtil.Solution solution2 = searchPitch(vec3d, solution != null ? solution.rotation().x : vector2f.x, entity);
        return solution2 != null && solution2.flight().impact().distanceTo(vec3d) < d ? solution2 : solution;
    }

    private static PearlTrajectoryUtil.Solution searchPitch(Vec3d vec3d, float f, Entity entity) {
        double d = Math.hypot(vec3d.x - mc.player.getX(), vec3d.z - mc.player.getZ());
        PearlTrajectoryUtil.Solution solution = null;
        double d2 = Double.MAX_VALUE;

        for (float f2 : new float[]{90.0F, -89.0F}) {
            float f3 = f2;
            float f4 = -40.0F;

            for (int i = 0; i < 8 && d2 > 0.5; i++) {
                float f5 = (f3 + f4) / 2.0F;
                PearlTrajectoryUtil.Flight flight = simulateThrow(f, f5, entity);
                double d3 = flight == null ? Double.MAX_VALUE : Math.hypot(flight.impact().x - mc.player.getX(), flight.impact().z - mc.player.getZ());
                if (d3 > d) {
                    f4 = f5;
                } else {
                    f3 = f5;
                }

                double d4 = flight == null ? Double.MAX_VALUE : flight.impact().distanceTo(vec3d);
                if (!(d4 >= d2)) {
                    d2 = d4;
                    solution = new PearlTrajectoryUtil.Solution(new Vector2f(f, f5), flight);
                }
            }
        }

        return solution;
    }

    public static PearlTrajectoryUtil.Solution solveThrowWithVelocity(Vec3d vec3d, Vec3d vec3d2, Predicate<PearlTrajectoryUtil.Flight> predicate) {
        if (mc.player == null) {
            return null;
        }

        boolean flag = !ProtocolUtil.isOlderThan("1.21.2");
        double d = flag ? vec3d2.x : 0.0;
        double d2 = mc.player.isOnGround() ? 0.0 : vec3d2.y;
        double d3 = flag ? vec3d2.z : 0.0;
        double d4 = vec3d.x - mc.player.getX();
        double d5 = vec3d.z - mc.player.getZ();
        double d6 = Math.hypot(d4, d5);
        double d7 = mc.player.getEyeY() - 0.1 - vec3d.y;

        for (double d8 : new double[]{0.0, -0.4, 0.4, -0.8, 0.8, -1.2, 1.2}) {
            if (!(d6 + d8 <= 0.0)) {
                double d9 = d6 < 1.0E-4 ? 1.0 : (d6 + d8) / d6;
                double d10 = 0.0;
                double d11 = 0.0;
                double d12 = 0.0;

                for (int i = 1; i <= 20; i++) {
                    d10 += Math.pow(0.99, i);
                    d11 = (d11 + 0.03) * 0.99;
                    d12 += d11;
                    double d13 = d4 * d9 / d10 - d;
                    double d14 = d5 * d9 / d10 - d3;
                    double d15 = Math.hypot(d13, d14);
                    if (!(d15 > 1.5)) {
                        double d16 = Math.sqrt(2.25 - d15 * d15);
                        float f = (float)Math.toDegrees(Math.atan2(d14, d13)) - 90.0F;
                        float f2 = (float)Math.toDegrees(Math.atan2(d16, d15));

                        for (float f3 : new float[]{f2, -f2}) {
                            double d17 = (f3 > 0.0F ? -d16 : d16) + d2;
                            double d18 = d7 + d17 * d10 - d12;
                            if (!(d18 < 0.0) && !(d18 > 4.0)) {
                                PearlTrajectoryUtil.Flight flight = simulateThrow(f, f3, null, vec3d2);
                                if (flight != null && predicate.test(flight)) {
                                    return new PearlTrajectoryUtil.Solution(new Vector2f(f, f3), flight);
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static Vec3d getImpactPos(float f, float f2) {
        PearlTrajectoryUtil.Flight flight = simulateThrow(f, f2, null);
        return flight == null ? null : flight.impact();
    }

    public static PearlTrajectoryUtil.Flight simulateThrow(float f, float f2, Entity entity) {
        return mc.player == null ? null : simulateThrow(f, f2, entity, mc.player.getVelocity());
    }

    public static PearlTrajectoryUtil.Flight simulateThrow(float f, float f2, Entity entity, Vec3d vec3d) {
        if (mc.player == null) {
            return null;
        }

        float f3 = f * (float) (Math.PI / 180.0);
        float f4 = f2 * (float) (Math.PI / 180.0);
        Vec3d vec3d2 = new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
        Vec3d vec3d3 = new Vec3d(-MathHelper.sin(f3) * MathHelper.cos(f4), -MathHelper.sin(f4), MathHelper.cos(f3) * MathHelper.cos(f4))
            .normalize()
            .multiply(1.5);
        boolean flag = !ProtocolUtil.isOlderThan("1.21.2");
        Vec3d vec3d4 = vec3d3.add(flag ? vec3d.x : 0.0, mc.player.isOnGround() ? 0.0 : vec3d.y, flag ? vec3d.z : 0.0);
        return simulateFlight(vec3d2, vec3d4, 0, mc.player, entity);
    }

    public static PearlTrajectoryUtil.Flight simulatePearl(EnderPearlEntity enderPearlEntity) {
        return simulateFlight(enderPearlEntity.getEntityPos(), enderPearlEntity.getVelocity(), enderPearlEntity.age, enderPearlEntity.getOwner(), null);
    }

    private static PearlTrajectoryUtil.Flight simulateFlight(Vec3d vec3d, Vec3d vec3d2, int i, Entity entity, Entity entity2) {
        if (mc.world == null) {
            return null;
        }

        Vec3d vec3d3 = vec3d;
        Vec3d vec3d4 = vec3d2;
        boolean flag;
        int i2;
        if (entity != null) {
            flag = !intersectsOwner(vec3d, vec3d2, entity);
            i2 = 1;
        } else {
            flag = true;
            i2 = 1;
        }

        for (; i2 <= 300; i2++) {
            boolean flag2 = mc.world.getFluidState(BlockPos.ofFloored(vec3d3)).isIn(FluidTags.WATER);
            vec3d4 = vec3d4.subtract(0.0, 0.03, 0.0).multiply(flag2 ? 0.8 : 0.99);
            Vec3d vec3d5 = vec3d3.add(vec3d4);
            BlockHitResult blockHitResult = mc.world
                .getCollisionsIncludingWorldBorder(new RaycastContext(vec3d3, vec3d5, ShapeType.COLLIDER, FluidHandling.NONE, ShapeContext.absent()));
            boolean flag3 = blockHitResult.getType() != Type.MISS;
            EntityHitResult entityHitResult = findEntityHit(vec3d3, flag3 ? blockHitResult.getPos() : vec3d5, vec3d4, i + i2, flag ? null : entity, entity2);
            if (entityHitResult != null) {
                return new PearlTrajectoryUtil.Flight(entityHitResult.getPos(), vec3d3, i2, entityHitResult.getEntity());
            }

            if (flag3) {
                return new PearlTrajectoryUtil.Flight(blockHitResult.getPos(), vec3d3, i2, null);
            }

            vec3d3 = vec3d5;
            boolean flag4 = mc.world
                .getChunkManager()
                .isChunkLoaded(ChunkSectionPos.getSectionCoordFloored(vec3d5.x), ChunkSectionPos.getSectionCoordFloored(vec3d5.z));
            if (!flag4 || vec3d5.y < mc.world.getBottomY() - 64) {
                break;
            }

            if (!flag) {
                flag = !intersectsOwner(vec3d5, vec3d4, entity);
            }
        }

        return null;
    }

    private static EntityHitResult findEntityHit(Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, int i, Entity entity2, Entity entity3) {
        float f = MathHelper.clamp((i - 2) / 20.0F, 0.0F, 0.3F);
        Box box = getPearlBox(vec3d).stretch(vec3d3).expand(1.0);
        EntityHitResult entityHitResult = null;
        double d = Double.MAX_VALUE;

        for (Entity entity4 : mc.world
            .getOtherEntities(null, box, entity5 -> entity5.canBeHitByProjectile() && entity5 != entity3 && (entity2 == null || !entity2.isConnectedThroughVehicle(entity5)))) {
            Optional optional = entity4.getBoundingBox().expand(f).raycast(vec3d, vec3d2);
            if (!optional.isEmpty()) {
                double d2 = vec3d.squaredDistanceTo((Vec3d)optional.get());
                if (!(d2 >= d)) {
                    d = d2;
                    entityHitResult = new EntityHitResult(entity4, (Vec3d)optional.get());
                }
            }
        }

        return entityHitResult;
    }

    private static boolean intersectsOwner(Vec3d vec3d, Vec3d vec3d2, Entity entity) {
        return getPearlBox(vec3d).stretch(vec3d2).expand(1.0).intersects(entity.getBoundingBox());
    }

    private static Box getPearlBox(Vec3d vec3d) {
        return new Box(vec3d.x - 0.125, vec3d.y, vec3d.z - 0.125, vec3d.x + 0.125, vec3d.y + 0.25, vec3d.z + 0.125);
    }

    private PearlTrajectoryUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public record Flight(Vec3d impact, Vec3d destination, int ticks, Entity hitEntity) {
    }

    public record Solution(Vector2f rotation, PearlTrajectoryUtil.Flight flight) {
    }
}
