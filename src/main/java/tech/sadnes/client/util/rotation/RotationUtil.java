package tech.sadnes.client.util.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.util.IMinecraft;

public final class RotationUtil implements IMinecraft {
    private RotationUtil() {
    }

    public static Vec2f getRotations(Vec3d vec3d, Vec3d vec3d2) {
        Vec3d vec3d3 = vec3d2.subtract(vec3d).normalize();
        double d = Math.hypot(vec3d3.x, vec3d3.z);
        float f = (float)Math.toDegrees(Math.atan2(vec3d3.z, vec3d3.x)) - 90.0F;
        float f2 = (float)(-Math.toDegrees(Math.atan2(vec3d3.y, d)));
        return new Vec2f(f, f2);
    }

    public static Vec2f getSnappedRotations(Vec3d vec3d, Vec3d vec3d2) {
        Vec3d vec3d3 = vec3d2.subtract(vec3d);
        double d = Math.sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z);
        float f = (float)(Math.toDegrees(Math.atan2(vec3d3.z, vec3d3.x)) - 90.0);
        float f2 = (float)(-Math.toDegrees(Math.atan2(vec3d3.y, d)));
        float f3 = (float)(Math.pow(mc.options.getMouseSensitivity().getValue(), 1.5) * 0.05F + 0.1F);
        float f4 = f3 * f3 * f3 * 1.2F;
        float f5 = f - f % f4;
        float f6 = f2 - f2 % (f4 * f3);
        return new Vec2f(f5, f6);
    }

    public static Vec2f getSnappedRotationsTo(Vec3d vec3d) {
        return mc.player == null ? new Vec2f(0.0F, 0.0F) : getSnappedRotations(mc.player.getEyePos(), vec3d);
    }

    public static Vec2f getRotationsToEntity(Entity entity) {
        return getRotationsTo(new Vec3d(entity.getX(), entity.getY(), entity.getZ()).add(0.0, entity.getHeight() / 2.0F, 0.0));
    }

    public static Vec2f getRotationsTo(Vec3d vec3d) {
        return mc.player == null ? new Vec2f(0.0F, 0.0F) : getRotations(mc.player.getEyePos(), vec3d);
    }

    public static Vec2f getRotationsToBlockFace(Vec3d vec3d, Direction direction) {
        double d = vec3d.x + 0.5 + direction.getOffsetX() * 0.5;
        double d2 = vec3d.y + 0.5 + direction.getOffsetY() * 0.5;
        double d3 = vec3d.z + 0.5 + direction.getOffsetZ() * 0.5;
        return getRotationsTo(new Vec3d(d, d2, d3));
    }

    public static float computeBodyYaw(float f) {
        if (mc.player == null) {
            return f;
        }

        double d = mc.player.getX() - mc.player.lastX;
        double d2 = mc.player.getZ() - mc.player.lastZ;
        float f2 = (float)(d * d + d2 * d2);
        float f3 = mc.player.bodyYaw;
        float f4 = f3;
        if (f2 > 0.0025000002F) {
            f4 = (float)Math.toDegrees(Math.atan2(d2, d)) - 90.0F;
        }

        if (mc.player.handSwingProgress > 0.0F) {
            f4 = f;
        }

        float f5 = MathHelper.wrapDegrees(f - (f3 + MathHelper.wrapDegrees(f4 - f3) * 0.3F));
        float f6 = MathHelper.clamp(f5, -75.0F, 75.0F);
        float f7 = f - f6;
        if (f6 * f6 > 2500.0F) {
            f7 += f6 * 0.2F;
        }

        return f7;
    }

    public static float getAngleDifference(float f, float f2) {
        float f3 = Math.abs(f2 - f) % 360.0F;
        return f3 > 180.0F ? 360.0F - f3 : f3;
    }

    public static Vec3d getEyePos(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ()).add(0.0, entity.getEyeHeight(entity.getPose()), 0.0);
    }

    public static float[] getRotationArrayTo(Vec3d vec3d) {
        return mc.player == null ? new float[]{0.0F, 0.0F} : getRotationArray(getEyePos(mc.player), vec3d);
    }

    public static float[] getRotationArray(Vec3d vec3d, Vec3d vec3d2) {
        double d = vec3d2.x - vec3d.x;
        double d2 = (vec3d2.y - vec3d.y) * -1.0;
        double d3 = vec3d2.z - vec3d.z;
        double d4 = Math.sqrt(d * d + d3 * d3);
        float f = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d3, d)) - 90.0);
        float f2 = (float)MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d2, d4))), -90.0, 90.0);
        return new float[]{f, f2};
    }

    public static float[] getRotationFromDirection(Vec3d vec3d) {
        float f = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0);
        float f2 = (float)(-Math.toDegrees(Math.atan2(vec3d.y, Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z))));
        return new float[]{f, f2};
    }
}
