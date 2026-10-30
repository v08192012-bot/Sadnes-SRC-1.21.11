package tech.sadnes.client.ui.menu;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class MenuTooltip {
    private MenuTooltip() {
    }

    public static void render(DrawContext drawContext, float f, float f2, String string, float f3, float f4) {
        if (!(f3 <= 0.004F) && Fonts.INTER_MEDIUM != null) {
            float f5 = 14.0F + Fonts.INTER_MEDIUM.getWidth(string, 6.0F);
            float f6 = f2 - 14.0F / 2.0F;
            Radius radius = Radius.all(6.0F);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f + f5 / 2.0F, f2, 0.92F + 0.08F * f4);
            GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f, f6, f5, 14.0F, f3 * 0.4F, 6.0F);
            RenderUtil renderUtil = new RenderUtil(drawContext, f, f6, f5, 14.0F);
            renderUtil.rect(Theme.withAlpha(ColorUtil.BLACK, 0.45F * f3), radius);
            if (GraphicsQuality.getCurrent().isEffectsEnabled()) {
                renderUtil.water(Theme.getPrimary(), f3, f4, 0.5F, 0.5F, (float)(System.currentTimeMillis() % 1000000L) / 1000.0F, radius);
            }

            renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, string, 7.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f3), 6.0F);
            MatrixUtil.pop(drawContext);
        }
    }
}
