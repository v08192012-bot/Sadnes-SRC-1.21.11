package tech.sadnes.client.setting;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.Side;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class SettingWidgets implements IInteractable {
    public static final float SWITCH_WIDTH = 14.0F;
    public static final float SWITCH_HEIGHT = 9.0F;
    public static final float CHECKBOX_SIZE = 10.0F;
    public static final float SPACING_SMALL = 2.0F;
    public static final float SPACING = 3.0F;
    public static final float SWATCH_SIZE = 10.0F;
    public static final float SWATCH_GAP = 1.5F;
    public static final float ICON_SIZE = 8.0F;
    public static final float PADDING = 6.0F;

    private SettingWidgets() {
    }

    public static float getSwatchRowWidth(int i) {
        return i <= 0 ? 0.0F : 6.0F + i * 10.0F + Math.max(0, i - 1) * 1.5F;
    }

    public static void drawSwitch(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        int i = ColorUtil.multiplyAlpha(ColorUtil.interpolate(0, Theme.getHighlight(), f5), f6);
        int i2 = ColorUtil.darken(i, 0.87F);
        int i3 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getAccentSoft(), Theme.getNearWhite(), f5), f6);
        ColorUtil.darken(i3, 0.74F);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f6, f4 / 2.0F).rect(i2, Radius.all(f4 / 2.0F));
        float f7 = Math.max(0.0F, Math.min(1.0F, 4.0F * f5 * (1.0F - f5)));
        float f8 = f4 - 2.0F;
        float f9 = f8 * (1.0F + 0.3F * f7);
        float f10 = f8 * (1.0F - 0.25F * f7);
        float f11 = f + 1.0F + f5 * (f3 - 2.0F - f9);
        float f12 = f2 + f4 / 2.0F - f10 / 2.0F;
        new RenderUtil(drawContext, f11, f12, f9, f10).rect(i3, Radius.all(f10 / 2.1F));
    }

    public static void drawDefaultSwitch(DrawContext drawContext, float f, float f2, float f3, float f4) {
        drawSwitch(drawContext, f, f2, 14.0F, 9.0F, f3, f4);
    }

    public static void drawCheckbox(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        Radius radius = Radius.all(f3 / 2.5F);
        int i = ColorUtil.multiplyAlpha(ColorUtil.interpolate(0, Theme.getHighlight(), f4), f5);
        int i2 = ColorUtil.multiplyAlpha(Theme.withAlpha(ColorUtil.BLACK, f4), f5);
        float f6 = f3 * (0.5F + 0.2F * f4);
        float f7 = (f3 - f6) / 2.0F;
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f3, f5, f3 / 3.0F);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f3);
        float f8 = -(f3 * 0.2F * (1.0F - f4));
        renderUtil.expand(Side.ALL, f8);
        renderUtil.rect(i, radius);
        renderUtil.expand(Side.ALL, -f8);
        renderUtil.icon(Icon.CHECK, f7, f7, f6, i2);
    }

    public static void drawSlider(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        int i = ColorUtil.multiplyAlpha(Theme.getHighlight(), f8);
        int i2 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getAccentSoft(), Theme.getNearWhite(), f5), f8);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f8, f4 / 2.0F);
        new RenderUtil(drawContext, f - 0.1F, f2, f3 * f5, f4).rect(i, Radius.all(f4 / 2.0F));
        float f9 = f6 * (1.0F + 0.3F * f7);
        float f10 = f6 * (1.0F - 0.25F * f7);
        float f11 = f + (f3 - f9) * f5;
        float f12 = f2 + f4 / 2.0F - f10 / 2.0F;
        new RenderUtil(drawContext, f11, f12, f9, f10).rect(i2, Radius.all(f10 / 2.0F));
    }

    public static void drawRangeSlider(
        DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10
    ) {
        int i = ColorUtil.multiplyAlpha(Theme.getHighlight(), f10);
        int i2 = ColorUtil.multiplyAlpha(Theme.getNearWhite(), f10);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f10, f4 / 2.0F);
        float f11 = Math.max(f3 - f7, 0.0F);
        float f12 = f + f7 / 2.0F + f11 * f5;
        float f13 = f + f7 / 2.0F + f11 * f6;
        new RenderUtil(drawContext, f12, f2, Math.max(0.0F, f13 - f12), f4).rect(i, Radius.all(f4 / 2.0F));
        drawSliderKnob(drawContext, f12, f2, f4, f7, f8, i2);
        drawSliderKnob(drawContext, f13, f2, f4, f7, f9, i2);
    }

    private static void drawSliderKnob(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i) {
        float f6 = f4 * (1.0F + 0.3F * f5);
        float f7 = f4 * (1.0F - 0.25F * f5);
        new RenderUtil(drawContext, f - f6 / 2.0F, f2 + f3 / 2.0F - f7 / 2.0F, f6, f7).rect(i, Radius.all(f7 / 2.0F));
    }

    public static void drawValueBox(DrawContext drawContext, float f, float f2, float f3, float f4, String string, float f5) {
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSurface(), 0.35F), f5);
        int i2 = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f5);
        new RenderUtil(drawContext, f, f2, f3, f4).rect(i, Radius.all(4.0F)).textCenter(Fonts.INTER_MEDIUM, string, i2, 5.5F, false);
    }

    public static void drawSurfaceLabel(DrawContext drawContext, float f, float f2, float f3, float f4, String string, float f5, float f6) {
        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSurface(), f5), f6);
        int i = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f6);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f6, 3.0F).textCenter(Fonts.INTER_MEDIUM, string, i, 5.5F, false);
    }

    public static void drawExpandArrow(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = f + f3 - 6.0F - 3.5F;
        float f8 = f2 + f4 / 2.0F - 6.0F / 2.0F;
        int i = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f6);
        RenderUtil.drawIcon(drawContext, Icon.CARET_RIGHT, f7, f8, 6.0F, 90.0F * f5, i);
    }

    public static void drawColorSwatch(DrawContext drawContext, float f, float f2, float f3, float f4, int i, float f5) {
        int i2 = ColorUtil.multiplyAlpha(i, f5);
        new RenderUtil(drawContext, f + 1.2F, f2 + 1.2F, f3 - 2.4F, f4 - 2.4F).rect(i2, Radius.all(3.0F));
    }

    public static void drawTextField(
        DrawContext drawContext, float f, float f2, float f3, float f4, String string, String string2, float f5, float f6, float f7
    ) {
        int i = ColorUtil.multiplyAlpha(Theme.getSurfaceDeep(), f7);
        int i2 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getOutline(), 0.28F + f6 * 0.12F), f7);
        ColorUtil.multiplyAlpha(Theme.getSecondaryText(), 0.9F * f7);
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f7);
        new RenderUtil(drawContext, f, f2, f3, f4)
            .rect(i, Radius.all(4.0F))
            .shadow(0.5F, i2, Radius.all(4.0F))
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, string2, 4.0F, 0.0F, f5, i3, 5.5F);
    }

    public static void drawDropdownBackground(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSurface(), 0.3F * f5), f6);
        int i2 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getOutline(), 0.26F * f5), f6);
        new RenderUtil(drawContext, f, f2, f3, f4).rect(i, Radius.all(4.0F)).shadow(0.5F, i2, Radius.all(4.0F));
    }

    public static void drawDropdownOption(
        DrawContext drawContext, float f, float f2, float f3, float f4, String string, boolean flag, boolean flag2, float f5, float f6, float f7
    ) {
        float f8 = flag2 ? 0.14F : 0.08F;
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSurfaceDark(), (f8 + f5 * 0.08F) * f6), f7);
        int i2 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getHighlight(), (0.1F + f5 * 0.9F) * f6), f7);
        int i3 = Theme.withAlpha(ColorUtil.multiplyAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f7), f6);
        new RenderUtil(drawContext, f, f2, f3, f4).rect(i, Radius.all(3.0F));
        new RenderUtil(drawContext, f + 2.5F + f5 * 2.5F, f2 + f4 / 2.0F - 2.0F * f5, 1.0F, 4.0F * f5).rect(i2, Radius.all(0.5F));
        new RenderUtil(drawContext, f, f2, f3, f4).textMiddleLeft(Fonts.INTER_MEDIUM, string, 5.0F + f5 * 3.5F, 0.0F, i3, 6.5F);
    }

    public static void drawBindBox(DrawContext drawContext, float f, float f2, float f3, float f4, String string, float f5, float f6) {
        ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSurfaceDeep(), Theme.getHighlight(), f5), f6);
        int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f6);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f6, 4.0F).textCenter(Fonts.INTER_MEDIUM, string, i, 5.5F, false);
    }

    public static void drawWaitingBind(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        float f6 = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 200.0);
        int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f5);
        int i2 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.35F + 0.55F * f6), f5);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f5, 4.0F)
            .shadow(0.5F, i2, Radius.all(4.0F))
            .textCenter(Fonts.INTER_MEDIUM, getWaitingText(), i, 5.5F, false);
    }

    public static String getWaitingText() {
        return Language.translate("setting.bind.waiting", "Press a key");
    }

    public static void drawLabelBox(DrawContext drawContext, float f, float f2, float f3, float f4, String string, float f5) {
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSurface(), 0.35F), f5);
        int i2 = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f5);
        new RenderUtil(drawContext, f, f2, f3, f4).rect(i, Radius.all(4.0F)).textCenter(Fonts.INTER_MEDIUM, string, i2, 5.5F, false);
    }

    public static String ellipsize(String string, float f) {
        if (string != null && !string.isEmpty() && !(f <= 0.0F)) {
            if (Fonts.INTER_MEDIUM.getWidth(string, 5.5F) <= f) {
                return string;
            }

            String string2 = "...";
            if (Fonts.INTER_MEDIUM.getWidth(string2, 5.5F) > f) {
                return "";
            }

            for (int i = string.length() - 1; i > 0; i--) {
                String string3 = string.substring(0, i) + string2;
                if (Fonts.INTER_MEDIUM.getWidth(string3, 5.5F) <= f) {
                    return string3;
                }
            }

            return string2;
        } else {
            return "";
        }
    }
}
