package tech.sadnes.client.util.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector2f;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.Rotation;

public final class SelfPearlUtil implements IMinecraft {
    private SelfPearlUtil() {
    }

    public static Vector2f getRotationOrDefault() {
        Vector2f vector2f = findBestRotation();
        return vector2f != null ? vector2f : new Vector2f(Rotation.getCameraYaw(), 89.0F);
    }

    public static double getLandingScore(Vector2f vector2f) {
        if (mc.player != null && mc.world != null) {
            Vec3d vec3d = simulateLanding(vector2f.x, vector2f.y, getNearbyPlayers());
            return vec3d == null ? 0.0 : getProximityScore(getProjectileBox(vec3d), mc.player.getBoundingBox());
        } else {
            return 0.0;
        }
    }

    public static Vector2f findBestRotation() {
        if (mc.player != null && mc.world != null) {
            List list = getNearbyPlayers();
            Box box = mc.player.getBoundingBox();
            Vector2f vector2f = null;
            double d = -Double.MAX_VALUE;
            float f = Rotation.getCameraYaw();

            for (int i = 0; i < 24; i++) {
                float f2 = MathHelper.wrapDegrees(f + i * 15.0F);

                for (float f3 = 90.0F; f3 >= -20.0F; f3 -= 5.0F) {
                    Vec3d vec3d = simulateLanding(f2, f3, list);
                    if (vec3d != null) {
                        Box box2 = getProjectileBox(vec3d);
                        double d2 = getProximityScore(box2, box);
                        if (!(d2 <= 0.0)) {
                            double d3 = d2 - getMaxProximityScore(box2, list);
                            if (!(d3 <= d)) {
                                d = d3;
                                vector2f = new Vector2f(f2, f3);
                                if (d3 >= 0.999) {
                                    return vector2f;
                                }
                            }
                        }
                    }
                }
            }

            return vector2f;
        } else {
            return null;
        }
    }

    private static List<LivingEntity> getNearbyPlayers() {
        ArrayList arrayList = new ArrayList();
        Box box = mc.player.getBoundingBox().expand(9.0);

        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (playerEntity != mc.player && !playerEntity.isSpectator() && playerEntity.isAlive() && box.intersects(playerEntity.getBoundingBox())) {
                arrayList.add(playerEntity);
            }
        }

        return arrayList;
    }

    private static Vec3d simulateLanding(float f, float f2, List<LivingEntity> livingEntities) {
        Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
        Vec3d vec3d2 = getThrowVelocity(f, f2);
        Vec3d vec3d3 = mc.player.getEyePos();

        for (int i = 0; i < 20; i++) {
            vec3d2 = vec3d2.add(0.0, -0.05, 0.0).multiply(0.99);
            Vec3d vec3d4 = vec3d.add(vec3d2);
            Vec3d vec3d5 = raycastStep(vec3d, vec3d4, livingEntities);
            if (vec3d5 != null) {
                return vec3d5;
            }

            if (vec3d4.squaredDistanceTo(vec3d3) > 36.0) {
                return null;
            }

            vec3d = vec3d4;
        }

        return null;
    }

    private static Vec3d getThrowVelocity(float f, float f2) {
        float f3 = f * (float) (Math.PI / 180.0);
        float f4 = f2 * (float) (Math.PI / 180.0);
        float f5 = (f2 - 20.0F) * (float) (Math.PI / 180.0);
        Vec3d vec3d = new Vec3d(-MathHelper.sin(f3) * MathHelper.cos(f4), -MathHelper.sin(f5), MathHelper.cos(f3) * MathHelper.cos(f4))
            .normalize()
            .multiply(0.5);
        Vec3d vec3d2 = mc.player.getMovement();
        double d = vec3d2.x;
        return mc.player.isOnGround() ? vec3d.add(d, 0.0, vec3d2.z) : vec3d.add(d, vec3d2.y, vec3d2.z);
    }

    private static Vec3d raycastStep(Vec3d vec3d, Vec3d vec3d2, List<LivingEntity> livingEntities) {
        HitResult hitResult = RaycastUtil.raycast(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, false);
        Vec3d vec3d3 = hitResult.getType() == Type.BLOCK ? hitResult.getPos() : null;
        int i = livingEntities.size();

        for (int i2 = 0; i2 < i; i2++) {
            Optional optional = ((LivingEntity)livingEntities.get(i2)).getBoundingBox().expand(0.3).raycast(vec3d, vec3d2);
            if (!optional.isEmpty() && (vec3d3 == null || vec3d.squaredDistanceTo((Vec3d)optional.get()) < vec3d.squaredDistanceTo(vec3d3))) {
                vec3d3 = (Vec3d)optional.get();
            }
        }

        return vec3d3;
    }

    private static Box getProjectileBox(Vec3d vec3d) {
        return new Box(vec3d.x - 0.125, vec3d.y, vec3d.z - 0.125, vec3d.x + 0.125, vec3d.y + 0.25, vec3d.z + 0.125);
    }

    private static double getProximityScore(Box box, Box box2) {
        double d = Math.max(Math.max(box.minX - box2.maxX, box2.minX - box.maxX), 0.0);
        double d2 = Math.max(Math.max(box.minY - box2.maxY, box2.minY - box.maxY), 0.0);
        double d3 = Math.max(Math.max(box.minZ - box2.maxZ, box2.minZ - box.maxZ), 0.0);
        if (!(d >= 4.0) && !(d2 >= 2.0) && !(d3 >= 4.0)) {
            double d4 = Math.sqrt(d * d + d2 * d2 + d3 * d3);
            return d4 < 4.0 ? 1.0 - d4 / 4.0 : 0.0;
        } else {
            return 0.0;
        }
    }

    private static double getMaxProximityScore(Box box, List<LivingEntity> livingEntities) {
        double d = 0.0;
        int i = livingEntities.size();

        for (int i2 = 0; i2 < i; i2++) {
            d = Math.max(d, getProximityScore(box, ((LivingEntity)livingEntities.get(i2)).getBoundingBox()));
        }

        return d;
    }
}
