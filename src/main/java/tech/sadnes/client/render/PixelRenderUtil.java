package tech.sadnes.client.render;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.util.color.ColorUtil;

public final class PixelRenderUtil {
    private PixelRenderUtil() {
    }

    public static void drawRect(DrawContext drawContext, float f, float f2, float f3, float f4, int i) {
        if (!(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            int i2 = Math.round(f);
            int i3 = Math.round(f2);
            int i4 = Math.round(f + f3);
            int i5 = Math.round(f2 + f4);
            drawContext.fill(i2, i3, i4, i5, i);
        }
    }

    public static void drawGradient(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, int i3, int i4) {
        int i5 = Math.max(1, Math.round(f4));

        for (int i6 = 0; i6 < i5; i6++) {
            float f5 = i5 <= 1 ? 0.0F : (float)i6 / (i5 - 1);
            int i7 = ColorUtil.interpolate(i, i4, f5);
            int i8 = ColorUtil.interpolate(i2, i3, f5);
            drawHorizontalGradient(drawContext, f, f2 + i6, f3, 1.0F, i7, i8);
        }
    }

    public static void drawVerticalGradient(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        if (!(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            int i3 = Math.round(f);
            int i4 = Math.round(f2);
            int i5 = Math.round(f + f3);
            int i6 = Math.round(f2 + f4);
            drawContext.fillGradient(i3, i4, i5, i6, i, i2);
        }
    }

    public static void drawHorizontalGradient(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        if (!(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            int i3 = Math.max(1, Math.round(f3));

            for (int i4 = 0; i4 < i3; i4++) {
                float f5 = i3 <= 1 ? 0.0F : (float)i4 / (i3 - 1);
                int i5 = ColorUtil.interpolate(i, i2, f5);
                drawRect(drawContext, f + i4, f2, 1.0F, f4, i5);
            }
        }
    }

    public static void drawRoundedRect(DrawContext drawContext, float f, float f2, float f3, float f4, Radius radius, int i) {
        if (!(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            int i2 = Math.round(Math.min(radius.getMax(), Math.min(f3, f4) / 2.0F));
            if (i2 <= 0) {
                drawRect(drawContext, f, f2, f3, f4, i);
                return;
            }

            int i3 = Math.round(f);
            int i4 = Math.round(f2);
            int i5 = Math.round(f + f3);
            int i6 = Math.round(f2 + f4);
            drawContext.fill(i3 + i2, i4, i5 - i2, i6, i);
            drawContext.fill(i3, i4 + i2, i5, i6 - i2, i);

            for (int i7 = 0; i7 < i2; i7++) {
                int i8 = (int)Math.floor(Math.sqrt(i2 * i2 - i7 * i7));
                drawContext.fill(i3 + i2 - i8, i4 + i7, i5 - i2 + i8, i4 + i7 + 1, i);
                drawContext.fill(i3 + i2 - i8, i6 - i7 - 1, i5 - i2 + i8, i6 - i7, i);
            }
        }
    }

    public static void drawRoundedOutline(DrawContext drawContext, float f, float f2, float f3, float f4, Radius radius, float f5, int i) {
        int i2 = Math.max(1, Math.round(f5));

        for (int i3 = 0; i3 < i2; i3++) {
            drawRoundedOutlineLine(
                drawContext, f + i3, f2 + i3, f3 - i3 * 2.0F, f4 - i3 * 2.0F, Radius.all(Math.max(0.0F, radius.getMax() - i3)), i
            );
        }
    }

    private static void drawRoundedOutlineLine(DrawContext drawContext, float f, float f2, float f3, float f4, Radius radius, int i) {
        if (!(f3 <= 1.0F) && !(f4 <= 1.0F)) {
            int i2 = Math.round(Math.min(radius.getMax(), Math.min(f3, f4) / 2.0F));
            int i3 = Math.round(f);
            int i4 = Math.round(f2);
            int i5 = Math.round(f + f3);
            int i6 = Math.round(f2 + f4);
            if (i2 <= 0) {
                drawContext.drawStrokedRectangle(i3, i4, i5 - i3, i6 - i4, i);
                return;
            }

            drawContext.fill(i3 + i2, i4, i5 - i2, i4 + 1, i);
            drawContext.fill(i3 + i2, i6 - 1, i5 - i2, i6, i);
            drawContext.fill(i3, i4 + i2, i3 + 1, i6 - i2, i);
            drawContext.fill(i5 - 1, i4 + i2, i5, i6 - i2, i);

            for (int i7 = 0; i7 < i2; i7++) {
                int i8 = (int)Math.floor(Math.sqrt(i2 * i2 - i7 * i7));
                drawPixel(drawContext, i3 + i2 - i8, i4 + i7, i);
                drawPixel(drawContext, i5 - i2 + i8 - 1, i4 + i7, i);
                drawPixel(drawContext, i3 + i2 - i8, i6 - i7 - 1, i);
                drawPixel(drawContext, i5 - i2 + i8 - 1, i6 - i7 - 1, i);
            }
        }
    }

    public static void drawShadow(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i) {
        int i2 = Math.max(1, Math.round(f5));
        int i3 = ColorUtil.alpha(i);

        for (int i4 = 1; i4 <= i2; i4++) {
            float f6 = 1.0F - i4 / (i2 + 1.0F);
            int i5 = ColorUtil.setAlpha(i, Math.round(i3 * f6 * f6));
            drawRect(drawContext, f - i4, f2 - i4, f3 + i4 * 2.0F, 1.0F, i5);
            drawRect(drawContext, f - i4, f2 + f4 + i4 - 1.0F, f3 + i4 * 2.0F, 1.0F, i5);
            drawRect(drawContext, f - i4, f2, 1.0F, f4, i5);
            drawRect(drawContext, f + f3 + i4 - 1.0F, f2, 1.0F, f4, i5);
        }
    }

    public static void scissor(DrawContext drawContext, float f, float f2, float f3, float f4, Runnable runnable) {
        boolean flag = ScissorStack.push(drawContext, f, f2, f3, f4);
        if (flag) {
            try {
                runnable.run();
            } finally {
                ScissorStack.pop(drawContext);
            }
        }
    }

    private static void drawPixel(DrawContext drawContext, int i, int i2, int i3) {
        drawContext.fill(i, i2, i + 1, i2 + 1, i3);
    }
}
