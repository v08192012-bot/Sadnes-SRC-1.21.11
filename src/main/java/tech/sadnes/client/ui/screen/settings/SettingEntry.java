package tech.sadnes.client.ui.screen.settings;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.FavoritesPrefs;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.OptionWidget;
import tech.sadnes.client.util.color.Theme;

public final class SettingEntry implements IRenderable {
    private static final float ICON_OFFSET = 8.0F;
    private static final float ICON_SIZE = 7.0F;
    private static final float TEXT_SIZE = 7.0F;
    private final Icon icon;
    private final String name;
    private final OptionWidget widget;
    private final boolean header;
    private boolean collapsed;
    private final Animation expandAnimation = new Animation();
    private final IconHoverAnimator iconRenderer = new IconHoverAnimator();
    private final IconHoverAnimator starIcon = new IconHoverAnimator();
    private final IconHoverAnimator resetIcon = new IconHoverAnimator();
    private final IconHoverAnimator chevronIcon = new IconHoverAnimator();
    private String[] keywords;
    private static final String[] NO_KEYWORDS = new String[0];
    private boolean showFavorite;
    private float resetX;
    private float resetY;
    private float resetWidth;
    private float resetHeight;
    private float x;
    private float y;
    private float width;
    private float height;
    private float starX;
    private float starY;
    private float starWidth;
    private float starHeight;

    public String getName() {
        return this.name;
    }

    public OptionWidget getWidget() {
        return this.widget;
    }

    public void setShowFavorite(boolean flag) {
        this.showFavorite = flag;
    }

    public SettingEntry(Icon icon2, String string, OptionWidget optionWidget) {
        this.keywords = NO_KEYWORDS;
        this.icon = icon2;
        this.name = string;
        this.widget = optionWidget;
        this.header = false;
    }

    private SettingEntry(String string) {
        this.keywords = NO_KEYWORDS;
        this.icon = null;
        this.name = string;
        this.widget = null;
        this.header = true;
    }

    public static SettingEntry createHeader(String string) {
        return new SettingEntry(string);
    }

    public SettingEntry withKeywords(String... strings) {
        if (strings != null) {
            this.keywords = strings;
            return this;
        } else {
            this.keywords = NO_KEYWORDS;
            return this;
        }
    }

    public boolean isHeader() {
        return this.header;
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public void toggleCollapsed() {
        if (!this.collapsed) {
            this.collapsed = true;
        } else {
            this.collapsed = false;
        }
    }

    public float getExpandProgress() {
        this.expandAnimation.update();
        if (this.collapsed) {
            this.expandAnimation.animate(0.0, 0.25, Easings.QUAD_OUT);
            return this.expandAnimation.get();
        } else {
            this.expandAnimation.animate(1.0, 0.25, Easings.QUAD_OUT);
            return this.expandAnimation.get();
        }
    }

    public boolean matchesQuery(String string) {
        if (this.header) {
            return false;
        }

        if (this.name.toLowerCase(Locale.ROOT).contains(string)) {
            return true;
        }

        for (String string2 : this.keywords) {
            if (string2 != null && string2.toLowerCase(Locale.ROOT).contains(string)) {
                return true;
            }
        }

        return false;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        if (this.header) {
            this.renderHeader(drawContext, f, f2, f3, f4, i, i2, f5);
        } else {
            boolean flag = this.isHovered(i, i2, f, f2, f3, f4);
            int i3 = Theme.withAlpha(Theme.getTextColor(), f5);
            float f6 = this.widget.getWidgetHeight();
            float f7 = f + f3 - f6;
            float f8 = f7 - 6.0F - 9.0F;
            this.resetX = f8 - 2.0F;
            this.resetY = f2;
            this.resetWidth = 9.0F + 4.0F;
            this.resetHeight = f4;
            float f9 = this.showFavorite ? 12.0F : 0.0F;
            if (this.showFavorite) {
                this.starX = f;
                this.starY = f2;
                this.starWidth = f9;
                this.starHeight = f4;
                boolean flag2 = FavoritesPrefs.isFavorite(this.name);
                boolean flag3;
                if (flag) {
                    flag3 = this.isHovered(i, i2, this.starX, this.starY, this.starWidth, this.starHeight);
                } else {
                    flag3 = false;
                }

                if (flag2 || flag) {
                    int i4 = flag2 ? Theme.withAlpha(Theme.getPrimary(), f5) : Theme.withAlpha(Theme.getSecondaryText(), 0.5F * f5);
                    this.starIcon.render(drawContext, Icon.STAR, f + 2.0F, f2 + (f4 - 8.0F) / 2.0F, 8.0F, i4, flag3 ? 1.0F : 0.0F);
                }
            }

            float f10 = this.showFavorite ? f9 : 8.0F;
            RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
            this.iconRenderer.render(drawContext, this.icon, f + f10, f2 + (f4 - 7.0F) / 2.0F, 7.0F, i3, flag ? 1.0F : 0.0F);
            float f11 = f10 + 7.0F + 6.0F;
            float f12 = f8 - 6.0F - (f + f11);
            renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, truncate(this.name, f12), f11, 0.0F, i3, 7.0F);
            if (this.widget.isFocused()) {
                int i5 = Theme.getPrimary();
                if (flag) {
                    boolean flag4 = this.isHovered(i, i2, f8, f2, 9.0F, f4);
                    this.resetIcon.render(drawContext, Icon.REFRESH, f8, f2 + (f4 - 7.0F) / 2.0F, 7.0F, Theme.withAlpha(i5, f5), flag4 ? 1.0F : 0.0F);
                } else {
                    float f13 = f8 + 9.0F / 2.0F;
                    new RenderUtil(drawContext, f13 - 2.0F, f2 + f4 / 2.0F - 2.0F, 4.0F, 4.0F).rect(Theme.withAlpha(i5, 0.9F * f5), Radius.all(2.0F));
                }
            }

            this.widget.renderWidget(drawContext, f7, f2, f6, f4, i, i2, f5);
        }
    }

    private void renderHeader(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        boolean flag = this.isHovered(i, i2, f, f2, f3, f4);
        if (flag) {
            GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, f4, f5 * 0.6F, Radius.all(5.0F));
        }

        int i3 = Theme.withAlpha(Theme.getSecondaryText(), (flag ? 0.95F : 0.78F) * f5);
        float f6 = f + 8.0F - 1.0F;
        float f7 = f2 + f4 / 2.0F;
        boolean flag2 = !this.collapsed;
        if (flag2) {
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f6 + 6.0F / 2.0F, f7, 90.0F);
        }

        this.chevronIcon.render(drawContext, Icon.CHEVRON_RIGHT, f6, f7 - 6.0F / 2.0F, 6.0F, i3, flag ? 1.0F : 0.0F);
        if (flag2) {
            MatrixUtil.pop(drawContext);
        }

        String string = this.name.toUpperCase(Locale.ROOT);
        float f8 = 8.0F + 6.0F + 5.0F;
        new RenderUtil(drawContext, f, f2, f3, f4).textMiddleLeft(Fonts.INTER_MEDIUM, string, f8, 0.0F, i3, 6.0F);
    }

    private static String truncate(String string, float f) {
        if (f <= 0.0F) {
            return "";
        }

        if (Fonts.INTER_MEDIUM.getWidth(string, 7.0F) <= f) {
            return string;
        }

        String string2 = "…";
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < string.length() && !(Fonts.INTER_MEDIUM.getWidth(builder.toString() + string.charAt(i) + string2, 7.0F) > f); i++) {
            builder.append(string.charAt(i));
        }

        return builder + string2;
    }

    public boolean clickFavorite(double d, double d2) {
        if (!this.header && this.showFavorite && this.isHovered(d, d2, this.starX, this.starY, this.starWidth, this.starHeight)) {
            FavoritesPrefs.toggle(this.name);
            return true;
        } else {
            return false;
        }
    }

    public boolean clickReset(double d, double d2) {
        if (!this.header && this.widget.isFocused() && this.isHovered(d, d2, this.resetX, this.resetY, this.resetWidth, this.resetHeight)) {
            this.widget.reset();
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseClicked(double d, double d2) {
        if (this.header) {
            if (this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
                this.toggleCollapsed();
                return true;
            } else {
                return false;
            }
        } else {
            return this.widget.mouseClicked(d, d2);
        }
    }

    public boolean keyPressed(int i) {
        if (this.header) {
            if (i == 0) {
                this.toggleCollapsed();
                return true;
            } else {
                return false;
            }
        } else {
            return this.widget.keyPressed(i);
        }
    }

    public boolean mouseReleased(double d, double d2) {
        return !this.header && this.widget.mouseReleased(d, d2);
    }

    public void update() {
        if (!this.header) {
            this.widget.update();
        }
    }

    public void reset() {
        if (!this.header) {
            this.widget.reset();
        }
    }

    public String getValue() {
        return this.header ? null : this.widget.getText();
    }

    public void setValue(String string) {
        if (!this.header) {
            this.widget.setText(string);
        }
    }
}
