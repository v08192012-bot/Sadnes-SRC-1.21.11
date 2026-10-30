package tech.sadnes.client.deobf;

public final class ExactMath {
    private ExactMath() {
    }

    private static double quiet(double d) {
        return Double.longBitsToDouble(Double.doubleToRawLongBits(d) | 2251799813685248L);
    }

    private static float quiet(float f) {
        return Float.intBitsToFloat(Float.floatToRawIntBits(f) | 4194304);
    }

    public static double add(double d, double d2) {
        if (Double.isNaN(d)) {
            return quiet(d);
        }

        if (Double.isNaN(d2)) {
            return quiet(d2);
        }

        double d3 = d + d2;
        return Double.isNaN(d3) ? Double.NaN : d3;
    }

    public static double subtract(double d, double d2) {
        if (Double.isNaN(d)) {
            return quiet(d);
        }

        if (Double.isNaN(d2)) {
            return quiet(d2);
        }

        double d3 = d - d2;
        return Double.isNaN(d3) ? Double.NaN : d3;
    }

    public static double multiply(double d, double d2) {
        if (Double.isNaN(d)) {
            return quiet(d);
        }

        if (Double.isNaN(d2)) {
            return quiet(d2);
        }

        double d3 = d * d2;
        return Double.isNaN(d3) ? Double.NaN : d3;
    }

    public static double divide(double d, double d2) {
        if (Double.isNaN(d)) {
            return quiet(d);
        }

        if (Double.isNaN(d2)) {
            return quiet(d2);
        }

        double d3 = d / d2;
        return Double.isNaN(d3) ? Double.NaN : d3;
    }

    public static double remainder(double d, double d2) {
        if (Double.isNaN(d)) {
            return quiet(d);
        }

        if (Double.isNaN(d2)) {
            return quiet(d2);
        }

        double d3 = d % d2;
        return Double.isNaN(d3) ? Double.NaN : d3;
    }

    public static float add(float f, float f2) {
        if (Float.isNaN(f)) {
            return quiet(f);
        }

        if (Float.isNaN(f2)) {
            return quiet(f2);
        }

        float f3 = f + f2;
        return Float.isNaN(f3) ? Float.NaN : f3;
    }

    public static float subtract(float f, float f2) {
        if (Float.isNaN(f)) {
            return quiet(f);
        }

        if (Float.isNaN(f2)) {
            return quiet(f2);
        }

        float f3 = f - f2;
        return Float.isNaN(f3) ? Float.NaN : f3;
    }

    public static float multiply(float f, float f2) {
        if (Float.isNaN(f)) {
            return quiet(f);
        }

        if (Float.isNaN(f2)) {
            return quiet(f2);
        }

        float f3 = f * f2;
        return Float.isNaN(f3) ? Float.NaN : f3;
    }

    public static float divide(float f, float f2) {
        if (Float.isNaN(f)) {
            return quiet(f);
        }

        if (Float.isNaN(f2)) {
            return quiet(f2);
        }

        float f3 = f / f2;
        return Float.isNaN(f3) ? Float.NaN : f3;
    }

    public static float remainder(float f, float f2) {
        if (Float.isNaN(f)) {
            return quiet(f);
        }

        if (Float.isNaN(f2)) {
            return quiet(f2);
        }

        float f3 = f % f2;
        return Float.isNaN(f3) ? Float.NaN : f3;
    }
}
