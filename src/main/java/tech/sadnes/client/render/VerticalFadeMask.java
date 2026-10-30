package tech.sadnes.client.render;

import tech.sadnes.client.util.color.ColorUtil;

public final class VerticalFadeMask {
    private static boolean active;
    private static float topStart;
    private static float topEnd;
    private static float topStrength;
    private static float bottomStart;
    private static float bottomEnd;
    private static float bottomStrength;

    private VerticalFadeMask() {
    }

    public static void begin(float f, float f2, float f3, float f4, float f5, float f6) {
        topStart = f;
        topEnd = f2;
        topStrength = clamp01(f3);
        bottomStart = f4;
        bottomEnd = f5;
        bottomStrength = clamp01(f6);
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
        if (topStrength > 0.0F && f < topEnd && topEnd > topStart) {
            float f3 = clamp01((f - topStart) / (topEnd - topStart));
            f2 = 1.0F * (1.0F - topStrength * (1.0F - f3));
        }

        if (bottomStrength > 0.0F && f > bottomStart && bottomEnd > bottomStart) {
            float f4 = clamp01((bottomEnd - f) / (bottomEnd - bottomStart));
            f2 *= 1.0F - bottomStrength * (1.0F - f4);
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
