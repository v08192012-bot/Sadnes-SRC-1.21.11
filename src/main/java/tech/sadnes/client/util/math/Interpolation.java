package tech.sadnes.client.util.math;

public final class Interpolation {
    private Interpolation() {
    }

    public static double interpolate(double d, double d2, double d3) {
        return d + d3 * (d2 - d);
    }

    public static float interpolate(float f, float f2, double d) {
        return (float)(f + d * (f2 - f));
    }

    public static int interpolate(int i, int i2, double d) {
        return (int)(i + d * (i2 - i));
    }
}
