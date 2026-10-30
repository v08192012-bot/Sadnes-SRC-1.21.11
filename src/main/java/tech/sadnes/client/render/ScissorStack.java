package tech.sadnes.client.render;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2fc;

public final class ScissorStack {
    private static final Deque<ScreenRect> STACK = new ArrayDeque<>();

    private ScissorStack() {
    }

    public static boolean push(DrawContext drawContext, float f, float f2, float f3, float f4) {
        return push(drawContext, f, f2, f3, f4, drawContext.getMatrices());
    }

    public static boolean push(DrawContext drawContext, float f, float f2, float f3, float f4, Matrix3x2fc matrix3x2fc) {
        if (drawContext == null || matrix3x2fc == null || !Float.isFinite(f) || !Float.isFinite(f2) || !Float.isFinite(f3) || !Float.isFinite(f4) || f3 <= 0.0F) {
            return false;
        } else {
            return f4 <= 0.0F ? false : pushRect(drawContext, transformRect(f, f2, f3, f4, matrix3x2fc));
        }
    }

    public static void pop(DrawContext drawContext) {
        if (drawContext != null && !STACK.isEmpty()) {
            STACK.pop();
            if (drawContext.scissorStack.peekLast() != null) {
                drawContext.disableScissor();
            }
        }
    }

    public static void clear(DrawContext drawContext) {
        if (drawContext == null) {
            STACK.clear();
        } else {
            while (!STACK.isEmpty()) {
                STACK.pop();
                if (drawContext.scissorStack.peekLast() != null) {
                    drawContext.disableScissor();
                }
            }
        }
    }

    public static int depth() {
        return STACK.size();
    }

    public static void popTo(DrawContext drawContext, int i) {
        if (i < 0) {
            i = 0;
        }

        while (STACK.size() > i) {
            pop(drawContext);
        }
    }

    public static ScissorStack.Bounds current() {
        if (STACK.isEmpty()) {
            return null;
        }

        ScreenRect screenRect = STACK.peek();
        return new ScissorStack.Bounds(screenRect.getLeft(), screenRect.getTop(), screenRect.getRight(), screenRect.getBottom());
    }

    public static void runWith(DrawContext drawContext, ScissorStack.Bounds bounds, Runnable runnable) {
        if (runnable != null) {
            if (bounds == null) {
                runnable.run();
            } else {
                float f = bounds.x2() - bounds.x1();
                float f2 = bounds.y2() - bounds.y1();
                if (pushRect(drawContext, new ScreenRect(bounds.x1(), bounds.y1(), Math.max(0, (int)f), Math.max(0, (int)f2)))) {
                    try {
                        runnable.run();
                    } finally {
                        pop(drawContext);
                    }
                }
            }
        }
    }

    private static boolean pushRect(DrawContext drawContext, ScreenRect screenRect) {
        if (drawContext != null && screenRect != null && !isEmpty(screenRect)) {
            ScreenRect screenRect2 = drawContext.scissorStack.peekLast();
            ScreenRect screenRect3 = screenRect2 == null ? screenRect : screenRect.intersection(screenRect2);
            if (screenRect3 != null) {
                if (isEmpty(screenRect3)) {
                    return false;
                }

                ScreenRect screenRect4 = drawContext.scissorStack.push(screenRect3);
                STACK.push(screenRect4);
                if (!isEmpty(screenRect4)) {
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    private static ScreenRect transformRect(float f, float f2, float f3, float f4, Matrix3x2fc matrix3x2fc) {
        float f5 = f + f3;
        float f6 = f2 + f4;
        float f7 = transformX(matrix3x2fc, f, f2);
        float f8 = transformY(matrix3x2fc, f, f2);
        float f9 = transformX(matrix3x2fc, f, f6);
        float f10 = transformY(matrix3x2fc, f, f6);
        float f11 = transformX(matrix3x2fc, f5, f6);
        float f12 = transformY(matrix3x2fc, f5, f6);
        float f13 = transformX(matrix3x2fc, f5, f2);
        float f14 = transformY(matrix3x2fc, f5, f2);
        int i = (int)Math.floor(Math.min(Math.min(f7, f9), Math.min(f11, f13)));
        int i2 = (int)Math.floor(Math.min(Math.min(f8, f10), Math.min(f12, f14)));
        int i3 = (int)Math.ceil(Math.max(Math.max(f7, f9), Math.max(f11, f13)));
        int i4 = (int)Math.ceil(Math.max(Math.max(f8, f10), Math.max(f12, f14)));
        return new ScreenRect(i, i2, Math.max(0, i3 - i), Math.max(0, i4 - i2));
    }

    private static float transformX(Matrix3x2fc matrix3x2fc, float f, float f2) {
        return matrix3x2fc.m00() * f + matrix3x2fc.m10() * f2 + matrix3x2fc.m20();
    }

    private static float transformY(Matrix3x2fc matrix3x2fc, float f, float f2) {
        return matrix3x2fc.m01() * f + matrix3x2fc.m11() * f2 + matrix3x2fc.m21();
    }

    private static boolean isEmpty(ScreenRect screenRect) {
        return screenRect.width() <= 0 || screenRect.height() <= 0;
    }

    public record Bounds(int x1, int y1, int x2, int y2) {
    }
}
