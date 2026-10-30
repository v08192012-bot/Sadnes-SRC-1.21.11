package tech.sadnes.client.ui.clickgui.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;

public final class HoverTooltip {
    private static final float PADDING = 5.0F;
    private static final float TEXT_SIZE = 6.0F;
    private static final float TITLE_SIZE = 7.5F;
    private static final float LINE_GAP = 2.0F;
    private static final float SECTION_GAP = 4.0F;
    private static final float SCREEN_MARGIN = 4.0F;
    private static final float BULLET_GAP = 1.5F;
    private static final float ENTRY_GAP = 4.0F;
    private static final float ANCHOR_OFFSET = 6.0F;
    private static final float ENTRY_INDENT = 8.0F;
    private static final float MAX_WIDTH = 135.0F;
    private static final float ICON_SIZE = 6.0F;
    private static final float MIN_PADDING = 4.0F;
    private static boolean enabled = true;
    private final Animation fadeAnimation = new Animation();
    private final StopWatch hoverTimer = new StopWatch();
    private boolean hovered;
    private String cacheKey;
    private List<HoverTooltip.TooltipLine> lines = new ArrayList<>();
    private float width;
    private float height;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean flag) {
        enabled = flag;
    }

    public void reset() {
        this.fadeAnimation.forceValue(0.0);
        this.hovered = false;
        this.hoverTimer.reset();
    }

    public void markHovered() {
        this.hovered = true;
    }

    public void update(String string) {
        this.update(null, string, List.of());
    }

    public void update(String string, List<HoverTooltip.TooltipEntry> tooltipEntries) {
        this.update(null, string, tooltipEntries);
    }

    public void update(String string, String string2, List<HoverTooltip.TooltipEntry> tooltipEntries) {
        this.fadeAnimation.update();
        boolean flag;
        if (string2 != null) {
            flag = !string2.isBlank();
        } else {
            flag = false;
        }

        boolean flag2;
        if (string != null) {
            flag2 = !string.isBlank();
        } else {
            flag2 = false;
        }

        if (this.hovered && enabled && (flag || flag2)) {
            this.layout(string, string2, tooltipEntries);
            if (this.hoverTimer.hasPassed(500.0)) {
                this.fadeAnimation.animate(1.0, 0.2, Easings.QUAD_OUT);
                this.hovered = false;
                return;
            }
        } else {
            this.hoverTimer.reset();
            this.fadeAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        }

        this.hovered = false;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, HoverTooltip.Side side, float f5) {
        float f6 = this.fadeAnimation.get();
        if (!(f6 <= 0.001F) && !this.lines.isEmpty()) {
            float f7 = MinecraftClient.getInstance().getWindow().getScaledWidth();
            float f8 = f + f3 + 6.0F;
            float f9 = f - this.width - 6.0F;
            float f10 = side == HoverTooltip.Side.RIGHT ? (f8 + this.width > f7 - 4.0F ? f9 : f8) : (f9 < 4.0F ? f8 : f9);
            float f11 = GuiRenderUtil.clampToScreenX(f10, this.width);
            float f12 = GuiRenderUtil.clampToScreenY(f2 + f4 / 2.0F - this.height / 2.0F, this.height);
            float f13 = f5 * f6;
            GuiRenderUtil.drawPopup(drawContext, f11, f12, this.width, this.height, f13, 6.0F);
            RenderUtil renderUtil = new RenderUtil(drawContext, f11, f12, this.width, this.height);
            float f14 = 5.0F;
            int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f13);

            for (int i2 = 0; i2 < this.lines.size(); i2++) {
                HoverTooltip.TooltipLine tooltipLine = this.lines.get(i2);
                float f15 = f14 + tooltipLine.topGap();
                if (i2 > 0) {
                    f15 += 2.0F;
                }

                int i3 = tooltipLine.color() != null ? ColorUtil.multiplyAlpha(tooltipLine.color(), f13) : i;
                MsdfFont msdfFont = tooltipLine.bold() ? Fonts.INTER_SEMIBOLD : Fonts.INTER_MEDIUM;
                if (tooltipLine.icon() != null) {
                    renderUtil.icon(tooltipLine.icon(), 5.0F, f15 + 3.0F - 3.0F, 6.0F, i3);
                }

                if (tooltipLine.bullet()) {
                    renderUtil.textTopLeft(Fonts.INTER_MEDIUM, "-", 5.0F, f15, i, 6.0F);
                }

                if (!tooltipLine.text().isEmpty()) {
                    renderUtil.textTopLeft(msdfFont, tooltipLine.text(), 5.0F + tooltipLine.indent(), f15, i3, tooltipLine.size());
                }

                f14 = f15 + tooltipLine.size();
            }
        }
    }

    private void layout(String string, String string2, List<HoverTooltip.TooltipEntry> tooltipEntries) {
        String string3 = buildCacheKey(string, string2, tooltipEntries);
        if (!string3.equals(this.cacheKey)) {
            this.cacheKey = string3;
            this.lines = new ArrayList<>();
            float f = Fonts.INTER_MEDIUM.getWidth("-", 6.0F) + 3.0F;
            float f2 = 0.0F;
            if (string != null && !string.isBlank()) {
                for (String string4 : wrapText(Fonts.INTER_SEMIBOLD, string, 125.0F, 7.5F)) {
                    this.lines.add(new HoverTooltip.TooltipLine(string4, 0.0F, 7.5F, true, false, null, null, 0.0F));
                }

                f2 = 4.0F;
            }

            if (string2 != null) {
                for (String string5 : string2.split("\n", -1)) {
                    if (string5.isBlank()) {
                        if (!this.lines.isEmpty()) {
                            f2 = Math.max(f2, 4.0F);
                        }
                    } else {
                        String string6 = string5.strip();
                        boolean flag;
                        float f3;
                        if (!string6.startsWith("- ") && !string6.startsWith("— ") && !string6.startsWith("* ")) {
                            flag = false;
                            f3 = 0.0F;
                        } else {
                            flag = true;
                            f3 = f;
                        }

                        float f4 = f3;
                        String string7 = flag ? string6.substring(2).strip() : string6;
                        boolean flag2 = true;

                        for (String string8 : wrapText(Fonts.INTER_MEDIUM, string7, 125.0F - f4, 6.0F)) {
                            float f5 = flag2 ? (f2 > 0.0F ? f2 : (flag ? 1.5F : 0.0F)) : 0.0F;
                            this.lines.add(new HoverTooltip.TooltipLine(string8, f4, 6.0F, false, flag && flag2, null, null, f5));
                            f2 = 0.0F;
                            flag2 = false;
                        }
                    }
                }
            }

            float f6 = 125.0F - 8.0F;

            for (HoverTooltip.TooltipEntry tooltipEntry : tooltipEntries) {
                List<String> strings = wrapText(Fonts.INTER_MEDIUM, tooltipEntry.text(), f6, 6.0F);
                if (strings.isEmpty()) {
                    strings = List.of("");
                }

                boolean flag3 = true;

                for (String string9 : strings) {
                    float f7 = flag3 ? Math.max(f2, 4.0F) : 0.0F;
                    this.lines.add(new HoverTooltip.TooltipLine(string9, 8.0F, 6.0F, false, false, flag3 ? tooltipEntry.icon() : null, tooltipEntry.color(), f7));
                    if (flag3) {
                        f2 = 0.0F;
                    }

                    flag3 = false;
                }
            }

            float f8 = 0.0F;

            for (HoverTooltip.TooltipLine tooltipLine : this.lines) {
                MsdfFont msdfFont = tooltipLine.bold() ? Fonts.INTER_SEMIBOLD : Fonts.INTER_MEDIUM;
                float f9 = tooltipLine.indent() + msdfFont.getWidth(tooltipLine.text(), tooltipLine.size());
                if (f9 > f8) {
                    f8 = f9;
                }
            }

            this.width = f8 + 10.0F;
            float f10 = 5.0F;

            for (int i = 0; i < this.lines.size(); i++) {
                float f11 = f10 + this.lines.get(i).topGap();
                if (i > 0) {
                    f11 += 2.0F;
                }

                f10 = f11 + this.lines.get(i).size();
            }

            this.height = f10 + 5.0F;
        }
    }

    private static String buildCacheKey(String string, String string2, List<HoverTooltip.TooltipEntry> tooltipEntries) {
        StringBuilder builder = new StringBuilder();
        if (string != null) {
            builder.append(string);
        }

        builder.append('\u0000').append(string2);

        for (HoverTooltip.TooltipEntry tooltipEntry : tooltipEntries) {
            builder.append('\n').append(tooltipEntry.color()).append('\n').append(tooltipEntry.text());
        }

        return builder.toString();
    }

    private static List<String> wrapText(MsdfFont msdfFont, String string, float f, float f2) {
        ArrayList arrayList = new ArrayList();
        StringBuilder builder = new StringBuilder();

        for (String string2 : string.trim().split("\\s+")) {
            if (!string2.isEmpty()) {
                while (msdfFont.getWidth(string2, f2) > f && string2.length() > 1) {
                    if (builder.length() > 0) {
                        arrayList.add(builder.toString());
                        builder.setLength(0);
                    }

                    int i = fitChars(msdfFont, string2, f, f2);
                    arrayList.add(string2.substring(0, i));
                    string2 = string2.substring(i);
                }

                if (builder.length() == 0) {
                    builder.append(string2);
                } else if (msdfFont.getWidth(builder + " " + string2, f2) <= f) {
                    builder.append(' ').append(string2);
                } else {
                    arrayList.add(builder.toString());
                    builder.setLength(0);
                    builder.append(string2);
                }
            }
        }

        if (builder.length() > 0) {
            arrayList.add(builder.toString());
        }

        return arrayList;
    }

    private static int fitChars(MsdfFont msdfFont, String string, float f, float f2) {
        int i = 1;

        while (i < string.length() && msdfFont.getWidth(string.substring(0, i + 1), f2) <= f) {
            i++;
        }

        return i;
    }

    public enum Side {
        LEFT,
        RIGHT;
    }

    public record TooltipEntry(Icon icon, int color, String text) {
    }

    record TooltipLine(String text, float indent, float size, boolean bold, boolean bullet, Icon icon, Integer color, float topGap) {
    }
}
