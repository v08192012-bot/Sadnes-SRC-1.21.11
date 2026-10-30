package tech.sadnes.client.render.font;

import tech.sadnes.client.util.color.ColorUtil;

public final class ShimmerText {
    private final StringBuilder builder = new StringBuilder();
    private String cachedText = "";
    private float[] charPositions;
    private float cachedWidth;
    private int baseColor = 16777216;
    private int lightColor;
    private int darkColor;
    private int highlightColor;

    public String apply(MsdfFont msdfFont, String string, float f, int i, int i2, int i3) {
        return this.apply(msdfFont, string, f, i, i2, i3, 0, 1.0F);
    }

    public String applyThemed(MsdfFont msdfFont, String string, float f, int i, int i2, float f2) {
        if (i != this.baseColor) {
            this.baseColor = i;
            this.lightColor = ColorUtil.interpolate(i, ColorUtil.WHITE, 0.45F);
            this.darkColor = ColorUtil.darken(i, 0.7F);
            this.highlightColor = ColorUtil.interpolate(i, ColorUtil.WHITE, 0.85F);
        }

        return this.apply(msdfFont, string, f, this.lightColor, this.darkColor, this.highlightColor, i2, f2);
    }

    public String apply(MsdfFont msdfFont, String string, float f, int i, int i2, int i3, int i4, float f2) {
        float f3 = msdfFont.getWidth(string, f);
        if (this.charPositions == null || !string.equals(this.cachedText) || f3 != this.cachedWidth) {
            this.computePositions(msdfFont, string, f, f3);
        }

        long l = System.currentTimeMillis();
        float f4 = (float)(l % 4600L) / 4600.0F;
        float f5 = Math.min(1.0F, (float)(l % 6500L) / 6500.0F / 0.55F);
        float f6 = -0.25F + f5 * 1.5F;
        this.builder.setLength(0);
        int i5 = 0;
        int i6 = 0;

        for (int i7 = string.length(); i6 < i7; i5++) {
            int i8 = string.offsetByCodePoints(i6, 1);
            float f7 = this.charPositions[i5];
            float f8 = 0.5F + 0.5F * (float)Math.sin((f7 - f4) * Math.PI * 2.0);
            float f9 = Math.max(0.0F, 1.0F - Math.abs(f7 - f6) / 0.2F);
            float f10 = f9 * f9 * 0.9F;
            int i9 = lerp(lerp(ColorUtil.red(i), ColorUtil.red(i2), f8), ColorUtil.red(i3), f10);
            int i10 = lerp(lerp(ColorUtil.green(i), ColorUtil.green(i2), f8), ColorUtil.green(i3), f10);
            int i11 = lerp(lerp(ColorUtil.blue(i), ColorUtil.blue(i2), f8), ColorUtil.blue(i3), f10);
            if (f2 < 1.0F) {
                i9 = lerp(ColorUtil.red(i4), i9, f2);
                i10 = lerp(ColorUtil.green(i4), i10, f2);
                i11 = lerp(ColorUtil.blue(i4), i11, f2);
            }

            this.builder.append("${rgb(").append(i9).append(',').append(i10).append(',').append(i11).append(")}").append(string, i6, i8);
            i6 = i8;
        }

        return this.builder.toString();
    }

    private void computePositions(MsdfFont msdfFont, String string, float f, float f2) {
        this.cachedText = string;
        this.cachedWidth = f2;
        int i = string.codePointCount(0, string.length());
        if (this.charPositions == null || this.charPositions.length != i) {
            this.charPositions = new float[i];
        }

        int i2 = 0;
        int i3 = 0;

        for (int i4 = string.length(); i3 < i4; i2++) {
            int i5 = string.offsetByCodePoints(i3, 1);
            float f3 = msdfFont.getWidth(string.substring(0, i3), f);
            float f4 = msdfFont.getWidth(string.substring(i3, i5), f);
            this.charPositions[i2] = (f3 + f4 * 0.5F) / f2;
            i3 = i5;
        }
    }

    private static int lerp(int i, int i2, float f) {
        return Math.round(i + (i2 - i) * f);
    }
}
