package tech.sadnes.client.ui.hud;

import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.api.mixin.accessor.BossBarHudAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.feature.impl.hud.Watermark;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class HudPanelUtil {
    public static final float PANEL_PADDING = 10.0F;
    public static final float PANEL_GAP = 2.0F;
    public static final float HEADER_HEIGHT = 13.0F;
    public static final float ROW_GAP = 2.0F;
    public static final float ROW_PADDING = 5.0F;
    public static final float HEADER_PADDING = 4.0F;
    public static final float ICON_GAP = 2.0F;
    public static final float TEXT_PADDING = 4.0F;
    public static final float SEPARATOR_GAP = 2.0F;
    public static final float ALPHA_SCALE = 1.0F;
    public static final float SEPARATOR_WIDTH = 0.5F;
    public static final float ICON_OFFSET_Y = 2.5F;
    public static final float SEPARATOR_HEIGHT = 3.0F;
    public static final float SEPARATOR_RADIUS = 0.25F;
    public static final float HEADER_INNER_PADDING = 4.0F;
    public static final float ROW_HEIGHT = 13.0F;
    public static final float ROW_SPACING = 2.0F;
    public static final float HEADER_ICON_SIZE = 8.0F;
    public static final float HEADER_RADIUS = 4.5F;
    public static final float ENTRY_PADDING = 4.0F;
    public static final float ENTRY_GAP = 3.0F;
    public static final float SMALL_RADIUS = 3.5F;
    public static final float PANEL_MARGIN = 5.0F;
    public static final float HOVER_ALPHA = 0.3F;
    public static final float BORDER_WIDTH = 1.0F;
    public static final double FADE_DURATION = 0.25;
    public static final double OPEN_DURATION = 0.4;
    public static final double CLOSE_DURATION = 0.4;
    private static final Radius SMALL_ROUNDING = Radius.all(3.5F);
    private static final Radius SEPARATOR_ROUNDING = Radius.all(0.25F);

    public static Radius getSmallRadius() {
        return SMALL_ROUNDING;
    }

    public static int applyAlpha(int i, float f) {
        return ColorUtil.setAlphaf(i, f * 1.0F);
    }

    public static RenderUtil drawHeader(
        DrawContext drawContext, float f, float f2, float f3, float f4, String string, Icon icon2, MsdfFont msdfFont, MsdfFont msdfFont2, float f5
    ) {
        int i = applyAlpha(Theme.getTextColor(), f4);
        int i2 = ColorUtil.setAlphaf(Theme.getPrimary(), f4);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, 13.0F);
        if (!renderScriptPanel(drawContext, f, f2, f3, 13.0F, f4, 4.5F)) {
            renderUtil = GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.LOW, true, f, f2, f3, 13.0F, f4, 4.5F);
        }

        renderUtil.icon(icon2, 4.0F, 2.5F, 8.0F, i2);
        drawSeparator(drawContext, renderUtil.getX() + 4.0F + 8.0F + 4.0F * 0.25F, renderUtil.getY() + renderUtil.getHeight() / 2.0F, f4);
        renderUtil.textMiddleLeft(msdfFont, string, 4.0F + 8.0F + 4.0F, 0.0F, i, f5);
        return renderUtil;
    }

    public static float getHeaderWidth(float f, float f2, boolean flag, boolean flag2) {
        return 4.0F + f + 4.0F + f2 + 4.0F;
    }

    public static RenderUtil drawSmallPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
        if (!renderScriptSmallPanel(drawContext, f, f2, f3, f4, f5, 3.5F)) {
            renderUtil.blur(f5, SMALL_ROUNDING, RenderUtil.BlurQuality.LOW);
            renderUtil.rect(Theme.getBase(), f5 * Theme.dim(0.185F), SMALL_ROUNDING);
        }

        return renderUtil;
    }

    public static RenderUtil drawSmallPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
        Radius radius = Radius.all(f6);
        if (!renderScriptSmallPanel(drawContext, f, f2, f3, f4, f5, f6)) {
            renderUtil.blur(f5, radius, RenderUtil.BlurQuality.LOW);
            renderUtil.rect(Theme.getBase(), f5 * Theme.dim(0.185F), radius);
        }

        return renderUtil;
    }

    public static RenderUtil drawPanelAutoRadius(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        float f6 = f4 / 2.4F;
        return renderScriptPanel(drawContext, f, f2, f3, f4, f5, f6)
            ? new RenderUtil(drawContext, f, f2, f3, f4)
            : GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f, f2, f3, f4, f5, f6);
    }

    public static RenderUtil drawPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        return renderScriptPanel(drawContext, f, f2, f3, f4, f5, f6)
            ? new RenderUtil(drawContext, f, f2, f3, f4)
            : GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f, f2, f3, f4, f5, f6);
    }

    public static RenderUtil drawPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, Radius radius) {
        return renderScriptPanel(drawContext, f, f2, f3, f4, f5, radius.getMax())
            ? new RenderUtil(drawContext, f, f2, f3, f4)
            : GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f, f2, f3, f4, f5, radius);
    }

    public static RenderUtil drawAnimatedPanel(
        DrawContext drawContext, float f, float f2, float f3, float f4, float f5, Radius radius, float f6, float f7, Easing easing
    ) {
        return renderScriptPanel(drawContext, f, f2, f3, f4, f5, radius.getMax())
            ? new RenderUtil(drawContext, f, f2, f3, f4)
            : GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f, f2, f3, f4, f5, radius, f6, f7, easing);
    }

    private static boolean renderScriptPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        return scriptManager != null && scriptManager.renderHudHeaderBackground(drawContext, f, f2, f3, f4, f5, f6);
    }

    private static boolean renderScriptSmallPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        return scriptManager != null && scriptManager.renderHudCellBackground(drawContext, f, f2, f3, f4, f5, f6);
    }

    public static float getTopOffset() {
        float f = 0.0F;

        try {
            Map map = ((BossBarHudAccessor)MinecraftClient.getInstance().inGameHud.getBossBarHud()).sadnes$getBossBars();
            if (map != null) {
                int i = map.size();
                if (i > 0) {
                    f = 12.0F + i * 19.0F;
                }
            }
        } catch (Throwable throwable) {
        }

        return Math.max(f, Watermark.getTopOffset());
    }

    public static void drawSeparator(DrawContext drawContext, float f, float f2, float f3) {
        new RenderUtil(drawContext, f, f2 - 1.5F, 0.5F, 3.0F).rect(ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.3F), SEPARATOR_ROUNDING);
    }
}
