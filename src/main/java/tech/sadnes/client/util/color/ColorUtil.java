package tech.sadnes.client.util.color;

import java.awt.Color;
import net.minecraft.util.math.MathHelper;

public final class ColorUtil {
    public static final int RED = rgb(255, 0, 0);
    public static final int GREEN = rgb(0, 255, 0);
    public static final int BLUE = rgb(0, 0, 255);
    public static final int YELLOW = rgb(255, 255, 0);
    public static final int WHITE = gray(255);
    public static final int BLACK = gray(0);

    public static int red(int i) {
        return i >> 16 & 0xFF;
    }

    public static int green(int i) {
        return i >> 8 & 0xFF;
    }

    public static int blue(int i) {
        return i & 0xFF;
    }

    public static int alpha(int i) {
        return i >> 24 & 0xFF;
    }

    public static float redf(int i) {
        return red(i) / 255.0F;
    }

    public static float greenf(int i) {
        return green(i) / 255.0F;
    }

    public static float bluef(int i) {
        return blue(i) / 255.0F;
    }

    public static float alphaf(int i) {
        return alpha(i) / 255.0F;
    }

    public static float luminance(int i) {
        return redf(i) * 0.299F + greenf(i) * 0.587F + bluef(i) * 0.114F;
    }

    public static float[] toHsb(int i) {
        return Color.RGBtoHSB(red(i), green(i), blue(i), null);
    }

    public static int fromHsb(float f, float f2, float f3) {
        int i = Color.HSBtoRGB(MathHelper.clamp(f, 0.0F, 1.0F), MathHelper.clamp(f2, 0.0F, 1.0F), MathHelper.clamp(f3, 0.0F, 1.0F));
        return rgb(red(i), green(i), blue(i));
    }

    public static int fromHsba(float f, float f2, float f3, float f4) {
        int i = fromHsb(f, f2, f3);
        return setAlphaf(i, f4);
    }

    public static int replaceAlpha(int i, int i2) {
        return rgba(red(i), green(i), blue(i), i2);
    }

    public static int rgbaf(float f, float f2, float f3, float f4) {
        return rgba(Math.round(f * 255.0F), Math.round(f2 * 255.0F), Math.round(f3 * 255.0F), Math.round(f4 * 255.0F));
    }

    public static int rgbWithAlphaf(int i, int i2, int i3, float f) {
        return rgba(i, i2, i3, Math.round(f * 255.0F));
    }

    public static int rgbf(float f, float f2, float f3) {
        return rgbaf(f, f2, f3, 1.0F);
    }

    public static int grayAlpha(int i, int i2) {
        return rgba(i, i, i, i2);
    }

    public static int grayAlphaf(int i, float f) {
        return grayAlpha(i, Math.round(f * 255.0F));
    }

    public static int gray(int i) {
        return rgb(i, i, i);
    }

    public static int rgba(int i, int i2, int i3, int i4) {
        return MathHelper.clamp(i4, 0, 255) << 24
            | MathHelper.clamp(i, 0, 255) << 16
            | MathHelper.clamp(i2, 0, 255) << 8
            | MathHelper.clamp(i3, 0, 255);
    }

    public static int rgb(int i, int i2, int i3) {
        return rgba(i, i2, i3, 255);
    }

    public static int setAlpha(int i, int i2) {
        return rgba(red(i), green(i), blue(i), i2);
    }

    public static int setAlphaf(int i, float f) {
        return rgbWithAlphaf(red(i), green(i), blue(i), f);
    }

    public static int multiplyAlpha(int i, float f) {
        return rgba(red(i), green(i), blue(i), Math.round(alpha(i) * f));
    }

    public static int darken(int i, float f) {
        return rgba(Math.round(red(i) * f), Math.round(green(i) * f), Math.round(blue(i) * f), alpha(i));
    }

    public static int multiply(int i, int i2) {
        return rgbaf(redf(i) * redf(i2), greenf(i) * greenf(i2), bluef(i) * bluef(i2), alphaf(i) * alphaf(i2));
    }

    public static int brighten(int i, float f) {
        return rgba(
            Math.min(255, Math.round(red(i) / f)),
            Math.min(255, Math.round(green(i) / f)),
            Math.min(255, Math.round(blue(i) / f)),
            alpha(i)
        );
    }

    public static int interpolate(int i, int i2, float f) {
        float f2 = MathHelper.clamp(f, 0.0F, 1.0F);
        return rgba(
            MathHelper.lerp(f2, red(i), red(i2)),
            MathHelper.lerp(f2, green(i), green(i2)),
            MathHelper.lerp(f2, blue(i), blue(i2)),
            MathHelper.lerp(f2, alpha(i), alpha(i2))
        );
    }

    public static int average(int i, int i2) {
        return interpolate(i, i2, 0.5F);
    }

    public static int rainbow(int i, int i2, float f, float f2, float f3) {
        int i3 = (int)((System.currentTimeMillis() / i + i2) % 360L);
        float f4 = i3 / 360.0F;
        int i4 = Color.HSBtoRGB(f4, f, f2);
        return rgba(red(i4), green(i4), blue(i4), Math.round(f3 * 255.0F));
    }

    public static int fade(int i, int i2, int i3, int i4) {
        int i5 = (int)((System.currentTimeMillis() / i + i2) % 360L);
        int i6 = i5 >= 180 ? 360 - i5 : i5;
        return interpolate(i3, i4, i6 / 180.0F);
    }

    public static int fromHex(String string) {
        if (string != null && !string.isEmpty()) {
            String string2 = string.replace("#", "");
            if (string2.length() == 6) {
                int i = Integer.parseInt(string2.substring(0, 2), 16);
                int i2 = Integer.parseInt(string2.substring(2, 4), 16);
                int i3 = Integer.parseInt(string2.substring(4, 6), 16);
                return rgb(i, i2, i3);
            } else if (string2.length() == 8) {
                int i4 = Integer.parseInt(string2.substring(0, 2), 16);
                int i5 = Integer.parseInt(string2.substring(2, 4), 16);
                int i6 = Integer.parseInt(string2.substring(4, 6), 16);
                int i7 = Integer.parseInt(string2.substring(6, 8), 16);
                return rgba(i5, i6, i7, i4);
            } else {
                return 0;
            }
        } else {
            throw new IllegalArgumentException("Hex color string cannot be null or empty");
        }
    }

    private ColorUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
