package tech.sadnes.client.render;

import tech.sadnes.client.util.color.ColorUtil;

public final class HorizontalFadeMask {
    private static boolean active;
    private static float leftStart;
    private static float leftEnd;
    private static float leftStrength;
    private static float rightStart;
    private static float rightEnd;
    private static float rightStrength;

    private HorizontalFadeMask() {
    }

    public static void begin(float f, float f2, float f3, float f4, float f5, float f6) {
        leftStart = f;
        leftEnd = f2;
        leftStrength = clamp01(f3);
        rightStart = f4;
        rightEnd = f5;
        rightStrength = clamp01(f6);
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getAlphaFactor(float f) {
        if (!active) {
            return 1.0F;
        }

        float f2 = 1.0F;
        if (leftStrength > 0.0F && f < leftEnd && leftEnd > leftStart) {
            float f3 = clamp01((f - leftStart) / (leftEnd - leftStart));
            f2 = 1.0F * (1.0F - leftStrength * (1.0F - f3));
        }

        if (rightStrength > 0.0F && f > rightStart && rightEnd > rightStart) {
            float f4 = clamp01((rightEnd - f) / (rightEnd - rightStart));
            f2 *= 1.0F - rightStrength * (1.0F - f4);
        }

        return f2;
    }

    public static int applyAlpha(int i, float f) {
        float f2 = getAlphaFactor(f);
        return f2 >= 0.999F ? i : ColorUtil.setAlpha(i, Math.round(ColorUtil.alpha(i) * f2));
    }

    private static float clamp01(float f) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f > 1.0F ? 1.0F : f;
        }
    }
}
