package tech.sadnes.client.ui.menu;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class MenuBackgroundRenderer {
    private static final int DARK_OVERLAY_COLOR = -16053492;

    private MenuBackgroundRenderer() {
    }

    public static void renderBackground(DrawContext drawContext, float f) {
        float f2 = GuiScale.getScaledWidth();
        float f3 = GuiScale.getScaledHeight();
        float f4;
        float f5;
        if (f2 / f3 > 1.7777778F) {
            f4 = f2;
            f5 = f2 / 1.7777778F;
        } else {
            f5 = f3;
            f4 = f3 * 1.7777778F;
        }

        float f6 = (f2 - f4) / 2.0F;
        float f7 = (f3 - f5) / 2.0F;
        GuiRenderUtil.bindTexture(Texture.MENU_BACKGROUND);
        if (GuiRenderUtil.getBoundTexture() != null) {
            new RenderUtil(drawContext, f6, f7, f4, f5).frostedGlass(f, Radius.zero());
        }

        new RenderUtil(drawContext, f6, f7, f4, f5).spriteBlended(AnimatedIcon.BG, 0.0F, 0.0F, f4, f5, Theme.withAlpha(ColorUtil.WHITE, f));
        renderBlurOverlay(drawContext, f2, f3, 1.0F, f);
    }

    public static void renderBlurOverlay(DrawContext drawContext, float f, float f2, float f3, float f4) {
        if (!(f4 <= 0.001F)) {
            float f5 = f3 < 0.0F ? 0.0F : Math.min(f3, 1.0F);
            new RenderUtil(drawContext, 0.0F, 0.0F, f, f2).blur(f4, Radius.zero(), RenderUtil.BlurQuality.LOW, true);
            if (f5 > 0.001F) {
                new RenderUtil(drawContext, 0.0F, 0.0F, f, f2).blur(f4 * f5, Radius.zero(), RenderUtil.BlurQuality.MAX, true);
            }

            new RenderUtil(drawContext, 0.0F, 0.0F, f, f2).rect(Theme.withAlpha(ColorUtil.BLACK, f4 * 0.2F), Radius.zero());
        }
    }

    public static void renderDimmedBlur(DrawContext drawContext, float f, float f2, float f3) {
        RenderUtil renderUtil = new RenderUtil(drawContext, 0.0F, 0.0F, f, f2);
        renderUtil.blur(f3, Radius.zero(), RenderUtil.BlurQuality.LOW, true);
        renderUtil.rect(-16053492, f3 * 0.32F, Radius.zero());
    }
}
