package tech.sadnes.client.neuro;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class NeuroFeatures {
    public static final int FEATURE_COUNT = 11;
    public static final int HISTORY_LENGTH = 5;
    public static final int INPUT_SIZE = 55;
    public static final int OUTPUT_SIZE = 2;
    public static final int INDEX_YAW = 0;
    public static final int INDEX_PITCH = 1;
    private static final float PITCH_ERROR_NORM = 45.0F;
    private static final float DISTANCE_NORM = 6.0F;
    private static final float HALF_WIDTH_ANGLE_NORM = 20.0F;
    private static final float TARGET_MOTION_NORM = 30.0F;
    private static final float PREV_DELTA_NORM = 40.0F;
    private static final float TICK_NORM = 20.0F;

    private NeuroFeatures() {
    }

    public static float[] calculateRotation(Vec3d vec3d, Vec3d vec3d2) {
        double d = vec3d2.x - vec3d.x;
        double d2 = vec3d2.y - vec3d.y;
        double d3 = vec3d2.z - vec3d.z;
        float f = (float)(Math.toDegrees(Math.atan2(d3, d)) - 90.0);
        float f2 = (float)(-Math.toDegrees(Math.atan2(d2, Math.sqrt(d * d + d3 * d3))));
        return new float[]{f, f2};
    }

    public static float[] buildFeatures(
        Vec3d vec3d, Vec3d vec3d2, Vec3d vec3d3, Vec3d vec3d4, double d, double d2, float f, float f2, float f3, float f4, int i
    ) {
        double d3 = vec3d3.x - vec3d.x;
        double d4 = vec3d3.y - vec3d.y;
        double d5 = vec3d3.z - vec3d.z;
        double d6 = Math.max(1.0E-4, Math.sqrt(d3 * d3 + d5 * d5));
        double d7 = Math.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
        float[] floats = calculateRotation(vec3d, vec3d3);
        float[] floats2 = calculateRotation(vec3d2, vec3d4);
        double d8 = MathHelper.wrapDegrees(floats[0] - f);
        double d9 = floats[1] - f2;
        double d10 = MathHelper.wrapDegrees(floats[0] - floats2[0]);
        double d11 = floats[1] - floats2[1];
        double d12 = Math.toDegrees(Math.atan2(d, d6));
        double d13 = Math.toDegrees(Math.atan2(d2, Math.max(d7, 1.0E-4)));
        double d14 = Math.toRadians(d8);
        return new float[]{
            (float)Math.sin(d14),
            (float)Math.cos(d14),
            clampUnit((float)(d9 / 45.0)),
            MathHelper.clamp((float)(d7 / 6.0), 0.0F, 1.0F),
            MathHelper.clamp((float)(d12 / 20.0), 0.0F, 1.0F),
            MathHelper.clamp((float)(d13 / 20.0), 0.0F, 1.0F),
            clampUnit((float)(d10 / 30.0)),
            clampUnit((float)(d11 / 30.0)),
            clampUnit(f3 / 40.0F),
            clampUnit(f4 / 40.0F),
            MathHelper.clamp(i / 20.0F, 0.0F, 1.0F)
        };
    }

    public static float[] mirrorFeatures(float[] floats) {
        float[] floats2 = (float[])floats.clone();

        for (int i = 0; i < 5; i++) {
            int i2 = i * 11;
            floats2[i2] = -floats2[i2];
            floats2[i2 + 6] = -floats2[i2 + 6];
            floats2[i2 + 8] = -floats2[i2 + 8];
        }

        return floats2;
    }

    private static float clampUnit(float f) {
        if (f < -1.0F) {
            return -1.0F;
        } else {
            return f > 1.0F ? 1.0F : f;
        }
    }
}
