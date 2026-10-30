package tech.sadnes.client.ui.screen.settings;

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
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.screen.options.OptionsCategory;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class OptionsCategoryTab implements IRenderable {
    private static final float ICON_SIZE = 6.0F;
    private static final float FONT_SIZE = 6.0F;
    private static final float PADDING = 8.0F;
    private static final float ICON_GAP = 4.0F;
    private final OptionsCategory category;
    private final Runnable onClick;
    private final Animation hoverAnimation = new Animation();
    private final Animation selectAnimation = new Animation();
    private final Animation clickAnimation = new Animation();
    private final ShimmerText shimmer = new ShimmerText();
    private final IconHoverAnimator iconAnimator = new IconHoverAnimator();
    private float x;
    private float y;
    private float width;
    private float height;
    private boolean selected;

    public OptionsCategoryTab(OptionsCategory optionsCategory, Runnable runnable) {
        this.category = optionsCategory;
        this.onClick = runnable;
    }

    public OptionsCategory getCategory() {
        return this.category;
    }

    public void setSelected(boolean flag) {
        this.selected = flag;
    }

    public float getPreferredWidth() {
        return 18.0F + Fonts.INTER_MEDIUM.getWidth(this.category.title, 6.0F) + 8.0F;
    }

    public OptionsCategoryTab setBounds(float f, float f2, float f3, float f4) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        return this;
    }

    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.hoverAnimation.update();
        boolean flag = this.isHovered(i, i2, this.x, this.y, this.width, this.height);
        this.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.selectAnimation.update();
        this.selectAnimation.animate(this.selected ? 1.0 : 0.0, 0.3, Easings.QUAD_OUT);
        this.clickAnimation.update();
        float f2 = this.hoverAnimation.get();
        float f3 = this.selectAnimation.get();
        float f4 = Math.max(f2, f3);
        float f5 = this.clickAnimation.get();
        boolean flag2 = f5 > 0.001F;
        if (flag2) {
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.x + this.width / 2.0F, this.y + this.height / 2.0F, 1.0F + 0.09F * f5);
        }

        Radius radius = Radius.all(7.0F);
        if (f3 > 0.01F) {
            new RenderUtil(drawContext, this.x, this.y, this.width, this.height).rect(Theme.withAlpha(ColorUtil.BLACK, 0.72F * f3 * f), radius);
        }

        GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, this.x, this.y, this.width, this.height, f * 0.55F, 7.0F);
        if (f3 <= 0.01F && f2 > 0.001F) {
            new RenderUtil(drawContext, this.x, this.y, this.width, this.height).rect(Theme.withAlpha(Theme.getPrimary(), 0.12F * f2 * f), radius);
        }

        int i3 = Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), ColorUtil.WHITE, f4), f);
        RenderUtil renderUtil = new RenderUtil(drawContext, this.x, this.y, this.width, this.height);
        this.iconAnimator.render(drawContext, this.category.icon, this.x + 8.0F, this.y + (this.height - 6.0F) / 2.0F, 6.0F, i3, f2);
        String string = this.category.title;
        if (f3 > 0.01F) {
            string = this.shimmer.applyThemed(Fonts.INTER_MEDIUM, string, 6.0F, Theme.getPrimary(), i3, f3);
        }

        renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, string, 18.0F, 0.0F, i3, 6.0F);
        if (flag2) {
            MatrixUtil.pop(drawContext);
        }
    }

    public boolean mouseClicked(double d, double d2) {
        if (this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            if (this.selected) {
                return true;
            }

            this.clickAnimation.forceValue(1.0);
            this.clickAnimation.animate(0.0, 0.32, Easings.EXPO_OUT);
            if (this.onClick != null) {
                this.onClick.run();
            }

            return true;
        } else {
            return false;
        }
    }
}
