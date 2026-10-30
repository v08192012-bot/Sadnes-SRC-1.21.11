package tech.sadnes.client.util.color;

import net.minecraft.util.math.MathHelper;

public final class Theme {
    private static int accent = ColorUtil.fromHex("4287f5");
    private static boolean shadowEnabled = true;
    private static int textColor;
    private static boolean customText;
    private static int background;
    private static boolean customBackground;

    public static int getAccent() {
        return accent;
    }

    public static void setAccent(int i) {
        accent = ColorUtil.setAlpha(i, 255);
    }

    public static int getBackground() {
        return customBackground ? background : ColorUtil.BLACK;
    }

    public static void setBackground(int i) {
        background = ColorUtil.setAlpha(i, 255);
        customBackground = true;
    }

    public static boolean hasCustomBackground() {
        return customBackground;
    }

    public static int getWhite() {
        return ColorUtil.WHITE;
    }

    public static int getPrimary() {
        return accent;
    }

    public static int getAccentLight() {
        return tintWhite(0.26F);
    }

    public static int getSurfaceDarkest() {
        return tintBackground(0.94F);
    }

    public static int getSurfaceDark() {
        return tintBackground(0.88F);
    }

    public static int getSurface() {
        return tintBackground(0.82F);
    }

    public static int getBase() {
        return getBackground();
    }

    public static int getOutline() {
        return tintBackground(0.62F);
    }

    public static int getMutedText() {
        return tintWhite(0.66F);
    }

    public static int getTextColor() {
        return customText ? textColor : tintWhite(0.92F);
    }

    public static int getSecondaryText() {
        return customText ? ColorUtil.interpolate(textColor, getBackground(), 0.3F) : tintWhite(0.68F);
    }

    public static int getContrastColor(int i) {
        return ColorUtil.luminance(i) > 0.6F ? ColorUtil.darken(i, 0.14F) : ColorUtil.interpolate(i, getWhite(), 0.92F);
    }

    public static void setTextColor(int i) {
        textColor = ColorUtil.setAlpha(i, 255);
        customText = true;
    }

    public static boolean hasCustomText() {
        return customText;
    }

    public static int getSurfaceDeep() {
        return tintBackground(0.98F);
    }

    public static int getHighlight() {
        return getPrimary();
    }

    public static int getAccentSoft() {
        return tintWhite(0.38F);
    }

    public static int getNearWhite() {
        return tintWhite(0.96F);
    }

    public static int getNearBackground() {
        return tintBackground(0.99F);
    }

    public static float getShadowStrength(float f) {
        return shadowEnabled ? f * f * 0.5F : 0.0F;
    }

    public static boolean isShadowEnabled() {
        return shadowEnabled;
    }

    public static void setShadowEnabled(boolean flag) {
        shadowEnabled = flag;
    }

    public static int withAlpha(int i, float f) {
        return ColorUtil.multiplyAlpha(i, MathHelper.clamp(f, 0.0F, 1.0F));
    }

    public static float dim(float f) {
        return f * 0.75F;
    }

    public static int tintWhite(float f) {
        return mix(accent, getWhite(), f);
    }

    public static int tintBackground(float f) {
        return mix(accent, getBackground(), f);
    }

    private static int mix(int i, int i2, float f) {
        return ColorUtil.interpolate(i, i2, MathHelper.clamp(f, 0.0F, 1.0F));
    }

    private Theme() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
