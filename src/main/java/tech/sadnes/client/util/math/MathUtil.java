package tech.sadnes.client.util.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;

public final class MathUtil {
    private MathUtil() {
    }

    public static float roundToStep(float f, float f2) {
        return Math.round(f / f2) * f2;
    }

    public static double round(double d, int i) {
        double d2 = Math.pow(10.0, i);
        return Math.round(d * d2) / d2;
    }

    public static float roundToStepPrecise(float f, float f2) {
        double d = (double)Math.round(f / f2) * f2;
        return new BigDecimal(d).setScale(3, RoundingMode.HALF_UP).floatValue();
    }

    public static double clamp(double d, double d2, double d3) {
        return Math.max(d, Math.min(d2, d3));
    }

    public static float clamp(float f, float f2, float f3) {
        return Math.max(f, Math.min(f2, f3));
    }

    public static int clamp(int i, int i2, int i3) {
        return Math.max(i, Math.min(i2, i3));
    }

    public static double randomBetween(double d, double d2) {
        if (d > d2) {
            double d3 = d;
            d = d2;
            d2 = d3;
        }

        return ThreadLocalRandom.current().nextDouble(d, d2);
    }

    public static float randomBetween(float f, float f2) {
        if (f > f2) {
            float f3 = f;
            f = f2;
            f2 = f3;
        }

        return (float)ThreadLocalRandom.current().nextDouble(f, f2);
    }

    public static float getFrameTime() {
        int i = MinecraftClient.getInstance().getCurrentFps();
        return i > 0 ? 1.0F / i : 1.0F;
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

    public static int random(int i, int i2) {
        return (int)random(i, i2 + 1.0F);
    }

    public static float random(float f, float f2) {
        return (float)random((double)f, (double)f2);
    }

    public static double random(double d, double d2) {
        if (d == d2) {
            return d;
        }

        if (d > d2) {
            double d3 = d;
            d = d2;
            d2 = d3;
        }

        return ThreadLocalRandom.current().nextDouble(d, d2);
    }
}
