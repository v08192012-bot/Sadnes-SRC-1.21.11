package tech.sadnes.client.util.math;

public final class WaveUtil {
    private WaveUtil() {
    }

    public static double cosWave(double d, double d2) {
        return Math.cos(System.currentTimeMillis() / d2) * d;
    }

    public static double sinWave(double d, double d2) {
        return Math.sin(System.currentTimeMillis() / d2) * d;
    }
}
