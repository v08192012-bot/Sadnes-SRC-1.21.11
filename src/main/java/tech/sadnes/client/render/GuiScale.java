package tech.sadnes.client.render;

import net.minecraft.client.gui.DrawContext;
import org.joml.Vector2f;
import tech.sadnes.client.util.IMinecraft;

public final class GuiScale implements IMinecraft {
    public static final float MIN_SCALE = 0.75F;
    public static final float MAX_SCALE = 1.5F;
    private static final float BASE_SCALE = 2.0F;
    private static float scale = 1.0F;
    private static boolean active;
    private static final float MIN_WIDTH = 200.0F;
    private static final float MIN_HEIGHT = 150.0F;

    private GuiScale() {
    }

    public static boolean isActive() {
        return active;
    }

    public static float getScale() {
        return scale;
    }

    public static void setScale(float f) {
        scale = Math.max(0.75F, Math.min(1.5F, f));
    }

    public static float getEffectiveScale() {
        return 2.0F * scale;
    }

    private static float getMatrixScale() {
        double d = mc.getWindow().getScaleFactor();
        if (d <= 0.0) {
            d = 1.0;
        }

        return (float)(getEffectiveScale() / d);
    }

    public static void push(DrawContext drawContext) {
        float f = getMatrixScale();
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(f, f);
        active = true;
    }

    public static void pop(DrawContext drawContext) {
        active = false;
        drawContext.getMatrices().popMatrix();
    }

    public static void reset() {
        active = false;
    }

    public static float toScaled(double d) {
        double d2 = mc.getWindow().getScaleFactor();
        return (float)(d * d2 / getEffectiveScale());
    }

    public static Vector2f toScaledPos(double d, double d2) {
        return new Vector2f(toScaled(d), toScaled(d2));
    }

    public static float applyScale(float f) {
        return f * getMatrixScale();
    }

    public static int getScaledWidthInt() {
        return scaleDimension(mc.getWindow().getFramebufferWidth());
    }

    public static int getScaledHeightInt() {
        return scaleDimension(mc.getWindow().getFramebufferHeight());
    }

    public static float getScaledWidth() {
        return getScaledWidthInt();
    }

    public static float getScaledHeight() {
        return getScaledHeightInt();
    }

    public static boolean isTooSmall() {
        return getScaledWidth() < 200.0F || getScaledHeight() < 150.0F;
    }

    private static int scaleDimension(int i) {
        double d = getEffectiveScale();
        if (d <= 0.0) {
            d = 1.0;
        }

        int i2 = (int)(i / d);
        return i / d > i2 ? i2 + 1 : i2;
    }
}
