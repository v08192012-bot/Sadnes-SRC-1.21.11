package tech.sadnes.client.ui.menu;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.ShimmerText;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class MenuButton implements IRenderable {
    private static final float TEXT_SIZE = 6.0F;
    private static final float ICON_SIZE = 7.0F;
    private final MenuButton.Style style;
    private final Icon icon;
    private final Runnable action;
    private final Animation hoverAnimation = new Animation();
    private final ShimmerText gradientText = new ShimmerText();
    private final IconHoverAnimator iconRenderer = new IconHoverAnimator();
    private final IconHoverAnimator chevronRenderer = new IconHoverAnimator();
    private String label;
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean enabled = true;
    private Integer accentColor;
    private boolean showChevron = true;
    private float slimeIntensity;
    private float hoverPosition = 0.5F;
    private float hoverVelocity;
    private long lastUpdateNanos;
    private final Animation clickAnimation = new Animation();
    private long clickTime = -100000L;
    private float clickPosition = 0.5F;

    public MenuButton(MenuButton.Style style2, Icon icon2, String string, Runnable runnable) {
        this.style = style2;
        this.icon = icon2;
        this.label = string;
        this.action = runnable;
    }

    public MenuButton withAccentColor(int i) {
        this.accentColor = i;
        return this;
    }

    public MenuButton withSlime(float f) {
        this.slimeIntensity = f;
        return this;
    }

    public MenuButton withChevron(boolean flag) {
        this.showChevron = flag;
        return this;
    }

    public MenuButton setBounds(float f, float f2, float f3, float f4) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        return this;
    }

    public void setLabel(String string) {
        this.label = string;
    }

    public void setEnabled(boolean flag) {
        this.enabled = flag;
    }

    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.hoverAnimation.update();
        boolean flag;
        if (this.enabled) {
            flag = this.isHovered(i, i2, this.x, this.y, this.width, this.height);
        } else {
            flag = false;
        }

        this.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = this.hoverAnimation.get();
        float f3 = (this.enabled ? 1.0F : 0.4F) * f;
        this.clickAnimation.update();
        this.clickAnimation.animate(0.0, 0.22, Easings.BACK_OUT);
        float f4 = this.clickAnimation.get();
        float f5 = 1.0F + 0.03F * f2 - 0.045F * f4;
        boolean flag2 = f2 > 0.001F || f4 > 0.001F;
        if (flag2) {
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.x + this.width / 2.0F, this.y + this.height / 2.0F, f5);
        }

        GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, this.x, this.y, this.width, this.height, f3 * 0.4F, 8.0F);
        new RenderUtil(drawContext, this.x, this.y, this.width, this.height).rect(Theme.withAlpha(ColorUtil.BLACK, (0.4F + 0.1F * f2) * f3), Radius.all(8.0F));
        boolean flag3 = this.slimeIntensity > 0.0F;
        int i3 = this.accentColor != null ? this.accentColor : Theme.getPrimary();
        float f6 = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F + (this.x + this.y) * 0.37F;
        float f7 = this.updateHoverPosition(i, flag);
        float f8 = (float)(System.currentTimeMillis() - this.clickTime) / 550.0F;
        if (flag3) {
            new RenderUtil(drawContext, this.x, this.y, this.width, this.height)
                .slime(i3, f3, f2, this.slimeIntensity, f7, f8, this.clickPosition, this.hoverVelocity, false, f6, Radius.all(8.0F));
        }

        int i4 = Theme.withAlpha(Theme.getTextColor(), f3);
        int i5 = Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), ColorUtil.WHITE, f2), f3);
        RenderUtil renderUtil = new RenderUtil(drawContext, this.x, this.y, this.width, this.height);
        float f9 = 9.0F;
        if (this.icon != null) {
            this.iconRenderer
                .render(drawContext, this.icon, this.x + 9.0F, this.y + (this.height - 7.0F) / 2.0F - this.getIconLift(9.0F / this.width, f2), 7.0F, i4, f2);
            f9 = 9.0F + 7.0F + 5.0F;
        }

        String string = this.label;
        if (this.style == MenuButton.Style.GHOST && f2 > 0.001F) {
            int i6 = this.accentColor != null ? this.accentColor : Theme.getPrimary();
            string = this.gradientText.applyThemed(Fonts.INTER_MEDIUM, this.label, 6.0F, i6, i4, f2);
        }

        renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, string, f9, 0.0F, i4, 6.0F);
        if (this.showChevron) {
            float f10 = f2 > 0.001F ? 1.2F * f2 * (float)Math.sin((float)(System.currentTimeMillis() % 1800L) / 1800.0F * Math.PI * 2.0) : 0.0F;
            this.chevronRenderer
                .render(
                    drawContext,
                    Icon.CHEVRON_RIGHT,
                    this.x + this.width - 9.0F - 5.5F + f10,
                    this.y + (this.height - 5.5F) / 2.0F - this.getIconLift((this.width - 9.0F) / this.width, f2),
                    5.5F,
                    i5,
                    f2
                );
        }

        if (flag3) {
            new RenderUtil(drawContext, this.x, this.y, this.width, this.height)
                .slime(i3, f3, f2, this.slimeIntensity, f7, f8, this.clickPosition, this.hoverVelocity, true, f6, Radius.all(8.0F));
        }

        if (flag2) {
            MatrixUtil.pop(drawContext);
        }
    }

    private float getIconLift(float f, float f2) {
        float f3 = (f - this.hoverPosition) / 0.17F;
        float f4 = 0.36F + 0.38F * f2 + 0.26F * f2 * (float)Math.exp(-f3 * f3);
        return this.height * f4 * 0.06F;
    }

    private float updateHoverPosition(int i, boolean flag) {
        long l = System.nanoTime();
        float f = this.lastUpdateNanos == 0L ? 0.016F : Math.min((float)(l - this.lastUpdateNanos) / 1.0E9F, 0.1F);
        this.lastUpdateNanos = l;
        if (!flag) {
            return this.hoverPosition;
        }

        float f2 = Math.max(0.0F, Math.min(1.0F, (i - this.x) / Math.max(this.width, 1.0F)));
        float f3 = f2 - this.hoverPosition;
        this.hoverPosition = this.hoverPosition + f3 * Math.min(1.0F, f * 9.0F);
        this.hoverVelocity = this.hoverVelocity + (Math.max(-1.0F, Math.min(1.0F, f3 * 6.0F)) - this.hoverVelocity) * Math.min(1.0F, f * 7.0F);
        return this.hoverPosition;
    }

    public boolean mouseClicked(double d, double d2) {
        if (this.enabled && this.action != null && this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            this.clickAnimation.forceValue(1.0);
            this.clickTime = System.currentTimeMillis();
            this.clickPosition = (float)Math.max(0.0, Math.min(1.0, (d - this.x) / Math.max(this.width, 1.0F)));
            this.action.run();
            return true;
        } else {
            return false;
        }
    }

    private static int contrastTextColor(int i) {
        double d = (0.299 * ColorUtil.red(i) + 0.587 * ColorUtil.green(i) + 0.114 * ColorUtil.blue(i)) / 255.0;
        return d > 0.6 ? -15724010 : -1;
    }

    public enum Style {
        PRIMARY,
        GHOST;
    }
}
