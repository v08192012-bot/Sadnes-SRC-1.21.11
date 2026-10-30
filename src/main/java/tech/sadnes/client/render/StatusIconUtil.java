package tech.sadnes.client.render;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.MarkerLevel;
import tech.sadnes.client.util.color.ColorUtil;

public final class StatusIconUtil {
    public static final int BUG_COLOR = ColorUtil.fromHex("ffa733");
    private static final float ICON_SPACING = 2.0F;

    private StatusIconUtil() {
    }

    public static float drawIcons(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, boolean flag, boolean flag2, MarkerLevel markerLevel) {
        float f6 = 0.0F;
        float f7 = f3 / 2.0F - f4 / 2.0F;
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f4, f3);
        if (flag2 && markerLevel != null) {
            renderUtil.icon(Icon.WARNING, 0.0F, f7, f4, ColorUtil.multiplyAlpha(markerLevel.getColor(), f5));
            f6 = 0.0F + (f4 + 2.0F);
        }

        if (flag) {
            renderUtil.icon(Icon.BUG, f6, f7, f4, ColorUtil.multiplyAlpha(BUG_COLOR, f5));
            f6 += f4 + 2.0F;
        }

        return f6;
    }

    public static int getNameColor(int i, float f, boolean flag, boolean flag2, MarkerLevel markerLevel) {
        if (flag2 && markerLevel != null) {
            return ColorUtil.multiplyAlpha(markerLevel.getColor(), f);
        } else {
            return flag ? ColorUtil.multiplyAlpha(BUG_COLOR, f) : i;
        }
    }
}
