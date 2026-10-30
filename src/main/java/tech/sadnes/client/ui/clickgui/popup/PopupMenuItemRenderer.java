package tech.sadnes.client.ui.clickgui.popup;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class PopupMenuItemRenderer {
    public static final float ROW_HEIGHT = 20.0F;
    public static final float ICON_X = 8.0F;
    private static final float ICON_SIZE = 8.5F;
    private static final float TEXT_SIZE = 6.5F;

    private PopupMenuItemRenderer() {
    }

    public static void renderItem(DrawContext drawContext, float f, float f2, float f3, Icon icon2, String string, float f4, boolean flag, float f5) {
        GuiRenderUtil.drawHighlight(drawContext, f + 4.0F, f2 + 1.0F, f3 - 8.0F, 18.0F, f5 * f4 * 0.67F, Radius.all(5.0F));
        int i = flag ? Theme.getPrimary() : Theme.withAlpha(Theme.getPrimary(), 0.55F + 0.45F * f4);
        int i2 = flag ? Theme.getPrimary() : ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f4);
        new RenderUtil(drawContext, f, f2, f3, 20.0F)
            .icon(icon2, 8.0F, 5.75F, 8.5F, ColorUtil.multiplyAlpha(i, f5))
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 22.5F, 0.0F, ColorUtil.multiplyAlpha(i2, f5), 6.5F);
    }
}
