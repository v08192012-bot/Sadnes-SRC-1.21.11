package tech.sadnes.client.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

public final class GuiBoundsUtil {
    private GuiBoundsUtil() {
    }

    public static ScreenRect getScissor(DrawContext drawContext) {
        ScreenRect screenRect = drawContext.scissorStack.peekLast();
        return screenRect != null && screenRect.width() > 0 && screenRect.height() > 0 ? screenRect : null;
    }

    public static ScreenRect computeBounds(float f, float f2, float f3, float f4, Matrix3x2fc matrix3x2fc, ScreenRect screenRect) {
        int i = (int)Math.floor(f);
        int i2 = (int)Math.floor(f2);
        int i3 = i + Math.max(1, (int)Math.ceil(f3) - i);
        int i4 = i2 + Math.max(1, (int)Math.ceil(f4) - i2);
        Vector2f vector2f = new Vector2f();
        matrix3x2fc.transformPosition(i, i2, vector2f);
        float f5 = vector2f.x;
        float f6 = vector2f.x;
        float f7 = vector2f.y;
        float f8 = vector2f.y;
        matrix3x2fc.transformPosition(i3, i2, vector2f);
        float f9 = Math.min(f5, vector2f.x);
        float f10 = Math.max(f6, vector2f.x);
        float f11 = Math.min(f7, vector2f.y);
        float f12 = Math.max(f8, vector2f.y);
        matrix3x2fc.transformPosition(i, i4, vector2f);
        float f13 = Math.min(f9, vector2f.x);
        float f14 = Math.max(f10, vector2f.x);
        float f15 = Math.min(f11, vector2f.y);
        float f16 = Math.max(f12, vector2f.y);
        matrix3x2fc.transformPosition(i3, i4, vector2f);
        float f17 = Math.min(f13, vector2f.x);
        float f18 = Math.max(f14, vector2f.x);
        float f19 = Math.min(f15, vector2f.y);
        float f20 = Math.max(f16, vector2f.y);
        ScreenRect screenRect2 = new ScreenRect(MathHelper.floor(f17), MathHelper.floor(f19), MathHelper.ceil(f18 - f17), MathHelper.ceil(f20 - f19));
        return screenRect != null ? screenRect.intersection(screenRect2) : screenRect2;
    }
}
